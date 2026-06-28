const auth = require('../../../utils/auth')
const activityService = require('../../../services/activity')
const userService = require('../../../services/user')
const format = require('../../../utils/format')

Page({
  data: {
    mode: 'registered',
    activities: [],
    loading: true
  },

  onShow() {
    this.load()
  },

  switchMode(event) {
    this.setData({ mode: event.currentTarget.dataset.mode })
    this.load()
  },

  async load() {
    if (!auth.isLoggedIn()) {
      wx.navigateTo({ url: '/pages/auth/login/index?redirect=/pages/profile/activities/index' })
      return
    }
    this.setData({ loading: true })
    try {
      let result
      if (this.data.mode === 'registered') {
        result = await activityService.getRegisteredActivities({ page: 1, size: 20 })
      } else {
        const profile = await userService.getMyProfile()
        result = await activityService.getActivities({ creatorId: profile.userId, page: 1, size: 20 })
      }
      this.setData({ activities: format.normalizePageResult(result).list })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/activity/detail/index?id=${event.detail.activity.activityId}` })
  }
})
