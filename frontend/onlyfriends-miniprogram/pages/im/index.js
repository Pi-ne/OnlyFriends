const imApi = require("../../api/im");
const imRealtime = require("../../utils/im-realtime");

Page({
  data: {
    loading: false,
    error: "",
    loggedIn: false,
    conversations: []
  },

  onShow() {
    imRealtime.ensureConnected();
    this.loadConversations();
    if (!this._offRealtime) {
      this._offRealtime = imRealtime.onEvent(() => this.scheduleRefresh());
    }
  },

  onHide() {
    if (this._refreshTimer) {
      clearTimeout(this._refreshTimer);
      this._refreshTimer = null;
    }
    if (this._offRealtime) {
      this._offRealtime();
      this._offRealtime = null;
    }
  },

  scheduleRefresh() {
    if (this._refreshTimer) {
      return;
    }
    this._refreshTimer = setTimeout(() => {
      this._refreshTimer = null;
      this.loadConversations(true);
    }, 400);
  },

  loadConversations(silent) {
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

    if (!silent) {
      this.setData({ loading: true, error: "", loggedIn: true });
    } else {
      this.setData({ loggedIn: true });
    }

    imApi.listConversations().then((list) => {
      this.setData({ conversations: (list || []).map((item) => this.normalizeConversation(item)) });
    }).catch((err) => {
      if (!silent) {
        this.setData({
          conversations: [],
          error: err.message || "会话加载失败"
        });
      }
    }).finally(() => {
      if (!silent) {
        this.setData({ loading: false });
      }
    });
  },

  normalizeConversation(item) {
    const title = item.title || item.peerNickname || "聊天";
    const isGroup = item.convType === 2;
    return {
      id: item.convId,
      convType: item.convType,
      peerUserId: item.peerUserId,
      teamId: item.teamId,
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
      wx.navigateTo({
        url: `/pages/im/chat/index?type=group&convId=${conversation.id}&teamId=${conversation.teamId || ""}&title=${encodeURIComponent(conversation.title)}`
      });
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
