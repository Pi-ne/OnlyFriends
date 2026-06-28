const userService = require('../../../services/user')
Page({
  data: { form: { nickname: '', bio: '' }, submitting: false },
  async onLoad() {
    const profile = await userService.getMyProfile()
    this.setData({ form: profile })
  },
  onNicknameInput(e) { this.setData({ 'form.nickname': e.detail.value }) },
  onBioInput(e) { this.setData({ 'form.bio': e.detail.value }) },
  async submit() {
    this.setData({ submitting: true })
    try {
      const form = this.data.form
      await userService.updateMyProfile({
        nickname: form.nickname,
        gender: form.gender,
        birthday: form.birthday,
        bio: form.bio,
        interestTags: form.interestTags || []
      })
      wx.navigateBack()
    } finally {
      this.setData({ submitting: false })
    }
  }
})
