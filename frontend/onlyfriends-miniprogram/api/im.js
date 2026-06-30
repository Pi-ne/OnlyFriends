const { request } = require("../utils/request");

function listConversations() {
  return request({ url: "/im/conversations" });
}

function listMessages(convId, params) {
  return request({ url: `/im/messages/${convId}`, data: params });
}

function sendPrivateMessage(data) {
  return request({ url: "/im/messages/private", method: "POST", data });
}

function markConversationRead(convId, lastReadMsgId) {
  return request({
    url: `/im/conversations/${convId}/read`,
    method: "POST",
    data: { lastReadMsgId }
  });
}

function recallMessage(msgId) {
  return request({ url: `/im/messages/${msgId}/recall`, method: "POST" });
}

module.exports = {
  listConversations,
  listMessages,
  sendPrivateMessage,
  markConversationRead,
  recallMessage
};
