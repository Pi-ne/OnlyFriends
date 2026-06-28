const request = require('../utils/request')

function getFriends() {
  return request({ url: '/friends' })
}

function applyFriend(userId, data) {
  return request({
    url: `/friends/${userId}/applies`,
    method: 'POST',
    data
  })
}

function getFriendApplies(type) {
  return request({
    url: '/friends/applies',
    data: { type: type || 'received' }
  })
}

function reviewFriendApply(id, data) {
  return request({
    url: `/friends/applies/${id}`,
    method: 'PUT',
    data
  })
}

function updateFriendSetting(userId, data) {
  return request({
    url: `/friends/${userId}/setting`,
    method: 'PUT',
    data
  })
}

function deleteFriend(userId) {
  return request({
    url: `/friends/${userId}`,
    method: 'DELETE'
  })
}

function follow(userId) {
  return request({
    url: `/follows/${userId}`,
    method: 'POST'
  })
}

function unfollow(userId) {
  return request({
    url: `/follows/${userId}`,
    method: 'DELETE'
  })
}

function getFollowing() {
  return request({ url: '/follows/following' })
}

function getFollowers() {
  return request({ url: '/follows/followers' })
}

module.exports = {
  getFriends,
  applyFriend,
  getFriendApplies,
  reviewFriendApply,
  updateFriendSetting,
  deleteFriend,
  follow,
  unfollow,
  getFollowing,
  getFollowers
}
