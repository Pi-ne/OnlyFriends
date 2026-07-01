const app = getApp();

const DEFAULT_CENTER = {
  latitude: 39.90469,
  longitude: 116.40717
};

Page({
  data: {
    keyword: "",
    centerLatitude: DEFAULT_CENTER.latitude,
    centerLongitude: DEFAULT_CENTER.longitude,
    scale: 12,
    markers: [],
    suggestions: [],
    selected: null,
    searching: false,
    locating: false,
    error: "",
    hint: "搜索地点并点选结果，系统会保存该地点的真实经纬度。"
  },

  onLoad(options) {
    const initial = this.locationFromOptions(options || {});
    if (initial) {
      this.selectLocation(initial, "initial");
      return;
    }
    this.locateCurrentPosition();
  },

  onUnload() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
  },

  locationFromOptions(options) {
    const latitude = Number(options.latitude);
    const longitude = Number(options.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return null;
    }
    const name = this.safeDecode(options.name) || "已选择地点";
    const detail = this.safeDecode(options.detail) || name;
    return {
      name,
      detail,
      latitude,
      longitude,
      source: "initial"
    };
  },

  safeDecode(value) {
    if (!value) {
      return "";
    }
    try {
      return decodeURIComponent(value);
    } catch (err) {
      return String(value);
    }
  },

  locateCurrentPosition() {
    this.setData({ locating: true });
    wx.getLocation({
      type: "gcj02",
      success: (res) => {
        const latitude = Number(res.latitude);
        const longitude = Number(res.longitude);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
          return;
        }
        this.setData({
          centerLatitude: latitude,
          centerLongitude: longitude,
          scale: 13
        });
      },
      complete: () => {
        this.setData({ locating: false });
      }
    });
  },

  updateKeyword(event) {
    const keyword = event.detail.value;
    this.setData({ keyword });
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    const text = this.cleanText(keyword);
    if (text.length < 2) {
      this.setData({
        suggestions: [],
        error: "",
        hint: "输入至少两个字搜索地点，如哈尔滨站、海南岛。"
      });
      return;
    }
    this.searchTimer = setTimeout(() => {
      this.searchPlaces(text);
    }, 350);
  },

  confirmSearch() {
    const text = this.cleanText(this.data.keyword);
    if (text.length < 2) {
      wx.showToast({ title: "请输入更具体的地点", icon: "none" });
      return;
    }
    this.searchPlaces(text);
  },

  clearKeyword() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer);
    }
    this.setData({
      keyword: "",
      suggestions: [],
      error: "",
      hint: "搜索地点并点选结果，系统会保存该地点的真实经纬度。"
    });
  },

  searchPlaces(keyword) {
    const key = app.globalData.tencentMapKey;
    if (!key) {
      this.setData({
        suggestions: [],
        error: "缺少腾讯地图 Key，请先配置后再搜索。"
      });
      return;
    }
    this.setData({ searching: true, error: "", hint: "正在搜索地点..." });
    wx.request({
      url: "https://apis.map.qq.com/ws/place/v1/suggestion",
      data: {
        keyword,
        page_size: 12,
        key
      },
      success: (res) => {
        if (res.data && res.data.status !== 0) {
          console.warn("腾讯地图地点搜索失败", res.data);
          this.setData({
            suggestions: [],
            error: this.mapApiMessage(res.data),
            hint: ""
          });
          return;
        }
        const suggestions = ((res.data && res.data.data) || [])
          .map((item, index) => this.normalizeSuggestion(item, index))
          .filter(Boolean);
        this.setData({
          suggestions,
          error: suggestions.length ? "" : "没有找到匹配地点，请换一个关键词。",
          hint: suggestions.length ? "点选下面的地点结果后再确认。" : ""
        });
      },
      fail: () => {
        this.setData({
          suggestions: [],
          error: "地点搜索请求失败，请检查网络或合法域名配置。",
          hint: ""
        });
      },
      complete: () => {
        this.setData({ searching: false });
      }
    });
  },

  normalizeSuggestion(item, index) {
    const location = item.location || {};
    const latitude = Number(location.lat || location.latitude);
    const longitude = Number(location.lng || location.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return null;
    }
    const name = this.cleanText(item.title || item.name);
    const region = [item.province, item.city, item.district].map((value) => this.cleanText(value)).filter(Boolean).join("");
    const address = this.cleanText(item.address);
    const detail = address || region || name;
    return {
      id: `${latitude},${longitude},${index}`,
      name: name || detail || "地图地点",
      detail,
      latitude,
      longitude,
      latitudeText: this.formatCoord(latitude),
      longitudeText: this.formatCoord(longitude),
      source: "search"
    };
  },

  selectSuggestion(event) {
    const index = Number(event.currentTarget.dataset.index);
    const item = this.data.suggestions[index];
    if (!item) {
      return;
    }
    this.selectLocation(item, "search");
  },

  tapMap(event) {
    const detail = event.detail || {};
    const latitude = Number(detail.latitude);
    const longitude = Number(detail.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return;
    }
    const point = {
      name: "正在解析地点名称",
      detail: "正在通过腾讯地图解析地址",
      latitude,
      longitude,
      source: "map"
    };
    this.selectLocation(point, "map");
    this.reverseGeocode(latitude, longitude);
  },

  reverseGeocode(latitude, longitude) {
    const key = app.globalData.tencentMapKey;
    if (!key) {
      this.setData({ error: "缺少腾讯地图 Key，无法解析地点名称。" });
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
          this.setData({ error: this.mapApiMessage(res.data) });
          return;
        }
        const result = res.data && res.data.result;
        const poi = result && result.pois && result.pois.length ? result.pois[0] : null;
        const name = this.cleanText(poi && poi.title)
          || this.cleanText(result && result.formatted_addresses && result.formatted_addresses.recommend)
          || this.cleanText(result && result.address);
        const detail = this.cleanText(poi && poi.address)
          || this.cleanText(result && result.address)
          || name;
        if (!name) {
          this.setData({ error: "地图没有返回地点名，请搜索地点并点选结果。" });
          return;
        }
        this.selectLocation({
          name,
          detail,
          latitude,
          longitude,
          source: "geocode"
        }, "geocode");
      },
      fail: () => {
        this.setData({ error: "地点名称解析失败，请搜索地点并点选结果。" });
      }
    });
  },

  selectLocation(location, source) {
    const selected = {
      name: this.cleanText(location.name) || "已选择地点",
      detail: this.cleanText(location.detail) || this.cleanText(location.name) || "地点详情待补充",
      latitude: Number(location.latitude),
      longitude: Number(location.longitude),
      latitudeText: this.formatCoord(location.latitude),
      longitudeText: this.formatCoord(location.longitude),
      source: source || location.source || "manual"
    };
    if (!Number.isFinite(selected.latitude) || !Number.isFinite(selected.longitude)) {
      return;
    }
    this.setData({
      selected,
      keyword: selected.source === "search" ? selected.name : this.data.keyword,
      centerLatitude: selected.latitude,
      centerLongitude: selected.longitude,
      scale: 16,
      markers: [this.createMarker(selected)],
      suggestions: [],
      error: "",
      hint: "已选中地点，确认后会保存名称和经纬度。"
    });
  },

  createMarker(location) {
    return {
      id: 1,
      latitude: location.latitude,
      longitude: location.longitude,
      title: location.name,
      width: 36,
      height: 36,
      callout: {
        content: location.name,
        display: "ALWAYS",
        padding: 8,
        borderRadius: 8,
        bgColor: "#ffffff",
        color: "#19202b",
        fontSize: 13
      }
    };
  },

  useNativeChooser() {
    wx.chooseLocation({
      success: (res) => {
        const chosen = this.normalizeNativeLocation(res);
        if (!Number.isFinite(chosen.latitude) || !Number.isFinite(chosen.longitude)) {
          wx.showToast({ title: "没有拿到有效坐标", icon: "none" });
          return;
        }
        if (this.isDefaultMapCenter(chosen.latitude, chosen.longitude)) {
          if (chosen.name || chosen.detail) {
            this.geocodeByText(chosen);
            return;
          }
          wx.showModal({
            title: "请重新选择地点",
            content: "系统地图只返回了默认中心点。请搜索地点并点选结果，或使用当前页搜索列表。",
            showCancel: false
          });
          return;
        }
        this.selectLocation(chosen, "native");
      },
      fail: (err) => {
        if (err && err.errMsg && err.errMsg.includes("cancel")) {
          return;
        }
        wx.showToast({ title: "系统地图打开失败", icon: "none" });
      }
    });
  },

  normalizeNativeLocation(res) {
    const rawLocation = res.location || {};
    const latitude = Number(res.latitude || rawLocation.latitude || rawLocation.lat);
    const longitude = Number(res.longitude || rawLocation.longitude || rawLocation.lng);
    const name = this.cleanText(res.name || res.poiName || res.title || res.locationName);
    const detail = this.cleanText(res.address || res.formattedAddress || res.addr || name);
    return {
      name,
      detail,
      latitude,
      longitude,
      source: "native"
    };
  },

  geocodeByText(chosen) {
    const key = app.globalData.tencentMapKey;
    const address = this.cleanText(`${chosen.name} ${chosen.detail}`);
    if (!key || !address) {
      wx.showToast({ title: "请使用搜索列表选择地点", icon: "none" });
      return;
    }
    this.setData({ searching: true, error: "", hint: "正在根据地点名获取坐标..." });
    wx.request({
      url: "https://apis.map.qq.com/ws/geocoder/v1/",
      data: {
        address,
        key
      },
      success: (res) => {
        if (res.data && res.data.status !== 0) {
          console.warn("腾讯地图地址解析失败", res.data);
          this.setData({ error: this.mapApiMessage(res.data) });
          return;
        }
        const result = res.data && res.data.result;
        const location = result && result.location;
        const latitude = Number(location && location.lat);
        const longitude = Number(location && location.lng);
        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
          this.setData({ error: "地点名没有解析出有效坐标，请改用搜索列表。" });
          return;
        }
        this.selectLocation({
          name: chosen.name || result.title || address,
          detail: chosen.detail || result.address || address,
          latitude,
          longitude,
          source: "geocode"
        }, "geocode");
      },
      fail: () => {
        this.setData({ error: "地点坐标解析失败，请改用搜索列表。" });
      },
      complete: () => {
        this.setData({ searching: false });
      }
    });
  },

  confirmSelection() {
    const selected = this.data.selected;
    if (!selected) {
      wx.showToast({ title: "请先选择地点", icon: "none" });
      return;
    }
    if (!Number.isFinite(selected.latitude) || !Number.isFinite(selected.longitude)) {
      wx.showToast({ title: "地点坐标无效", icon: "none" });
      return;
    }
    if (this.isDefaultMapCenter(selected.latitude, selected.longitude) && !["search", "geocode"].includes(selected.source)) {
      wx.showModal({
        title: "请重新选择地点",
        content: "当前仍是地图默认中心点。请搜索地点并点选搜索结果后再确认。",
        showCancel: false
      });
      return;
    }
    const payload = {
      name: selected.name,
      detail: selected.detail,
      latitude: selected.latitude,
      longitude: selected.longitude,
      source: selected.source
    };
    wx.setStorageSync("pendingActivityLocation", payload);
    const eventChannel = this.getOpenerEventChannel && this.getOpenerEventChannel();
    if (eventChannel && eventChannel.emit) {
      eventChannel.emit("locationSelected", payload);
    }
    wx.navigateBack();
  },

  mapApiMessage(data) {
    const status = data && data.status;
    const message = data && data.message;
    if (status === 121) {
      return "腾讯地图 Key 今日额度已用完，暂时无法搜索地点。";
    }
    if (status === 110 || status === 311) {
      return "腾讯地图 Key 未授权或配置不完整，请检查 WebServiceAPI。";
    }
    return message || "腾讯地图接口暂时不可用。";
  },

  isDefaultMapCenter(latitude, longitude) {
    return Number.isFinite(latitude)
      && Number.isFinite(longitude)
      && Math.abs(latitude - DEFAULT_CENTER.latitude) < 0.002
      && Math.abs(longitude - DEFAULT_CENTER.longitude) < 0.002;
  },

  formatCoord(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number.toFixed(6) : "";
  },

  cleanText(value) {
    return String(value || "").trim();
  }
});
