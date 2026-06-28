const socialService = require('../../../services/social')
Page({
  data: { list: [] },
  onShow() { this.load() },
  async load() { this.setData({ list: await socialService.getFriendApplies('received') }) },
  async review(e) {
    await socialService.reviewFriendApply(e.currentTarget.dataset.id, { action: Number(e.currentTarget.dataset.action), reason: '' })
    this.load()
  }
})
