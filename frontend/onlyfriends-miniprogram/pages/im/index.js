const imApi = require("../../api/im");

Page({
  data: {
    loading: false,
    error: "",
    loggedIn: false,
    conversations: []
  },

  onShow() {
    this.loadConversations();
  },

  loadConversations() {
    const token = wx.getStorageSync("accessToken");
    if (!token) {
      this.setData({
        loading: false,
        error: "",
        loggedIn: false,
        conversations: []
      });
      return;
    }

    this.setData({ loading: true, error: "", loggedIn: true });
    imApi.listConversations().then((list) => {
      this.setData({ conversations: (list || []).map((item) => this.normalizeConversation(item)) });
    }).catch((err) => {
      this.setData({
        conversations: [],
        error: err.message || "会话加载失败"
      });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  normalizeConversation(item) {
    const title = item.title || item.peerNickname || "聊天";
    const isGroup = item.convType === 2;
    return {
      id: item.convId,
      convType: item.convType,
      peerUserId: item.peerUserId,
      title,
      avatarText: title.slice(0, 1),
      last: item.lastMsgPreview || "暂无消息",
      time: this.formatTime(item.lastMsgAt),
      unread: item.unreadCount || 0,
      isGroup
    };
  },

  formatTime(value) {
    if (!value) {
      return "";
    }
    const normalized = value.replace("T", " ");
    const match = normalized.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/);
    if (!match) {
      return normalized;
    }
    return `${Number(match[1])}月${Number(match[2])}日 ${match[3]}:${match[4]}`;
  },

  openConversation(event) {
    const id = Number(event.currentTarget.dataset.id);
    const conversation = this.data.conversations.find((item) => item.id === id);
    if (!conversation) {
      return;
    }
    if (conversation.isGroup) {
      wx.showToast({ title: "群聊页面稍后接入", icon: "none" });
      return;
    }
    wx.navigateTo({
      url: `/pages/im/chat/index?convId=${conversation.id}&peerUserId=${conversation.peerUserId}&title=${encodeURIComponent(conversation.title)}`
    });
  },

  goLogin() {
    wx.navigateTo({ url: "/pages/auth/login/index" });
  }
});
