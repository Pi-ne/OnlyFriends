const activityApi = require("../../../api/activity");

const DEFAULT_CENTER = {
  latitude: 39.9042,
  longitude: 116.4074
};

Page({
  data: {
    latitude: DEFAULT_CENTER.latitude,
    longitude: DEFAULT_CENTER.longitude,
    userLatitude: null,
    userLongitude: null,
    scale: 12,
    activities: [],
    markers: [],
    selected: null,
    loading: false,
    error: "",
    locationReady: false
  },

  onLoad() {
    this.initMap();
  },

  initMap() {
    this.setData({ loading: true, error: "" });
    this.loadCurrentLocation()
      .catch(() => null)
      .then(() => this.loadActivities())
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  loadCurrentLocation() {
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: "gcj02",
        success: (res) => {
          this.setData({
            latitude: res.latitude,
            longitude: res.longitude,
            userLatitude: res.latitude,
            userLongitude: res.longitude,
            locationReady: true
          });
          resolve(res);
        },
        fail: reject
      });
    });
  },

  loadActivities() {
    return activityApi.listActivities({
      page: 1,
      size: 50
    }).then((page) => {
      const activities = (page.list || [])
        .map((item, index) => this.normalizeActivity(item, index))
        .filter((item) => item.hasLocation && item.visibleOnMap);
      const markers = activities.map((item, index) => this.toMarker(item, index));
      const first = activities[0] || null;
      this.setData({
        activities,
        markers,
        selected: first,
        latitude: first && !this.data.locationReady ? first.latitude : this.data.latitude,
        longitude: first && !this.data.locationReady ? first.longitude : this.data.longitude,
        scale: first && !this.data.locationReady ? 14 : this.data.scale,
        error: activities.length ? "" : "暂无带地图位置的活动"
      });
    }).catch((err) => {
      this.setData({
        activities: [],
        markers: [],
        selected: null,
        error: err.message || "活动地图加载失败"
      });
    });
  },

  normalizeActivity(item, index) {
    const maxParticipants = item.maxParticipants || 0;
    const fee = Number(item.fee || 0);
    const latitude = Number(item.locationLat);
    const longitude = Number(item.locationLng);
    const hasLocation = Number.isFinite(latitude) && Number.isFinite(longitude) && (latitude !== 0 || longitude !== 0);
    const distanceMeters = this.distanceFromCenter(latitude, longitude);
    return {
      id: item.activityId,
      markerId: Number(item.activityId) || index + 1,
      title: item.title || "未命名活动",
      status: item.statusText || "待开放",
      statusCode: item.status,
      visibleOnMap: !item.status || [2, 3, 4, 5].includes(item.status),
      time: this.formatTime(item.startTime),
      location: item.locationName || item.locationDetail || "地点待定",
      distance: distanceMeters ? `${(distanceMeters / 1000).toFixed(1)}km` : "",
      joined: item.currentCount || 0,
      capacity: maxParticipants > 0 ? maxParticipants : "不限",
      fee: fee > 0 ? `${fee}元` : "免费",
      latitude,
      longitude,
      hasLocation
    };
  },

  toMarker(activity, index) {
    return {
      id: activity.markerId,
      latitude: activity.latitude,
      longitude: activity.longitude,
      title: activity.title,
      width: 34,
      height: 34,
      callout: {
        content: activity.title,
        display: "ALWAYS",
        padding: 8,
        borderRadius: 8,
        bgColor: "#ffffff",
        color: "#19202b",
        fontSize: 13
      },
      label: {
        content: String(index + 1),
        color: "#ffffff",
        fontSize: 13,
        anchorX: -4,
        anchorY: -32
      }
    };
  },

  selectMarker(event) {
    const markerId = Number(event.detail.markerId);
    const selected = this.data.activities.find((item) => item.markerId === markerId);
    if (!selected) {
      return;
    }
    this.setData({
      selected,
      latitude: selected.latitude,
      longitude: selected.longitude,
      scale: 15
    });
  },

  recenter() {
    if (!this.data.locationReady) {
      this.initMap();
      return;
    }
    this.setData({
      latitude: this.data.userLatitude,
      longitude: this.data.userLongitude,
      scale: 13
    });
  },

  goDetail() {
    if (!this.data.selected) {
      return;
    }
    wx.navigateTo({ url: `/pages/activity/detail/index?id=${this.data.selected.id}` });
  },

  openNavigation() {
    const selected = this.data.selected;
    if (!selected) {
      return;
    }
    wx.openLocation({
      latitude: selected.latitude,
      longitude: selected.longitude,
      name: selected.title,
      address: selected.location,
      scale: 17
    });
  },

  distanceFromCenter(latitude, longitude) {
    if (!this.data.locationReady || !Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      return 0;
    }
    const earthRadius = 6371000;
    const toRad = (value) => value * Math.PI / 180;
    const lat1 = toRad(this.data.latitude);
    const lat2 = toRad(latitude);
    const dLat = toRad(latitude - this.data.latitude);
    const dLng = toRad(longitude - this.data.longitude);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
      + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return Math.round(earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
  },

  formatTime(value) {
    if (!value) {
      return "时间待定";
    }
    const normalized = value.replace("T", " ");
    const match = normalized.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/);
    if (!match) {
      return normalized;
    }
    return `${Number(match[1])}月${Number(match[2])}日 ${match[3]}:${match[4]}`;
  }
});
