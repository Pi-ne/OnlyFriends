const activityApi = require("../../../api/activity");

const app = getApp();
const DEFAULT_WECHAT_MAP_CENTER = {
  latitude: 39.90469,
  longitude: 116.40717
};

const CATEGORIES = [
  "运动健身",
  "户外徒步",
  "桌游聚会",
  "学习交流",
  "公益活动",
  "城市探索",
  "其他"
];

function createEmptyForm() {
  return {
    title: "",
    category: "",
    startDate: "",
    startClock: "",
    endDate: "",
    endClock: "",
    regDate: "",
    regClock: "",
    location: "",
    locationDetail: "",
    locationLat: null,
    locationLng: null,
    capacity: "",
    desc: ""
  };
}

function createUiHints(form) {
  const hasLocation = form.locationLat !== null;
  const categoryIndex = resolveCategoryIndex(form.category);
  return {
    startDateText: form.startDate || "选择日期",
    startClockText: form.startClock || "选择时间",
    endDateText: form.endDate || "选择日期",
    endClockText: form.endClock || "选择时间",
    regDateText: form.regDate || "选择日期",
    regClockText: form.regClock || "选择时间",
    locationName: form.location || (hasLocation ? "已选择地图位置" : "从地图选择活动地点"),
    locationDetail: form.locationDetail || (hasLocation ? "请在下方填写具体地点名称" : "选择后会保存真实地图位置"),
    locationCta: hasLocation ? "重选" : "选择",
    categoryText: form.category || "请选择活动类型",
    startDatePlaceholder: !form.startDate,
    startClockPlaceholder: !form.startClock,
    endDatePlaceholder: !form.endDate,
    endClockPlaceholder: !form.endClock,
    regDatePlaceholder: !form.regDate,
    regClockPlaceholder: !form.regClock,
    categoryPlaceholder: !form.category,
    locationSelected: hasLocation,
    pickerCategoryIndex: categoryIndex < 0 ? 0 : categoryIndex
  };
}

function resolveCategoryIndex(category) {
  const index = CATEGORIES.indexOf(category);
  return index >= 0 ? index : -1;
}

