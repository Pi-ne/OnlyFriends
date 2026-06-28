const activityService = require('../../../services/activity')
const auth = require('../../../utils/auth')
const route = require('../../../utils/route')
const format = require('../../../utils/format')

Page({
  data: {
    id: '',
    activity: {},
    comments: [],
    registration: null,
    state: 'loading',
    submitting: false,
    primaryAction: null,
    startText: '',
    endText: '',
    deadlineText: '',
    feeText: ''
  },

  onLoad(options) {
    this.setData({ id: options.id })
    this.loadAll()
  },

  onShow() {
    if (this.data.id && this.data.state === 'success') {
      this.loadRegistration()
    }
  },

  async loadAll() {
    if (!this.data.id) {
      this.setData({ state: 'error' })
      return
    }
    this.setData({ state: 'loading' })
    try {
      const activity = await activityService.getActivityDetail(this.data.id)
      const comments = await activityService.getComments(this.data.id, { page: 1, size: 10 }).catch(() => ({ list: [] }))
      this.setData({
        activity,
        comments: format.normalizePageResult(comments).list,
        startText: format.formatDateTime(activity.startTime),
        endText: format.formatDateTime(activity.endTime),
        deadlineText: format.formatDateTime(activity.regDeadline),
        feeText: format.formatFee(activity.fee),
        state: 'success'
      })
      await this.loadRegistration()
    } catch (error) {
      this.setData({ state: 'error' })
    }
  },

  async loadRegistration() {
    if (!auth.isLoggedIn()) {
      this.updatePrimaryAction(null)
      return
    }
    const registration = await activityService.getActivityRegistration(this.data.id).catch(() => null)
    this.setData({ registration })
    this.updatePrimaryAction(registration)
  },

  updatePrimaryAction(registration) {
    const activity = this.data.activity
    const userInfo = auth.getSession().userInfo || {}
    const isCreator = userInfo.userId && activity.creatorId === userInfo.userId
    const registered = registration && registration.registrationStatus === 1
    const finished = activity.status === 6
    let primaryAction = null

    if (isCreator && finished) {
      primaryAction = { key: 'summary', text: '发布总结', type: 'primary' }
    } else if (registered && finished) {
      primaryAction = { key: 'comment', text: '发布评价', type: 'primary' }
    } else if (registered) {
      primaryAction = { key: 'cancel', text: '取消报名', type: 'danger' }
    } else if ([2, 3].indexOf(activity.status) >= 0 || !activity.status) {
      primaryAction = { key: 'register', text: '立即报名', type: 'primary' }
    }

    this.setData({ primaryAction })
  },

  async handlePrimary() {
    if (!this.data.primaryAction) return
    const target = `/pages/activity/detail/index?id=${this.data.id}`
    if (!route.requireLogin(target)) return

    const key = this.data.primaryAction.key
    if (key === 'comment') {
      wx.navigateTo({ url: `/pages/activity/comment/index?id=${this.data.id}` })
      return
    }
    if (key === 'summary') {
      wx.navigateTo({ url: `/pages/activity/summary/index?id=${this.data.id}` })
      return
    }

    this.setData({ submitting: true })
    try {
      if (key === 'cancel') {
        await activityService.cancelRegistration(this.data.id)
        wx.showToast({ title: '已取消报名', icon: 'success' })
      } else {
        await activityService.registerActivity(this.data.id)
        wx.showToast({ title: '报名成功', icon: 'success' })
      }
      await this.loadAll()
    } finally {
      this.setData({ submitting: false })
    }
  },

  goHome() {
    wx.switchTab({ url: '/pages/home/index/index' })
  }
})
