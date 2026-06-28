const authService = require('../../../services/auth')

Page({
  data: {
    nickname: '',
    email: '',
    password: '',
    redirect: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({ redirect: options.redirect || '' })
  },

  onNicknameInput(event) {
    this.setData({ nickname: event.detail.value })
  },

  onEmailInput(event) {
    this.setData({ email: event.detail.value })
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value })
  },

  async submit() {
    const nickname = this.data.nickname.trim()
    const email = this.data.email.trim()
    const password = this.data.password
    if (!nickname || !email || !password) {
      wx.showToast({ title: '请完整填写注册信息', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      await authService.register({ nickname, email, password })
      wx.showModal({
        title: '注册成功',
        content: '请查看邮箱完成账号激活，然后返回登录。',
        showCancel: false,
        success: () => this.goLogin()
      })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goLogin() {
    const redirect = this.data.redirect ? `?redirect=${encodeURIComponent(this.data.redirect)}` : ''
    wx.redirectTo({ url: `/pages/auth/login/index${redirect}` })
  }
})