Page({
  data: {
    categories: CATEGORIES,
    saving: false,
    planning: false,
    form: createEmptyForm(),
    ui: createUiHints(createEmptyForm()),
    locationMarkers: []
  },

  onShow() {
    this.consumePendingLocation();
    wx.pageScrollTo({
      scrollTop: 0,
      duration: 0
    });
  },

  syncFormState(formPatch) {
    const form = Object.assign({}, this.data.form, formPatch);
    this.setData({
      form,
      ui: createUiHints(form)
    });
  },

  consumePendingLocation() {
    const location = wx.getStorageSync("pendingActivityLocation");
    if (!location) {
      return;
    }
    wx.removeStorageSync("pendingActivityLocation");
    this.applyChosenLocation(location);
  },

  updateField(event) {
    const key = event.currentTarget.dataset.key;
    this.syncFormState({ [key]: event.detail.value });
  },

  updateCategory(event) {
    const index = Number(event.detail.value);
    const category = this.data.categories[index] || "";
    this.syncFormState({ category });
  },

  updateStartDate(event) {
    this.syncFormState({ startDate: event.detail.value });
    this.applyStartTimeDefaults();
  },

  updateStartClock(event) {
    this.syncFormState({ startClock: event.detail.value });
    this.applyStartTimeDefaults();
  },

  updateEndDate(event) {
    this.syncFormState({ endDate: event.detail.value });
  },

  updateEndClock(event) {
    this.syncFormState({ endClock: event.detail.value });
  },

  updateRegDate(event) {
    this.syncFormState({ regDate: event.detail.value });
  },

  updateRegClock(event) {
    this.syncFormState({ regClock: event.detail.value });
  },

  applyStartTimeDefaults() {
    const startTime = this.parseDateTime(this.data.form.startDate, this.data.form.startClock);
    if (!startTime) {
      return;
    }
    const form = this.data.form;
    const patch = {};
    if (!form.endDate || !form.endClock) {
      const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000);
      patch.endDate = this.formatDate(endTime);
      patch.endClock = this.formatClock(endTime);
    }
    if (!form.regDate || !form.regClock) {
      const regDeadline = new Date(startTime.getTime() - 24 * 60 * 60 * 1000);
      patch.regDate = this.formatDate(regDeadline);
      patch.regClock = this.formatClock(regDeadline);
    }
    if (Object.keys(patch).length) {
      this.syncFormState(patch);
    }
  },

  chooseActivityLocation() {
    const form = this.data.form;
    const params = [];
    if (Number.isFinite(Number(form.locationLat)) && Number.isFinite(Number(form.locationLng))) {
      params.push(`latitude=${encodeURIComponent(form.locationLat)}`);
      params.push(`longitude=${encodeURIComponent(form.locationLng)}`);
    }
    if (form.location) {
      params.push(`name=${encodeURIComponent(form.location)}`);
    }
    if (form.locationDetail) {
      params.push(`detail=${encodeURIComponent(form.locationDetail)}`);
    }
    wx.navigateTo({
      url: `/pages/location/picker/index${params.length ? `?${params.join("&")}` : ""}`,
      events: {
        locationSelected: (location) => {
          wx.removeStorageSync("pendingActivityLocation");
          this.applyChosenLocation(location);
        }
      },
      fail: () => {
        this.openNativeLocationPicker();
      }
    });
  },

  openNativeLocationPicker() {
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
      if (chosen.resolvedByText) {
        this.resolveLocationByText(chosen);
        return;
      }
      this.clearChosenLocation();
      wx.showModal({
        title: "请重新选择地图位置",
        content: "当前只拿到了地图默认中心点。请搜索地点并点选结果后再确认。",
        showCancel: false
      });
      return;
    }
    if (!Number.isFinite(chosen.latitude) || !Number.isFinite(chosen.longitude)) {
      this.clearChosenLocation();
      wx.showModal({
        title: "请重新选择地图位置",
        content: "当前没有拿到有效经纬度。请在地图页搜索地点并点选结果，或拖动红点到真实位置后再确认。",
        showCancel: false
      });
      return;
    }
    const fallbackName = chosen.name || "正在解析地点名称";
    const fallbackDetail = chosen.detail || "正在通过腾讯地图解析地址";
    this.syncFormState({
      location: fallbackName,
      locationDetail: fallbackDetail,
      locationLat: chosen.latitude,
      locationLng: chosen.longitude
    });
    this.setData({
      locationMarkers: this.createLocationMarkers(chosen)
    });
    if (chosen.resolvedByText) {
      return;
    }
    this.reverseGeocodeLocation(chosen.latitude, chosen.longitude, chosen);
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
    const source = this.cleanLocationText(res.source || res.resolveSource);

    return {
      name,
      detail,
      latitude,
      longitude,
      source,
      resolvedByText: Boolean(name || detail)
    };
  },

  cleanLocationText(value) {
    return String(value || "").trim();
  },

  clearChosenLocation() {
    this.syncFormState({
      location: "",
      locationDetail: "",
      locationLat: null,
      locationLng: null
    });
    this.setData({ locationMarkers: [] });
  },

  isSuspiciousDefaultLocation(location) {
    if (["search", "geocode"].includes(location.source)) {
      return false;
    }
    if (location.resolvedByText) {
      return this.isDefaultMapCenter(location.latitude, location.longitude);
    }
    if (!Number.isFinite(location.latitude) || !Number.isFinite(location.longitude)) {
      return true;
    }
    return this.isDefaultMapCenter(location.latitude, location.longitude);
  },

  isDefaultMapCenter(latitude, longitude) {
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return true;
    }
    return Math.abs(latitude - DEFAULT_WECHAT_MAP_CENTER.latitude) < 0.002
      && Math.abs(longitude - DEFAULT_WECHAT_MAP_CENTER.longitude) < 0.002;
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

  reverseGeocodeLocation(latitude, longitude, chosen) {
    const key = app.globalData.tencentMapKey;
    if (!key || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      this.showLocationResolveFallback(chosen);
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
        if (res.data && res.data.status !== 0) {
          console.warn("腾讯地图逆地址解析失败", res.data);
          this.showLocationResolveFallback(chosen);
          return;
        }
        const result = res.data && res.data.result;
        if (!result) {
          this.showLocationResolveFallback(chosen);
          return;
        }
        const poi = result.pois && result.pois.length ? result.pois[0] : null;
        const name = this.cleanLocationText(chosen && chosen.name)
          || this.cleanLocationText(poi && poi.title)
          || this.cleanLocationText(result.formatted_addresses && result.formatted_addresses.recommend)
          || this.cleanLocationText(result.address);
        const detail = this.cleanLocationText(chosen && chosen.detail)
          || this.cleanLocationText(poi && poi.address)
          || this.cleanLocationText(result.address);
        if (!name && !detail) {
          this.showLocationResolveFallback(chosen);
          return;
        }
        this.syncFormState({
          location: name || detail,
          locationDetail: detail || name
        });
      },
      fail: () => {
        this.showLocationResolveFallback(chosen);
      }
    });
  },

  resolveLocationByText(chosen) {
    const key = app.globalData.tencentMapKey;
    const keyword = this.cleanLocationText(`${chosen.name} ${chosen.detail}`);
    if (!key || !keyword) {
      this.clearChosenLocation();
      wx.showModal({
        title: "请重新选择地图位置",
        content: "当前没有拿到真实坐标。请在选址页搜索地点并点选搜索结果。",
        showCancel: false
      });
      return;
    }
    this.setData({
      locationMarkers: []
    });
    this.syncFormState({
      location: chosen.name || chosen.detail || "正在定位地点",
      locationDetail: "正在根据地点名获取真实坐标",
      locationLat: null,
      locationLng: null
    });
    wx.request({
      url: "https://apis.map.qq.com/ws/geocoder/v1/",
      data: {
        address: keyword,
        key
      },
      success: (res) => {
        if (res.data && res.data.status !== 0) {
          console.warn("腾讯地图地址解析失败", res.data);
          this.clearChosenLocation();
          wx.showModal({
            title: "地点坐标获取失败",
            content: "当前地图接口没有返回真实坐标，请在选址页搜索地点并点选搜索结果后再确认。",
            showCancel: false
          });
          return;
        }
        const result = res.data && res.data.result;
        const location = result && result.location;
        const latitude = Number(location && location.lat);
        const longitude = Number(location && location.lng);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
          this.clearChosenLocation();
          wx.showModal({
            title: "地点坐标获取失败",
            content: "当前地图接口没有返回有效经纬度，请重新搜索并点选地点。",
            showCancel: false
          });
          return;
        }
        this.applyChosenLocation({
          name: chosen.name || result.title || keyword,
          detail: chosen.detail || result.address || keyword,
          latitude,
          longitude,
          source: "geocode"
        });
      },
      fail: () => {
        this.clearChosenLocation();
        wx.showModal({
          title: "地点坐标获取失败",
          content: "网络请求失败，请重新搜索并点选地点。",
          showCancel: false
        });
      }
    });
  },

  showLocationResolveFallback(chosen) {
    if (chosen && chosen.resolvedByText) {
      return;
    }
    this.syncFormState({
      location: "已选择地图位置",
      locationDetail: "腾讯地图暂未返回地点名，请在下方补充"
    });
    wx.showToast({ title: "已保存经纬度，请补充地点名称", icon: "none" });
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
    const startTime = this.parseDateTime(this.data.form.startDate, this.data.form.startClock);
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
      this.syncFormState({
        title: plan.title || this.data.form.title,
        category: tags[0] || this.data.form.category,
        location: plan.locationSuggestion || this.data.form.location,
        locationDetail: plan.locationSuggestion ? "" : this.data.form.locationDetail,
        locationLat: plan.locationSuggestion ? null : this.data.form.locationLat,
        locationLng: plan.locationSuggestion ? null : this.data.form.locationLng,
        desc: plan.description || this.data.form.desc,
        capacity: plan.suggestedMaxParticipants || this.data.form.capacity
      });
      if (plan.locationSuggestion) {
        this.setData({ locationMarkers: [] });
      }
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
    const form = createEmptyForm();
    this.setData({
      form,
      ui: createUiHints(form),
      locationMarkers: [],
      planning: false
    });
  },

  createActivity(isDraft) {
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    if (!this.data.form.title || !this.data.form.category
      || !this.data.form.startDate || !this.data.form.startClock
      || !this.data.form.endDate || !this.data.form.endClock
      || !this.data.form.regDate || !this.data.form.regClock
      || !this.data.form.location || !this.data.form.capacity || !this.data.form.desc) {
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
    const timeError = this.validateActivityTimes();
    if (timeError) {
      wx.showToast({ title: timeError, icon: "none" });
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
    const startTime = this.parseDateTime(this.data.form.startDate, this.data.form.startClock);
    const endTime = this.parseDateTime(this.data.form.endDate, this.data.form.endClock);
    const regDeadline = this.parseDateTime(this.data.form.regDate, this.data.form.regClock);
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
    return Boolean(location)
      && !["已选择地图位置", "正在解析地点名称", "正在定位地点"].includes(location);
  },

  parseDateTime(date, clock) {
    if (!date || !clock) {
      return null;
    }
    const dateMatch = String(date).match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
    const clockMatch = String(clock).match(/^(\d{1,2}):(\d{2})$/);
    if (!dateMatch || !clockMatch) {
      return null;
    }
    return new Date(
      Number(dateMatch[1]),
      Number(dateMatch[2]) - 1,
      Number(dateMatch[3]),
      Number(clockMatch[1]),
      Number(clockMatch[2]),
      0
    );
  },

  validateActivityTimes() {
    const startTime = this.parseDateTime(this.data.form.startDate, this.data.form.startClock);
    const endTime = this.parseDateTime(this.data.form.endDate, this.data.form.endClock);
    const regDeadline = this.parseDateTime(this.data.form.regDate, this.data.form.regClock);
    if (!startTime || !endTime || !regDeadline) {
      return "请完整选择开始、结束和报名截止时间";
    }
    if (endTime.getTime() <= startTime.getTime()) {
      return "结束时间必须晚于开始时间";
    }
    if (regDeadline.getTime() >= startTime.getTime()) {
      return "报名截止时间必须早于开始时间";
    }
    return "";
  },

  formatDate(date) {
    const pad = (value) => String(value).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  },

  formatClock(date) {
    const pad = (value) => String(value).padStart(2, "0");
    return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  },

  formatDateTime(date) {
    return `${this.formatDate(date)}T${this.formatClock(date)}:00`;
  }
});
