const activityApi = require("../../../api/activity");

const app = getApp();
const DEFAULT_WECHAT_MAP_CENTER = {
  latitude: 39.90469,
  longitude: 116.40717
};

function createEmptyForm() {
  return {
    title: "",
    category: "",
    date: "",
    clock: "",
    time: "",
    location: "",
    locationDetail: "",
    locationLat: null,
    locationLng: null,
    capacity: "",
    desc: ""
  };
}

Page({
  data: {
    templates: [],
    templateLoading: false,
    templateError: "",
    saving: false,
    planning: false,
    form: createEmptyForm(),
    locationMarkers: []
  },

  onShow() {
    this.loadTemplates();
    wx.pageScrollTo({
      scrollTop: 0,
      duration: 0
    });
  },

  loadTemplates() {
    if (!wx.getStorageSync("accessToken")) {
      this.setData({ templates: [], templateError: "" });
      return;
    }
    this.setData({ templateLoading: true, templateError: "" });
    activityApi.listTemplates().then((list) => {
      const templates = (list || []).map((item) => ({
        id: item.templateId,
        title: item.name || "活动模板",
        type: item.category || "",
        desc: item.description || "",
        tags: item.defaultTags || [],
        capacity: item.defaultMaxParticipants || ""
      }));
      this.setData({ templates });
    }).catch((err) => {
      this.setData({
        templates: [],
        templateError: err.message || "模板加载失败"
      });
    }).finally(() => {
      this.setData({ templateLoading: false });
    });
  },

  updateField(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [`form.${key}`]: event.detail.value });
  },

  updateDate(event) {
    this.setData({ "form.date": event.detail.value });
    this.refreshTime();
  },

  updateClock(event) {
    this.setData({ "form.clock": event.detail.value });
    this.refreshTime();
  },

  refreshTime() {
    const date = this.data.form.date;
    const clock = this.data.form.clock;
    this.setData({ "form.time": date && clock ? `${date} ${clock}` : "" });
  },

  chooseActivityLocation() {
    wx.getLocation({
      type: "gcj02",
      success: (res) => {
        this.chooseMapLocation({
          latitude: res.latitude,
          longitude: res.longitude
        });
      },
      fail: () => {
        this.chooseMapLocation();
      }
    });
  },

  chooseMapLocation(center) {
    wx.chooseLocation({
      latitude: center && center.latitude,
      longitude: center && center.longitude,
      success: (res) => {
        this.applyChosenLocation(res);
      },
      fail: (err) => {
        if (this.isUserCancel(err)) {
          return;
        }
        wx.showToast({ title: "地图选择失败，请检查定位或模拟器位置", icon: "none" });
      }
    });
  },

  applyChosenLocation(res) {
    const chosen = this.normalizeChosenLocation(res);
    if (this.isSuspiciousDefaultLocation(chosen)) {
      this.clearChosenLocation();
      wx.showModal({
        title: "请重新选择地图位置",
        content: "当前只拿到了地图默认中心点。请在地图页搜索地点并点选结果，或拖动红点到真实位置后再确认。",
        showCancel: false
      });
      return;
    }
    this.setData({
      "form.location": chosen.name,
      "form.locationDetail": chosen.detail,
      "form.locationLat": chosen.latitude,
      "form.locationLng": chosen.longitude,
      locationMarkers: this.createLocationMarkers(chosen)
    });
    if (!chosen.resolvedByText) {
      this.reverseGeocodeLocation(chosen.latitude, chosen.longitude);
    }
  },

  normalizeChosenLocation(res) {
    const rawLocation = res.location || {};
    const latitude = Number(res.latitude || rawLocation.latitude || rawLocation.lat);
    const longitude = Number(res.longitude || rawLocation.longitude || rawLocation.lng);
    const name = this.cleanLocationText(
      res.name || res.poiName || res.title || res.locationName || res.address || res.formattedAddress || res.addr
    );
    const detail = this.cleanLocationText(
      res.address || res.formattedAddress || res.addr || res.name || res.poiName || res.title || res.locationName
    );

    return {
      name,
      detail,
      latitude,
      longitude,
      resolvedByText: Boolean(name || detail)
    };
  },

  cleanLocationText(value) {
    return String(value || "").trim();
  },

  clearChosenLocation() {
    this.setData({
      "form.location": "",
      "form.locationDetail": "",
      "form.locationLat": null,
      "form.locationLng": null,
      locationMarkers: []
    });
  },

  isSuspiciousDefaultLocation(location) {
    if (location.resolvedByText) {
      return false;
    }
    if (!Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) {
      return true;
    }
    return Math.abs(location.latitude - DEFAULT_WECHAT_MAP_CENTER.latitude) < 0.002
      && Math.abs(location.longitude - DEFAULT_WECHAT_MAP_CENTER.longitude) < 0.002;
  },

  createLocationMarkers(location) {
    if (!Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) {
      return [];
    }
    return [{
      id: 1,
      latitude: location.latitude,
      longitude: location.longitude,
      title: location.name || "活动地点"
    }];
  },

  isUserCancel(err) {
    return Boolean(err && err.errMsg && err.errMsg.includes("cancel"));
  },

  reverseGeocodeLocation(latitude, longitude) {
    const key = app.globalData.tencentMapKey;
    if (!key || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return;
    }
    wx.request({
      url: "https://apis.map.qq.com/ws/geocoder/v1/",
      data: {
        location: `${latitude},${longitude}`,
        get_poi: 1,
        key
      },
      success: (res) => {
        const result = res.data && res.data.result;
        if (!result) {
          return;
        }
        const poi = result.pois && result.pois.length ? result.pois[0] : null;
        const name = this.cleanLocationText(poi && poi.title) || this.cleanLocationText(result.formatted_addresses && result.formatted_addresses.recommend) || this.cleanLocationText(result.address);
        const detail = this.cleanLocationText(poi && poi.address) || this.cleanLocationText(result.address);
        if (!name && !detail) {
          return;
        }
        this.setData({
          "form.location": name || detail,
          "form.locationDetail": detail || name
        });
      }
    });
  },

  useTemplate(event) {
    const item = this.data.templates[event.currentTarget.dataset.index];
    this.setData({
      "form.category": item.type,
      "form.desc": item.desc,
      "form.capacity": item.capacity || this.data.form.capacity
    });
    wx.showToast({ title: "已套用模板", icon: "success" });
  },

  generateAiPlan() {
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    if (!this.data.form.title || !this.data.form.location) {
      wx.showToast({ title: "请先填写活动名称和地点", icon: "none" });
      return;
    }
    const startTime = this.parseStartTime(this.data.form.time);
    this.setData({ planning: true });
    activityApi.planActivity({
      theme: this.data.form.title,
      locationName: this.data.form.location,
      startTime: startTime ? this.formatDateTime(startTime) : null,
      durationHours: 2,
      maxParticipants: Number(this.data.form.capacity) || null,
      preferences: [this.data.form.category].filter(Boolean)
    }).then((plan) => {
      const tags = plan.tags || [];
      this.setData({
        "form.title": plan.title || this.data.form.title,
        "form.category": tags[0] || this.data.form.category,
        "form.location": plan.locationSuggestion || this.data.form.location,
        "form.locationDetail": plan.locationSuggestion ? "" : this.data.form.locationDetail,
        "form.locationLat": plan.locationSuggestion ? null : this.data.form.locationLat,
        "form.locationLng": plan.locationSuggestion ? null : this.data.form.locationLng,
        "form.desc": plan.description || this.data.form.desc,
        "form.capacity": plan.suggestedMaxParticipants || this.data.form.capacity
      });
      wx.showToast({ title: "已生成草稿", icon: "success" });
    }).catch((err) => {
      wx.showToast({ title: err.message || "AI 策划失败", icon: "none" });
    }).finally(() => {
      this.setData({ planning: false });
    });
  },

  saveDraft() {
    this.createActivity(true);
  },

  submitReview() {
    this.createActivity(false);
  },

  resetForm() {
    this.setData({
      form: createEmptyForm(),
      locationMarkers: [],
      planning: false
    });
  },

  createActivity(isDraft) {
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    if (!this.data.form.title || !this.data.form.category || !this.data.form.date || !this.data.form.clock ||
      !this.data.form.location || !this.data.form.capacity || !this.data.form.desc) {
      wx.showToast({ title: "请补全必填项", icon: "none" });
      return;
    }
    if (!this.hasSelectedLocation()) {
      wx.showToast({ title: "请从地图选择活动地点", icon: "none" });
      return;
    }
    if (!this.hasReadableLocationName()) {
      wx.showToast({ title: "请填写具体地点名称", icon: "none" });
      return;
    }
    if (!this.parseStartTime(this.data.form.time)) {
      wx.showToast({ title: "请选择日期和时间", icon: "none" });
      return;
    }
    this.setData({ saving: true });
    activityApi.createActivity(this.buildPayload(isDraft)).then((res) => {
      this.resetForm();
      wx.showToast({
        title: isDraft ? "草稿已保存" : "已提交审核",
        icon: "success"
      });
      setTimeout(() => {
        wx.navigateTo({ url: "/pages/profile/my-activities/index?tab=all" });
      }, 500);
      return res;
    }).catch((err) => {
      wx.showToast({ title: err.message || "创建失败", icon: "none" });
    }).finally(() => {
      this.setData({ saving: false });
    });
  },

  buildPayload(isDraft) {
    const startTime = this.parseStartTime(this.data.form.time);
    const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000);
    const regDeadline = new Date(startTime.getTime() - 24 * 60 * 60 * 1000);
    return {
      title: this.data.form.title,
      description: this.data.form.desc,
      tags: [this.data.form.category].filter(Boolean),
      startTime: this.formatDateTime(startTime),
      endTime: this.formatDateTime(endTime),
      regDeadline: this.formatDateTime(regDeadline),
      locationName: this.data.form.location,
      locationLat: Number(this.data.form.locationLat),
      locationLng: Number(this.data.form.locationLng),
      locationDetail: this.data.form.locationDetail || this.data.form.location,
      maxParticipants: Number(this.data.form.capacity),
      fee: 0,
      locationVerify: 0,
      locationRadius: 100,
      isDraft
    };
  },

  hasSelectedLocation() {
    const lat = Number(this.data.form.locationLat);
    const lng = Number(this.data.form.locationLng);
    return Number.isFinite(lat) && Number.isFinite(lng) && (lat !== 0 || lng !== 0);
  },

  hasReadableLocationName() {
    const location = this.cleanLocationText(this.data.form.location);
    return Boolean(location);
  },

  parseStartTime(value) {
    const now = new Date();
    const text = value || "";
    const fullMatch = text.match(/^(\d{4})-(\d{1,2})-(\d{1,2})\s+(\d{1,2}):(\d{2})$/);
    if (fullMatch) {
      return new Date(Number(fullMatch[1]), Number(fullMatch[2]) - 1, Number(fullMatch[3]), Number(fullMatch[4]), Number(fullMatch[5]), 0);
    }
    const match = text.match(/(\d{1,2})月(\d{1,2})日\s*(\d{1,2}):(\d{2})/);
    if (match) {
      return new Date(now.getFullYear(), Number(match[1]) - 1, Number(match[2]), Number(match[3]), Number(match[4]), 0);
    }
    return null;
  },

  formatDateTime(date) {
    const pad = (value) => String(value).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
  }
});
