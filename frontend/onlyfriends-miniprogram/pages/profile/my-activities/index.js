const activityApi = require("../../../api/activity");

const TABS = [
  { key: "pending", label: "待审核" },
  { key: "approved", label: "已审核" },
  { key: "expired", label: "已失效" },
  { key: "all", label: "全部活动" }
];

const STATUS_GROUPS = {
  pending: [1],
  approved: [2, 3, 4, 5],
  expired: [6, 8]
};

Page({
  data: {
    tabs: TABS,
    activeTab: "pending",
    loading: false,
    error: "",
    activities: [],
    currentList: []
  },

  onLoad(options) {
    this.setData({ activeTab: options.tab || "pending" });
  },

  onShow() {
    this.loadActivities();
  },

  loadActivities() {
    const token = wx.getStorageSync("accessToken");
    const user = wx.getStorageSync("userInfo") || {};
    const creatorId = user.id || user.userId;
    if (!token) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    if (!creatorId) {
      this.setData({ error: "请先回到我的页面刷新用户资料", activities: [], currentList: [] });
      return;
    }

    this.setData({ loading: true, error: "" });
    activityApi.listActivities({
      creatorId,
      page: 1,
      size: 50
    }).then((page) => {
      const activities = (page.list || []).map((item) => this.normalizeActivity(item));
      this.setData({ activities });
      this.refreshList(this.data.activeTab, activities);
    }).catch((err) => {
      this.setData({
        activities: [],
        currentList: [],
        error: err.message || "活动加载失败"
      });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  switchTab(event) {
    const key = event.currentTarget.dataset.key;
    this.refreshList(key, this.data.activities);
  },

  refreshList(key, activities) {
    const source = activities || [];
    const statuses = STATUS_GROUPS[key];
    const currentList = statuses ? source.filter((item) => statuses.includes(item.status)) : source;
    this.setData({ activeTab: key, currentList });
  },

  normalizeActivity(item) {
    const maxParticipants = item.maxParticipants || 0;
    const fee = Number(item.fee || 0);
    return {
      id: item.activityId,
      title: item.title || "未命名活动",
      category: (item.tags && item.tags[0]) || "活动",
      status: item.status,
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
