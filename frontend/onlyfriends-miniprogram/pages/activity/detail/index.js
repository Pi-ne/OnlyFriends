const activityApi = require("../../../api/activity");
const socialApi = require("../../../api/social");

const COVER_CLASSES = ["linear-green", "linear-yellow", "linear-blue"];

Page({
  data: {
    activity: null,
    loading: false,
    error: "",
    registered: false,
    followedOrganizer: false,
    followLoading: false
  },

  onLoad(options) {
    this.loadActivity(Number(options.id));
  },

  loadActivity(id) {
    if (!id) {
      this.setData({ error: "活动不存在" });
      return;
    }
    this.setData({ loading: true, error: "" });
    activityApi.getActivity(id).then((item) => {
      const activity = this.normalizeActivity(item);
      this.setData({ activity, followedOrganizer: false });
      this.loadFollowStatus(activity);
    }).catch((err) => {
      this.setData({ activity: null, error: err.message || "活动加载失败" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  normalizeActivity(item) {
    const maxParticipants = item.maxParticipants || 0;
    const fee = Number(item.fee || 0);
    const tags = item.tags || [];
    const latitude = Number(item.locationLat);
    const longitude = Number(item.locationLng);
    const hasLocation = Number.isFinite(latitude) && Number.isFinite(longitude) && (latitude !== 0 || longitude !== 0);
    const locationName = this.cleanLocationText(item.locationName);
    const locationDetail = this.cleanLocationText(item.locationDetail);
    const location = locationName || locationDetail || "地点待定";
    return {
      id: item.activityId,
      creatorId: item.creatorId,
      title: item.title || "未命名活动",
      category: tags[0] || "活动",
      cover: COVER_CLASSES[(item.activityId || 0) % COVER_CLASSES.length],
      status: item.status,
      statusText: item.statusText || "未知",
      time: this.formatTime(item.startTime),
      deadline: this.formatTime(item.regDeadline),
      location,
      locationDetail,
      latitude,
      longitude,
      hasLocation,
      markers: hasLocation ? [{
        id: 1,
        latitude,
        longitude,
        title: location
      }] : [],
      joined: item.currentCount || 0,
      capacity: maxParticipants > 0 ? maxParticipants : "不限",
      fee: fee > 0 ? `${fee}元` : "免费",
      organizer: item.creatorNickname || "发起人",
      desc: item.description || "暂无活动介绍",
      tags,
      isOwner: this.isOwner(item.creatorId),
      canRegister: [2, 3].includes(item.status) && !this.isOwner(item.creatorId)
    };
  },

  isOwner(creatorId) {
    const user = wx.getStorageSync("userInfo") || {};
    const currentUserId = user.id || user.userId;
    return Boolean(creatorId && currentUserId && Number(creatorId) === Number(currentUserId));
  },

  cleanLocationText(value) {
    const text = String(value || "").trim();
    if (!text || text.includes("未获取到地点名称") || text === "已选择地图位置") {
      return "";
    }
    return text;
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

  registerNow() {
    const activity = this.data.activity;
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    if (activity.isOwner) {
      wx.showToast({ title: "不能报名自己发起的活动", icon: "none" });
      return;
    }
    if (!activity.canRegister) {
      wx.showToast({ title: "当前状态不可报名", icon: "none" });
      return;
    }
    wx.showModal({
      title: "确认报名",
      content: `${activity.title}\n${activity.desc}`,
      confirmText: "确认报名",
      success: (res) => {
        if (!res.confirm) return;
        activityApi.registerActivity(activity.id).then(() => {
          this.setData({ registered: true });
          wx.showToast({ title: "报名成功", icon: "success" });
        }).catch((err) => {
          wx.showToast({ title: err.message || "报名失败", icon: "none" });
        });
      }
    });
  },

  followOrganizer() {
    const activity = this.data.activity;
    if (!activity || !activity.creatorId) {
      wx.showToast({ title: "发起人信息缺失", icon: "none" });
      return;
    }
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    if (activity.isOwner) {
      wx.showToast({ title: "不能关注自己", icon: "none" });
      return;
    }

    const action = this.data.followedOrganizer ? socialApi.unfollowUser : socialApi.followUser;
    this.setData({ followLoading: true });
    action(activity.creatorId).then(() => {
      const followedOrganizer = !this.data.followedOrganizer;
      this.setData({ followedOrganizer });
      wx.showToast({
        title: followedOrganizer ? "已关注" : "已取消关注",
        icon: "success"
      });
    }).catch((err) => {
      wx.showToast({ title: err.message || "操作失败", icon: "none" });
    }).finally(() => {
      this.setData({ followLoading: false });
    });
  },

  openNavigation() {
    const activity = this.data.activity;
    if (!activity || !activity.hasLocation) {
      wx.showToast({ title: "当前活动没有地图坐标", icon: "none" });
      return;
    }
    wx.openLocation({
      latitude: activity.latitude,
      longitude: activity.longitude,
      name: activity.location,
      address: activity.locationDetail || activity.location,
      scale: 17
    });
  },

  loadFollowStatus(activity) {
    if (!activity || activity.isOwner || !wx.getStorageSync("accessToken")) {
      return;
    }
    socialApi.listFollowing().then((list) => {
      const followedOrganizer = (list || []).some((item) => Number(item.userId) === Number(activity.creatorId));
      this.setData({ followedOrganizer });
    }).catch(() => {
      this.setData({ followedOrganizer: false });
    });
  }
});
