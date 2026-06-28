const imService = require('../../../services/im')
const format = require('../../../utils/format')

Page({
  data: {
    type: 'private',
    convId: '',
    receiverId: '',
    teamId: '',
    title: '聊天',
    messages: [],
    content: '',
    sending: false,
    scrollIntoView: ''
  },
  onLoad(options) {
    this.setData({
      type: options.type || 'private',
      convId: options.convId || '',
      receiverId: options.receiverId || '',
      teamId: options.teamId || '',
      title: decodeURIComponent(options.title || '聊天')
    })
    wx.setNavigationBarTitle({ title: this.data.title })
    this.load()
  },
  async load() {
    const result = this.data.type === 'group'
      ? await imService.getGroupMessages(this.data.teamId, { page: 1, size: 30 }).catch(() => ({ list: [] }))
      : this.data.convId
        ? await imService.getMessages(this.data.convId, { page: 1, size: 30 }).catch(() => ({ list: [] }))
        : { list: [] }
    const messages = format.normalizePageResult(result).list
    this.setData({
      messages,
      scrollIntoView: messages.length ? `msg-${messages[messages.length - 1].msgId}` : ''
    })
  },
  onInput(e) { this.setData({ content: e.detail.value }) },
  async send() {
    const content = this.data.content.trim()
    if (!content) return
    this.setData({ sending: true })
    try {
      const msg = this.data.type === 'group'
        ? await imService.sendGroup({ teamId: Number(this.data.teamId), msgType: 1, content, mentionAll: false, mentionUserIds: [] })
        : await imService.sendPrivate({ receiverId: Number(this.data.receiverId), msgType: 1, content })
      const messages = this.data.messages.concat(msg)
      this.setData({ messages, content: '', scrollIntoView: `msg-${msg.msgId}` })
    } finally {
      this.setData({ sending: false })
    }
  }
})
