const route = require('../../../utils/route')
const teamService = require('../../../services/team')

Page({
  data: {
    keyword: '',
    joinedTeams: [],
    teams: [],
    loading: false
  },

  onShow() {
    if (route.requireLogin('/pages/team/list/index')) {
      this.loadJoined()
      this.loadRecommended()
    }
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value })
  },

  async loadJoined() {
    const joinedTeams = await teamService.getTeams({ joined: true }).catch(() => [])
    this.setData({ joinedTeams })
  },

  async loadRecommended() {
    this.setData({ loading: true })
    try {
      const teams = await teamService.getTeams({ keyword: this.data.keyword.trim() })
      this.setData({ teams })
    } finally {
      this.setData({ loading: false })
    }
  },

  goDetail(event) {
    wx.navigateTo({ url: `/pages/team/detail/index?id=${event.detail.team.teamId}` })
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/team/create/index' })
  }
})
