const format = require('../../utils/format')

Component({
  properties: {
    conversation: { type: Object, value: {} }
  },
  observers: {
    conversation(value) {
      this.setData({
        title: (value && (value.title || value.peerNickname)) || '会话',
        timeText: format.formatDateTime(value && value.lastMsgAt)
      })
    }
  },
  data: {
    title: '会话',
    timeText: ''
  },
  methods: {
    handleTap() {
      this.triggerEvent('tap-chat', { conversation: this.data.conversation })
    }
  }
})
