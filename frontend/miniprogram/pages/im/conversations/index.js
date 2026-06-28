const route = require('../../../utils/route')
const imService = require('../../../services/im')

Page({
  data: {
    conversations: [],
    loading: false
  },
  onShow() {
    if (route.requireLogin('/pages/im/conversations/index')) this.load()
  },
  async load() {
    this.setData({ loading: true })
    try {
      const conversations = await imService.getConversations()
      this.setData({ conversations })
    } finally {
      this.setData({ loading: false })
    }
  },
  goChat(event) {
    const c = event.detail.conversation
    const params = c.convType === 2
      ? `type=group&teamId=${c.teamId}&title=${encodeURIComponent(c.title || '群聊')}`
      : `type=private&convId=${c.convId}&receiverId=${c.peerUserId}&title=${encodeURIComponent(c.peerNickname || '私聊')}`
    wx.navigateTo({ url: `/pages/im/chat/index?${params}` })
  }
})
