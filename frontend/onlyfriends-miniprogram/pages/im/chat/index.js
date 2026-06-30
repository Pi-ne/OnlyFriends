const imApi = require("../../../api/im");

Page({
  data: {
    convId: null,
    peerUserId: null,
    title: "聊天",
    loading: false,
    sending: false,
    error: "",
    input: "",
    messages: [],
    scrollIntoView: ""
  },

  onLoad(options) {
    const convId = options.convId ? Number(options.convId) : null;
    const peerUserId = options.peerUserId ? Number(options.peerUserId) : null;
    const title = options.title ? decodeURIComponent(options.title) : "聊天";
    this.setData({ convId, peerUserId, title });
    wx.setNavigationBarTitle({ title });
  },

  onShow() {
    if (this.data.convId) {
      this.loadMessages();
    }
  },

  loadMessages() {
    this.setData({ loading: true, error: "" });
    imApi.listMessages(this.data.convId, {
      page: 1,
      size: 50
    }).then((page) => {
      const messages = (page.list || []).map((item) => this.normalizeMessage(item));
      this.setData({
        messages,
        scrollIntoView: messages.length ? `msg-${messages[messages.length - 1].id}` : ""
      });
      this.markLatestRead(messages);
    }).catch((err) => {
      this.setData({ messages: [], error: err.message || "消息加载失败" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  normalizeMessage(item) {
    return {
      id: item.msgId,
      convId: item.convId,
      senderId: item.senderId,
      content: item.status === 2 ? "消息已撤回" : (item.content || ""),
      mine: Boolean(item.mine),
      recalled: item.status === 2,
      time: item.createdAt ? this.formatTime(item.createdAt) : "刚刚"
    };
  },

  updateInput(event) {
    this.setData({ input: event.detail.value });
  },

  sendMessage() {
    const content = this.data.input.trim();
    if (!content) {
      return;
    }
    if (!this.data.peerUserId) {
      wx.showToast({ title: "接收人信息缺失", icon: "none" });
      return;
    }

    this.setData({ sending: true });
    imApi.sendPrivateMessage({
      receiverId: this.data.peerUserId,
      msgType: 1,
      content
    }).then((message) => {
      const normalized = this.normalizeMessage(message);
      const messages = this.data.messages.concat(normalized);
      const convId = message.convId || this.data.convId;
      this.setData({
        input: "",
        convId,
        messages,
        scrollIntoView: `msg-${normalized.id}`
      }, () => {
        this.markLatestRead(messages);
      });
    }).catch((err) => {
      wx.showToast({ title: err.message || "发送失败", icon: "none" });
    }).finally(() => {
      this.setData({ sending: false });
    });
  },

  markLatestRead(messages) {
    const last = messages[messages.length - 1];
    const convId = this.data.convId;
    if (!convId || !last || !last.id) {
      return;
    }
    imApi.markConversationRead(convId, last.id).catch(() => {});
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
  }
});
