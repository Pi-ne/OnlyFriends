const teamService = require('../../../services/team')
const format = require('../../../utils/format')

Page({
  data: {
    tagText: '',
    form: { name: '', description: '', tags: [], joinType: 0, maxMembers: 30 },
    submitting: false
  },
  onNameInput(e) { this.setData({ 'form.name': e.detail.value }) },
  onDescInput(e) { this.setData({ 'form.description': e.detail.value }) },
  onMaxInput(e) { this.setData({ 'form.maxMembers': Number(e.detail.value) }) },
  onTagsInput(e) {
    this.setData({ tagText: e.detail.value, 'form.tags': format.splitTags(e.detail.value) })
  },
  async submit() {
    if (!this.data.form.name) {
      wx.showToast({ title: '请填写小队名称', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const team = await teamService.createTeam(this.data.form)
      wx.redirectTo({ url: `/pages/team/detail/index?id=${team.teamId}` })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
