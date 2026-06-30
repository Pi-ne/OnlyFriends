const activityApi = require("../../api/activity");

Page({
  data: {
    keyword: "",
    filters: ["全部", "运动健身", "桌游聚会", "城市探索", "学习交流"],
    activeFilter: "全部",
    loading: false,
    error: "",
    results: []
  },

  onLoad(options) {
    const keyword = options.keyword || "";
    this.setData({ keyword }, this.loadResults);
  },

  onInput(event) {
    this.setData({ keyword: event.detail.value }, this.loadResults);
  },

  pickFilter(event) {
    this.setData({ activeFilter: event.currentTarget.dataset.filter }, this.loadResults);
  },

  loadResults() {
    const keyword = this.data.keyword.trim();
    const activeFilter = this.data.activeFilter;
    this.setData({ loading: true, error: "" });
    activityApi.listActivities({
      keyword,
      tags: activeFilter === "全部" ? "" : activeFilter,
      page: 1,
      size: 30
    }).then((page) => {
      const results = (page.list || []).map((item) => this.normalizeActivity(item));
      this.setData({ results });
    }).catch((err) => {
      this.setData({ results: [], error: err.message || "搜索失败" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  clearFilters() {
    this.setData({ keyword: "", activeFilter: "全部" }, this.loadResults);
  },

  normalizeActivity(item) {
    const fee = Number(item.fee || 0);
    const distanceMeters = item.distanceMeters || 0;
    const tags = item.tags || [];
    return {
      id: item.activityId,
      title: item.title,
      category: tags[0] || "活动",
      status: item.statusText || "待开放",
      time: this.formatTime(item.startTime),
      location: item.locationName || item.locationDetail || "地点待定",
      distance: distanceMeters ? `${(distanceMeters / 1000).toFixed(1)}km` : "距离待定",
      fee: fee > 0 ? `${fee}元` : "免费"
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
