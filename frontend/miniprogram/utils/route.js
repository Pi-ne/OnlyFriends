const auth = require('./auth')

const tabPages = [
  '/pages/home/index/index',
  '/pages/activity/create/index',
  '/pages/team/list/index',
  '/pages/im/conversations/index',
  '/pages/profile/index/index'
]

function normalizeUrl(url) {
  return url && url.charAt(0) === '/' ? url : `/${url || ''}`
}

function isTabPage(url) {
  const path = normalizeUrl(url).split('?')[0]
  return tabPages.indexOf(path) >= 0
}

function navigate(url) {
  const target = normalizeUrl(url)
  if (isTabPage(target)) {
    wx.switchTab({ url: target.split('?')[0] })
    return
  }
  wx.navigateTo({ url: target })
}

function redirectAfterLogin(redirect) {
  if (!redirect) {
    wx.switchTab({ url: '/pages/home/index/index' })
    return
  }
  const target = decodeURIComponent(redirect)
  if (isTabPage(target)) {
    wx.switchTab({ url: target.split('?')[0] })
    return
  }
  wx.redirectTo({ url: target })
}

function requireLogin(targetUrl) {
  if (auth.isLoggedIn()) {
    return true
  }
  const redirect = encodeURIComponent(targetUrl || currentPageUrl())
  wx.navigateTo({
    url: `/pages/auth/login/index?redirect=${redirect}`
  })
  return false
}

function currentPageUrl() {
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  if (!current) {
    return '/pages/home/index/index'
  }
  const query = Object.keys(current.options || {})
    .map((key) => `${key}=${encodeURIComponent(current.options[key])}`)
    .join('&')
  return `/${current.route}${query ? `?${query}` : ''}`
}

module.exports = {
  navigate,
  requireLogin,
  redirectAfterLogin,
  isTabPage,
  currentPageUrl
}
