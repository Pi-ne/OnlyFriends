const auth = require('./auth')

const DEFAULT_BASE_URL = 'http://localhost:8080/api/v1'

function getBaseUrl() {
  const app = getApp({ allowDefault: true })
  return app && app.globalData && app.globalData.baseUrl ? app.globalData.baseUrl : DEFAULT_BASE_URL
}

function buildUrl(path) {
  if (/^https?:\/\//.test(path)) {
    return path
  }
  return `${getBaseUrl()}${path}`
}

function request(options) {
  const token = wx.getStorageSync('accessToken')

  return new Promise((resolve, reject) => {
    wx.request({
      url: buildUrl(options.url),
      method: options.method || 'GET',
      data: options.data || {},
      header: Object.assign({
        'Content-Type': options.contentType || 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      }, options.header || {}),
      success(res) {
        const body = res.data || {}
        const statusOk = res.statusCode >= 200 && res.statusCode < 300

        if (body.code === 200) {
          resolve(body.data)
          return
        }

        if (statusOk && typeof body.code === 'undefined') {
          resolve(body)
          return
        }

        if (body.code === 401 || res.statusCode === 401) {
          auth.clearSession()
          wx.showToast({ title: '请先登录', icon: 'none' })
          wx.navigateTo({ url: '/pages/auth/login/index' })
          reject(body)
          return
        }

        wx.showToast({
          title: body.message || '请求失败',
          icon: 'none'
        })
        reject(body)
      },
      fail(error) {
        wx.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none'
        })
        reject(error)
      }
    })
  })
}

function upload(options) {
  const token = wx.getStorageSync('accessToken')

  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: buildUrl(options.url),
      filePath: options.filePath,
      name: options.name || 'file',
      formData: options.formData || {},
      header: {
        Authorization: token ? `Bearer ${token}` : ''
      },
      success(res) {
        let body = {}
        try {
          body = JSON.parse(res.data || '{}')
        } catch (error) {
          body = {}
        }

        if (body.code === 200) {
          resolve(body.data)
          return
        }

        wx.showToast({ title: body.message || '上传失败', icon: 'none' })
        reject(body)
      },
      fail(error) {
        wx.showToast({ title: '上传失败', icon: 'none' })
        reject(error)
      }
    })
  })
}

module.exports = request
module.exports.upload = upload
