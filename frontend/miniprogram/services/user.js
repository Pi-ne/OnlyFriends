const request = require('../utils/request')

function getMyProfile() {
  return request({ url: '/users/me/profile' })
}

function updateMyProfile(data) {
  return request({
    url: '/users/me/profile',
    method: 'PUT',
    data
  })
}

function getUserProfile(id) {
  return request({ url: `/users/${id}` })
}

function uploadAvatar(filePath) {
  return request.upload({
    url: '/users/me/avatar',
    filePath
  })
}

module.exports = {
  getMyProfile,
  updateMyProfile,
  getUserProfile,
  uploadAvatar
}
