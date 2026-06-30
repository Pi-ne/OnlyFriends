const socialApi = require("../../../api/social");

Page({
  data: {
    tabs: [
      { key: "following", label: "我关注的" },
      { key: "followers", label: "关注我的" }
    ],
    activeTab: "following",
    loading: false,
    error: "",
    users: []
  },

  onShow() {
    this.loadUsers();
  },

  switchTab(event) {
    this.setData({
      activeTab: event.currentTarget.dataset.key,
      users: [],
      error: ""
    }, this.loadUsers);
  },

  loadUsers() {
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }

    const loader = this.data.activeTab === "followers" ? socialApi.listFollowers : socialApi.listFollowing;
    this.setData({ loading: true, error: "" });
    loader().then((list) => {
      this.setData({ users: (list || []).map((item) => this.normalizeUser(item)) });
    }).catch((err) => {
      this.setData({
        users: [],
        error: err.message || "关注列表加载失败"
      });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  normalizeUser(item) {
    const nickname = item.nickname || "用户";
    return {
      id: item.userId,
      nickname,
      avatarText: nickname.slice(0, 1),
      role: item.userType === 1 ? "商家用户" : "个人用户",
      mutualFollow: Boolean(item.mutualFollow),
      friend: Boolean(item.friend)
    };
  },

  unfollow(event) {
    const userId = event.currentTarget.dataset.id;
    wx.showModal({
      title: "取消关注",
      content: "确定不再关注这个用户吗？",
      confirmText: "取消关注",
      success: (res) => {
        if (!res.confirm) return;
        socialApi.unfollowUser(userId).then(() => {
          wx.showToast({ title: "已取消关注", icon: "success" });
          this.loadUsers();
        }).catch((err) => {
          wx.showToast({ title: err.message || "操作失败", icon: "none" });
        });
      }
    });
  },

  followBack(event) {
    const userId = event.currentTarget.dataset.id;
    socialApi.followUser(userId).then(() => {
      wx.showToast({ title: "已关注", icon: "success" });
      this.loadUsers();
    }).catch((err) => {
      wx.showToast({ title: err.message || "操作失败", icon: "none" });
    });
  },

  openChat(event) {
    const userId = event.currentTarget.dataset.id;
    const user = this.data.users.find((item) => Number(item.id) === Number(userId));
    if (!user) {
      return;
    }
    wx.navigateTo({
      url: `/pages/im/chat/index?peerUserId=${user.id}&title=${encodeURIComponent(user.nickname)}`
    });
  }
});
