const TOKEN_KEYS = {
  accessToken: 'accessToken',
  refreshToken: 'refreshToken',
  userInfo: 'userInfo'
}

function getSession() {
  return {
    accessToken: wx.getStorageSync(TOKEN_KEYS.accessToken) || '',
    refreshToken: wx.getStorageSync(TOKEN_KEYS.refreshToken) || '',
    userInfo: wx.getStorageSync(TOKEN_KEYS.userInfo) || null
  }
}

function setSession(session) {
  if (session.accessToken) {
    wx.setStorageSync(TOKEN_KEYS.accessToken, session.accessToken)
  }
  if (session.refreshToken) {
    wx.setStorageSync(TOKEN_KEYS.refreshToken, session.refreshToken)
  }
  if (session.userInfo) {
    wx.setStorageSync(TOKEN_KEYS.userInfo, session.userInfo)
  }

  const app = getApp({ allowDefault: true })
  if (app && app.globalData) {
    app.globalData.accessToken = session.accessToken || app.globalData.accessToken
    app.globalData.refreshToken = session.refreshToken || app.globalData.refreshToken
    app.globalData.userInfo = session.userInfo || app.globalData.userInfo
  }
}

function clearSession() {
  wx.removeStorageSync(TOKEN_KEYS.accessToken)
  wx.removeStorageSync(TOKEN_KEYS.refreshToken)
  wx.removeStorageSync(TOKEN_KEYS.userInfo)

  const app = getApp({ allowDefault: true })
  if (app && app.globalData) {
    app.globalData.accessToken = ''
    app.globalData.refreshToken = ''
    app.globalData.userInfo = null
  }
}

function isLoggedIn() {
  return Boolean(wx.getStorageSync(TOKEN_KEYS.accessToken))
}

module.exports = {
  getSession,
  setSession,
  clearSession,
  isLoggedIn
}
