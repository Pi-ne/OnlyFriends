const request = require('../utils/request')

function planActivity(data) {
  return request({
    url: '/ai/plan-activity',
    method: 'POST',
    data
  })
}

function classifyImages(data) {
  return request({
    url: '/ai/classify-images',
    method: 'POST',
    data
  })
}

function reviewContent(data) {
  return request({
    url: '/ai/review-content',
    method: 'POST',
    data
  })
}

module.exports = {
  planActivity,
  classifyImages,
  reviewContent
}
