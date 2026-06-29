const activityApi = require("../../../api/activity");

Page({
  data: {
    loading: false,
    error: "",
    activities: []
  },

  onShow() {
    this.loadRegistrations();
  },

  loadRegistrations() {
    const token = wx.getStorageSync("accessToken");
    if (!token) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }

    this.setData({ loading: true, error: "" });
    activityApi.listRegisteredActivities({
      page: 1,
      size: 50
    }).then((page) => {
      const activities = (page.list || []).map((item) => this.normalizeActivity(item));
      this.setData({ activities });
    }).catch((err) => {
      this.setData({
        activities: [],
        error: err.message || "报名列表加载失败"
      });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  normalizeActivity(item) {
    const maxParticipants = item.maxParticipants || 0;
    const fee = Number(item.fee || 0);
    return {
      id: item.activityId,
      title: item.title || "未命名活动",
      statusText: item.statusText || "未知",
      time: this.formatTime(item.startTime),
      location: item.locationName || item.locationDetail || "地点待定",
      feeText: fee > 0 ? `${fee}元` : "免费",
      joined: item.currentCount || 0,
      capacity: maxParticipants > 0 ? maxParticipants : "不限",
      tags: item.tags || []
    };
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
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/activity/detail/index?id=${event.currentTarget.dataset.id}` });
  }
});
