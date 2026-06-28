const activityService = require('../../../services/activity')
Page({
  data: { id: '', rating: 5, content: '', submitting: false },
  onLoad(options) { this.setData({ id: options.id }) },
  onRatingInput(e) { this.setData({ rating: Number(e.detail.value) }) },
  onContentInput(e) { this.setData({ content: e.detail.value }) },
  async submit() {
    this.setData({ submitting: true })
    try {
      await activityService.publishComment(this.data.id, { rating: this.data.rating, content: this.data.content })
      wx.navigateBack()
    } finally {
      this.setData({ submitting: false })
    }
  }
})
