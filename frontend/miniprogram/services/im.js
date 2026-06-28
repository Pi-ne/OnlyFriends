const request = require('../utils/request')

function getConversations() {
  return request({ url: '/im/conversations' })
}

function getMessages(convId, params) {
  return request({
    url: `/im/messages/${convId}`,
    data: params
  })
}

function getGroupMessages(teamId, params) {
  return request({
    url: `/im/groups/${teamId}/messages`,
    data: params
  })
}

function sendPrivate(data) {
  return request({
    url: '/im/messages/private',
    method: 'POST',
    data
  })
}

function sendGroup(data) {
  return request({
    url: '/im/messages/group',
    method: 'POST',
    data
  })
}

function recallMessage(msgId) {
  return request({
    url: `/im/messages/${msgId}/recall`,
    method: 'POST'
  })
}

function markConversationRead(convId, lastReadMsgId) {
  return request({
    url: `/im/conversations/${convId}/read`,
    method: 'POST',
    data: { lastReadMsgId }
  })
}

module.exports = {
  getConversations,
  getMessages,
  getGroupMessages,
  sendPrivate,
  sendGroup,
  recallMessage,
  markConversationRead
}
