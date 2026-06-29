const activityApi = require("../../api/activity");
const RECOMMEND_LIMIT = 3;
const COVER_CLASSES = ["linear-green", "linear-yellow", "linear-blue"];

Page({
  data: {
    tabs: [
      { key: "recommend", label: "推荐" },
      { key: "latest", label: "最新" },
      { key: "nearby", label: "附近" }
    ],
    activeTab: "recommend",
    recommendLimit: RECOMMEND_LIMIT,
    showMapSuggestion: false,
    loading: false,
    error: "",
    activities: [],
    currentList: []
  },

  onLoad() {
    this.loadActivities("recommend");
  },

  switchTab(event) {
    const key = event.currentTarget.dataset.key;
    this.loadActivities(key);
  },

  loadActivities(key) {
    this.setData({ activeTab: key, loading: true, error: "" });
    activityApi.listActivities({
      tab: key,
      page: 1,
      size: 20
    }).then((page) => {
      const list = (page.list || []).map((item, index) => this.normalizeActivity(item, index));
      this.refreshList(key, list);
    }).catch((err) => {
      this.setData({
        currentList: [],
        activities: [],
        showMapSuggestion: false,
        error: err.message || "活动列表加载失败"
      });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  refreshList(key, source) {
    let list = source || [];
    if (key === "latest") {
      list = [...list].reverse();
    }
    if (key === "nearby") {
      list = [...list].sort((a, b) => a.distanceMeters - b.distanceMeters);
    }
    if (key === "recommend") {
      list = list.slice(0, RECOMMEND_LIMIT);
    }
    this.setData({
      activeTab: key,
      activities: source || [],
      currentList: list,
      showMapSuggestion: key === "recommend" && (source || []).length >= RECOMMEND_LIMIT
    });
  },

  normalizeActivity(item, index) {
    const maxParticipants = item.maxParticipants || 0;
    const fee = Number(item.fee || 0);
    const distanceMeters = item.distanceMeters || 0;
    return {
      id: item.activityId,
      title: item.title,
      category: (item.tags && item.tags[0]) || "活动",
      status: item.statusText || "待开放",
      cover: COVER_CLASSES[index % COVER_CLASSES.length],
      time: this.formatTime(item.startTime),
      location: item.locationName || item.locationDetail || "地点待定",
      distance: distanceMeters ? `${(distanceMeters / 1000).toFixed(1)}km` : "距离待定",
      distanceMeters,
      fee: fee > 0 ? `${fee}元` : "免费",
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

  goSearch() {
    wx.navigateTo({ url: "/pages/search/index" });
  },

  goMap() {
    wx.navigateTo({ url: "/pages/activity/map/index" });
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/activity/detail/index?id=${event.currentTarget.dataset.id}` });
  }
});
