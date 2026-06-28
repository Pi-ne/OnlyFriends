const auth = require('./utils/auth')

App({
  globalData: {
    baseUrl: 'http://localhost:8080/api/v1',
    wsUrl: 'ws://localhost:8080/ws/im',
    accessToken: '',
    refreshToken: '',
    userInfo: null
  },

  onLaunch() {
    const session = auth.getSession()
    this.globalData.accessToken = session.accessToken
    this.globalData.refreshToken = session.refreshToken
    this.globalData.userInfo = session.userInfo
  }
})
