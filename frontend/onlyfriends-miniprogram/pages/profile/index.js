const userApi = require("../../api/user");
const activityApi = require("../../api/activity");

const guestUser = {
  nickname: "未登录",
  avatarText: "未",
  role: "游客",
  city: "请先登录",
  tags: []
};

Page({
  data: {
    user: guestUser,
    loggedIn: false,
    loading: false,
    statsLoading: false,
    stats: {
      registeredCount: "--",
      createdCount: "--"
    }
  },

  onShow() {
    this.loadProfile();
  },

  loadProfile() {
    const cached = wx.getStorageSync("userInfo");
    const token = wx.getStorageSync("accessToken");
    if (!token) {
      this.setData({
        user: guestUser,
        loggedIn: false,
        loading: false,
        stats: {
          registeredCount: "--",
          createdCount: "--"
        }
      });
      return;
    }

    this.setData({
      loggedIn: true,
      loading: true,
      user: cached ? this.normalizeUser(cached) : this.data.user
    });

    userApi.getProfile().then((profile) => {
      const user = this.normalizeUser(profile);
      wx.setStorageSync("userInfo", user);
      this.setData({ user, loggedIn: true });
      this.loadStats(user);
    }).catch((err) => {
      wx.showToast({ title: err.message || "资料加载失败", icon: "none" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  loadStats(user) {
    const userId = user && (user.id || user.userId);
    if (!userId) {
      return;
    }
    this.setData({ statsLoading: true });
    Promise.all([
      activityApi.listRegisteredActivities({ page: 1, size: 1 }),
      activityApi.listActivities({ creatorId: userId, page: 1, size: 1 })
    ]).then(([registeredPage, createdPage]) => {
      this.setData({
        stats: {
          registeredCount: this.pageTotal(registeredPage),
          createdCount: this.pageTotal(createdPage)
        }
      });
    }).catch(() => {
      this.setData({
        stats: {
          registeredCount: "--",
          createdCount: "--"
        }
      });
    }).finally(() => {
      this.setData({ statsLoading: false });
    });
  },

  pageTotal(page) {
    if (!page) {
      return 0;
    }
    if (page.total !== undefined && page.total !== null) {
      return page.total;
    }
    return (page.list || []).length;
  },

  normalizeUser(profile) {
    const nickname = profile.nickname || "用户";
    const tags = profile.interestTags || profile.tags || [];
    return {
      id: profile.userId || profile.id,
      nickname,
      avatarText: nickname.slice(0, 1),
      role: profile.userType === 1 ? "商家用户" : "个人用户",
      city: profile.bio || profile.email || "资料待完善",
      tags,
      creditScore: profile.creditScore || 0
    };
  },

  goLogin() {
    wx.navigateTo({ url: "/pages/auth/login/index" });
  },

  ensureLoggedIn() {
    if (this.data.loggedIn) {
      return true;
    }
    wx.navigateTo({ url: "/pages/auth/login/index" });
    return false;
  },

  goRegistrations() {
    if (!this.ensureLoggedIn()) {
      return;
    }
    wx.navigateTo({ url: "/pages/profile/registrations/index" });
  },

  goMyActivities() {
    if (!this.ensureLoggedIn()) {
      return;
    }
    wx.navigateTo({ url: "/pages/profile/my-activities/index" });
  },

  goFriends() {
    if (!this.ensureLoggedIn()) {
      return;
    }
    wx.navigateTo({ url: "/pages/profile/follows/index" });
  },

  logout() {
    wx.removeStorageSync("accessToken");
    wx.removeStorageSync("refreshToken");
    wx.removeStorageSync("userInfo");
    this.setData({ user: guestUser, loggedIn: false });
    wx.showToast({ title: "已退出登录", icon: "success" });
  },

  applyMerchant() {
    wx.showModal({
      title: "商家申请",
      content: "这里将接入商家名称、活动领域和营业凭证上传表单。",
      showCancel: false
    });
  }
});
