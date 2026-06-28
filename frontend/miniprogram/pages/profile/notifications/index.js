const activityService = require('../../../services/activity')

Page({
  data: {
    notifications: [],
    loading: false
  },

  onShow() {
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      const result = await activityService.getNotifications({ page: 1, size: 20 })
      this.setData({ notifications: result.list || [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  async read(event) {
    const id = event.currentTarget.dataset.id
    await activityService.markNotificationRead(id)
    this.load()
  }
})
