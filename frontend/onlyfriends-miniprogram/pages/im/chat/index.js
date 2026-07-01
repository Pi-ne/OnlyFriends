const imApi = require("../../../api/im");
const imRealtime = require("../../../utils/im-realtime");
const socialApi = require("../../../api/social");

const ROLE_TEXT = {
  2: "队长",
  1: "管理员",
  0: "成员"
};

Page({
  data: {
    convId: null,
    peerUserId: null,
    teamId: null,
    isGroup: false,
    title: "聊天",
    loading: false,
    sending: false,
    error: "",
    input: "",
    messages: [],
    scrollIntoView: "",
    showMembers: false,
    membersLoading: false,
    membersError: "",
    members: [],
    memberCount: 0,
    isCaptain: false,
    dissolving: false
  },

  onLoad(options) {
    const convId = options.convId ? Number(options.convId) : null;
    const peerUserId = options.peerUserId ? Number(options.peerUserId) : null;
    const teamId = options.teamId ? Number(options.teamId) : null;
    const isGroup = options.type === "group" || Boolean(teamId);
    const title = options.title ? decodeURIComponent(options.title) : "聊天";
    this.setData({ convId, peerUserId, teamId, isGroup, title });
    wx.setNavigationBarTitle({ title });

    imRealtime.ensureConnected();
    this._offRealtime = imRealtime.onEvent((event) => this.handleRealtimeEvent(event));
    if (isGroup && teamId) {
      imRealtime.subscribeTeam(teamId);
      this.loadMembers();
    }
  },

  onShow() {
    imRealtime.ensureConnected();
    if (!this.data.convId && !this.data.teamId) {
      return;
    }
    if (!this._initialLoaded || !imRealtime.isConnected()) {
      this.loadMessages();
      this._initialLoaded = true;
    }
  },

  onUnload() {
    if (this._offRealtime) {
      this._offRealtime();
      this._offRealtime = null;
    }
    if (this.data.isGroup && this.data.teamId) {
      imRealtime.unsubscribeTeam(this.data.teamId);
    }
  },

  loadMessages() {
    this.setData({ loading: true, error: "" });
    const { isGroup, convId, teamId } = this.data;
    const requestPromise = isGroup && !convId
      ? imApi.listGroupMessages(teamId, { page: 1, size: 50 })
      : imApi.listMessages(convId, { page: 1, size: 50 });

    requestPromise.then((page) => {
      const messages = (page.list || []).map((item) => this.normalizeMessage(item));
      const resolvedConvId = convId || (messages.length ? messages[0].convId : null);
      this.setData({
        convId: resolvedConvId,
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

  handleRealtimeEvent(event) {
    if (!event || !event.message) {
      return;
    }
    if (!this.matchesConversation(event.message)) {
      return;
    }
    if (event.eventType === "RECALL") {
      this.applyRecall(event.message);
      return;
    }
    this.appendIncomingMessage(event.message);
  },

  matchesConversation(message) {
    const { isGroup, convId, teamId, peerUserId } = this.data;
    if (isGroup) {
      if (teamId && message.teamId === teamId) {
        return true;
      }
      return convId && message.convId === convId;
    }
    if (convId && message.convId === convId) {
      return true;
    }
    if (!peerUserId) {
      return false;
    }
    return message.senderId === peerUserId || message.receiverId === peerUserId;
  },

  normalizeMessage(item) {
    const isGroup = this.data.isGroup;
    const mine = Boolean(item.mine);
    return {
      id: item.msgId,
      convId: item.convId,
      senderId: item.senderId,
      senderNickname: item.senderNickname || "成员",
      content: item.status === 2 ? "消息已撤回" : (item.content || ""),
      mine,
      recalled: item.status === 2,
      showSender: isGroup && !mine,
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

    if (this.data.isGroup) {
      this.sendGroupMessage(content);
      return;
    }
    this.sendPrivateMessage(content);
  },

  sendPrivateMessage(content) {
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
      this.appendSentMessage(message, content);
    }).catch((err) => {
      wx.showToast({ title: err.message || "发送失败", icon: "none" });
    }).finally(() => {
      this.setData({ sending: false });
    });
  },

  sendGroupMessage(content) {
    if (!this.data.teamId) {
      wx.showToast({ title: "小队信息缺失", icon: "none" });
      return;
    }

    this.setData({ sending: true });
    imApi.sendGroupMessage({
      teamId: this.data.teamId,
      msgType: 1,
      content
    }).then((message) => {
      this.appendSentMessage(message, content);
    }).catch((err) => {
      wx.showToast({ title: err.message || "发送失败", icon: "none" });
    }).finally(() => {
      this.setData({ sending: false });
    });
  },

  appendIncomingMessage(message) {
    const normalized = this.normalizeMessage(message);
    if (this.data.messages.some((item) => item.id === normalized.id)) {
      return;
    }
    const messages = this.data.messages.concat(normalized);
    const convId = message.convId || this.data.convId;
    this.setData({
      convId,
      messages,
      scrollIntoView: `msg-${normalized.id}`
    }, () => {
      this.markLatestRead(messages);
    });
  },

  appendSentMessage(message, content) {
    const normalized = this.normalizeMessage(message);
    if (!normalized.content && content) {
      normalized.content = content;
    }
    if (this.data.messages.some((item) => item.id === normalized.id)) {
      this.setData({ input: "" });
      return;
    }
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
  },

  applyRecall(message) {
    const msgId = message.msgId;
    const messages = this.data.messages.map((item) => {
      if (item.id !== msgId) {
        return item;
      }
      return {
        ...item,
        content: "消息已撤回",
        recalled: true
      };
    });
    this.setData({ messages });
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
  },

  openMembers() {
    if (!this.data.isGroup || !this.data.teamId) {
      return;
    }
    this.setData({ showMembers: true });
    if (!this.data.members.length) {
      this.loadMembers();
    }
  },

  closeMembers() {
    this.setData({ showMembers: false });
  },

  stopBubble() {},

  loadMembers() {
    this.setData({ membersLoading: true, membersError: "" });
    socialApi.listTeamMembers(this.data.teamId).then((list) => {
      const members = (list || []).map((item) => this.normalizeMember(item));
      const currentUserId = Number((wx.getStorageSync("userInfo") || {}).userId);
      const self = members.find((item) => item.id === currentUserId);
      this.setData({
        members,
        memberCount: members.length,
        isCaptain: Boolean(self && self.isOwner)
      });
    }).catch((err) => {
      this.setData({
        members: [],
        membersError: err.message || "成员加载失败"
      });
    }).finally(() => {
      this.setData({ membersLoading: false });
    });
  },

  normalizeMember(item) {
    const nickname = item.nickname || "用户";
    const role = item.role == null ? 0 : item.role;
    return {
      id: item.userId,
      nickname,
      avatarText: nickname.slice(0, 1),
      role,
      roleText: ROLE_TEXT[role] || "成员",
      isOwner: role === 2,
      isAdmin: role === 1,
      joinedAt: item.joinedAt ? this.formatTime(item.joinedAt) : ""
    };
  },

  confirmDissolveTeam() {
    if (!this.data.isCaptain || !this.data.teamId) {
      return;
    }
    wx.showModal({
      title: "解散群聊",
      content: "解散后该小队群聊将不可用，所有成员将无法继续在此聊天。此操作不可撤销，确定解散吗？",
      confirmText: "解散",
      confirmColor: "#d94c3d",
      success: (res) => {
        if (res.confirm) {
          this.dissolveTeam();
        }
      }
    });
  },

  dissolveTeam() {
    if (this.data.dissolving || !this.data.teamId) {
      return;
    }
    this.setData({ dissolving: true });
    socialApi.dissolveTeam(this.data.teamId).then(() => {
      imRealtime.unsubscribeTeam(this.data.teamId);
      this.setData({ showMembers: false });
      wx.showToast({ title: "群聊已解散", icon: "success" });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
    }).catch((err) => {
      wx.showToast({ title: err.message || "解散失败", icon: "none" });
    }).finally(() => {
      this.setData({ dissolving: false });
    });
  }
});
