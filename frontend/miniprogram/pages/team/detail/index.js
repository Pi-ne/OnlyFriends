const teamService = require('../../../services/team')

Page({
  data: {
    id: '',
    team: {},
    tabs: [
      { key: 'feed', name: '动态' },
      { key: 'members', name: '成员' },
      { key: 'album', name: '相册' },
      { key: 'files', name: '文件' },
      { key: 'votes', name: '投票' },
      { key: 'scores', name: '积分' }
    ],
    activeTab: 'feed',
    announcements: [],
    members: [],
    album: [],
    files: [],
    votes: [],
    scores: [],
    submitting: false
  },

  onLoad(options) {
    this.setData({ id: options.id })
    this.load()
  },

  async load() {
    const team = await teamService.getTeamDetail(this.data.id)
    this.setData({ team })
    this.loadTab()
  },

  switchTab(event) {
    this.setData({ activeTab: event.currentTarget.dataset.key })
    this.loadTab()
  },

  async loadTab() {
    const id = this.data.id
    const key = this.data.activeTab
    if (key === 'feed') this.setData({ announcements: await teamService.getAnnouncements(id).catch(() => []) })
    if (key === 'members') this.setData({ members: await teamService.getMembers(id).catch(() => []) })
    if (key === 'album') this.setData({ album: await teamService.getAlbum(id).catch(() => []) })
    if (key === 'files') this.setData({ files: await teamService.getFiles(id).catch(() => []) })
    if (key === 'votes') this.setData({ votes: await teamService.getVotes(id).catch(() => []) })
    if (key === 'scores') this.setData({ scores: await teamService.getScores(id).catch(() => []) })
  },

  async toggleJoin() {
    this.setData({ submitting: true })
    try {
      if (this.data.team.joined) {
        await teamService.leaveTeam(this.data.id)
        wx.showToast({ title: '已退出', icon: 'success' })
      } else {
        await teamService.joinTeam(this.data.id, { message: '希望加入小队' })
        wx.showToast({ title: '已提交', icon: 'success' })
      }
      await this.load()
    } finally {
      this.setData({ submitting: false })
    }
  },

  goChat() {
    wx.navigateTo({ url: `/pages/im/chat/index?type=group&teamId=${this.data.id}&title=${encodeURIComponent(this.data.team.name || '小队群聊')}` })
  }
})
