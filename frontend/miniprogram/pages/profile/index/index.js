const auth = require('../../../utils/auth')
const authService = require('../../../services/auth')
const userService = require('../../../services/user')
const activityService = require('../../../services/activity')
const teamService = require('../../../services/team')
const format = require('../../../utils/format')

Page({
  data: {
    loggedIn: false,
    profile: {},
    registeredCount: 0,
    createdCount: 0,
    teamCount: 0
  },

  onShow() {
    this.setData({ loggedIn: auth.isLoggedIn() })
    if (auth.isLoggedIn()) {
      this.loadProfile()
    }
  },

  async loadProfile() {
    const profile = await userService.getMyProfile().catch(() => auth.getSession().userInfo || {})
    const registered = await activityService.getRegisteredActivities({ page: 1, size: 1 }).catch(() => ({ total: 0 }))
    const created = profile.userId
      ? await activityService.getActivities({ creatorId: profile.userId, page: 1, size: 1 }).catch(() => ({ total: 0 }))
      : { total: 0 }
    const teams = await teamService.getTeams({ joined: true }).catch(() => [])
    this.setData({
      profile,
      registeredCount: format.normalizePageResult(registered).total,
      createdCount: format.normalizePageResult(created).total,
      teamCount: Array.isArray(teams) ? teams.length : 0
    })
  },

  goLogin() { wx.navigateTo({ url: '/pages/auth/login/index?redirect=/pages/profile/index/index' }) },
  goEdit() { wx.navigateTo({ url: '/pages/profile/edit/index' }) },
  goActivities() { wx.navigateTo({ url: '/pages/profile/activities/index' }) },
  goNotifications() { wx.navigateTo({ url: '/pages/profile/notifications/index' }) },
  goTeams() { wx.switchTab({ url: '/pages/team/list/index' }) },
  goMerchant() { wx.navigateTo({ url: '/pages/profile/merchant-apply/index' }) },
  logout() {
    authService.logout()
    this.setData({ loggedIn: false, profile: {} })
  }
})
