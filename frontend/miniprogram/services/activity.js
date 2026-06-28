const request = require('../utils/request')

function getActivities(params) {
  return request({
    url: '/activities',
    data: params
  })
}

function getNearbyActivities(params) {
  return request({
    url: '/activities/nearby',
    data: params
  })
}

function getRegisteredActivities(params) {
  return request({
    url: '/activities/registered',
    data: params
  })
}

function getActivityDetail(id) {
  return request({ url: `/activities/${id}` })
}

function getActivityRegistration(id) {
  return request({ url: `/activities/${id}/registration/me` })
}

function getRegistrations(id) {
  return request({ url: `/activities/${id}/registrations` })
}

function createActivity(data) {
  return request({
    url: '/activities',
    method: 'POST',
    data
  })
}

function updateActivity(id, data) {
  return request({
    url: `/activities/${id}`,
    method: 'PUT',
    data
  })
}

function submitActivity(id) {
  return request({
    url: `/activities/${id}/submit`,
    method: 'POST'
  })
}

function registerActivity(id) {
  return request({
    url: `/activities/${id}/register`,
    method: 'POST'
  })
}

function cancelRegistration(id) {
  return request({
    url: `/activities/${id}/register`,
    method: 'DELETE'
  })
}

function getComments(id, params) {
  return request({
    url: `/activities/${id}/comments`,
    data: params
  })
}

function publishComment(id, data) {
  return request({
    url: `/activities/${id}/comments`,
    method: 'POST',
    data
  })
}

function getTemplates() {
  return request({ url: '/activities/templates' })
}

function getTags(params) {
  return request({
    url: '/activities/tags',
    data: params
  })
}

function uploadImage(filePath) {
  return request.upload({
    url: '/activities/images',
    filePath
  })
}

function getCheckinQrcode(id) {
  return request({ url: `/activities/${id}/checkin/qrcode` })
}

function checkin(id, data) {
  return request({
    url: `/activities/${id}/checkin`,
    method: 'POST',
    data
  })
}

function publishSummary(id, data) {
  return request({
    url: `/activities/${id}/summary`,
    method: 'POST',
    data
  })
}

function getSummary(id) {
  return request({ url: `/activities/${id}/summary` })
}

function getNotifications(params) {
  return request({
    url: '/notifications',
    data: params
  })
}

function markNotificationRead(id) {
  return request({
    url: `/notifications/${id}/read`,
    method: 'PUT'
  })
}

module.exports = {
  getActivities,
  getNearbyActivities,
  getRegisteredActivities,
  getActivityDetail,
  getActivityRegistration,
  getRegistrations,
  createActivity,
  updateActivity,
  submitActivity,
  registerActivity,
  cancelRegistration,
  getComments,
  publishComment,
  getTemplates,
  getTags,
  uploadImage,
  getCheckinQrcode,
  checkin,
  publishSummary,
  getSummary,
  getNotifications,
  markNotificationRead
}
