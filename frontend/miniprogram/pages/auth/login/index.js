const authService = require('../../../services/auth')
const route = require('../../../utils/route')

Page({
  data: {
    email: '',
    password: '',
    redirect: '',
    submitting: false
  },

  onLoad(options) {
    this.setData({ redirect: options.redirect || '' })
  },

  onEmailInput(event) {
    this.setData({ email: event.detail.value })
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value })
  },

  async submit() {
    const email = this.data.email.trim()
    const password = this.data.password
    if (!email || !password) {
      wx.showToast({ title: '请填写邮箱和密码', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    try {
      await authService.login({ email, password })
      wx.showToast({ title: '登录成功', icon: 'success' })
      route.redirectAfterLogin(this.data.redirect)
    } finally {
      this.setData({ submitting: false })
    }
  },

  goRegister() {
    const redirect = this.data.redirect ? `?redirect=${encodeURIComponent(this.data.redirect)}` : ''
    wx.navigateTo({ url: `/pages/auth/register/index${redirect}` })
  }
})
