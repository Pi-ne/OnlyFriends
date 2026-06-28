const socialService = require('../../../services/social')

Page({
  data: {
    tab: 'friends',
    list: []
  },
  onShow() { this.load() },
  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    this.load()
  },
  async load() {
    const tab = this.data.tab
    let list = []
    if (tab === 'friends') list = await socialService.getFriends()
    if (tab === 'following') list = await socialService.getFollowing()
    if (tab === 'followers') list = await socialService.getFollowers()
    if (tab === 'applies') list = await socialService.getFriendApplies('received')
    this.setData({ list })
  },
  async review(e) {
    await socialService.reviewFriendApply(e.currentTarget.dataset.id, {
      action: Number(e.currentTarget.dataset.action),
      reason: ''
    })
    this.load()
  }
})
