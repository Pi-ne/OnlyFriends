const activityService = require('../../../services/activity')
Page({
  data: { id: '', title: '', content: '', submitting: false },
  onLoad(options) { this.setData({ id: options.id }) },
  onTitleInput(e) { this.setData({ title: e.detail.value }) },
  onContentInput(e) { this.setData({ content: e.detail.value }) },
  async submit() {
    this.setData({ submitting: true })
    try {
      await activityService.publishSummary(this.data.id, { title: this.data.title, content: this.data.content, imageUrls: [] })
      wx.navigateBack()
    } finally {
      this.setData({ submitting: false })
    }
  }
})
