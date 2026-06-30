const { request } = require("../utils/request");

function listActivities(params) {
  return request({ url: "/activities", data: params });
}

function listRegisteredActivities(params) {
  return request({ url: "/activities/registered", data: params });
}

function listTemplates() {
  return request({ url: "/activities/templates" });
}

function getActivity(id) {
  return request({ url: `/activities/${id}` });
}

function createActivity(data) {
  return request({ url: "/activities", method: "POST", data });
}

function registerActivity(id, data) {
  return request({ url: `/activities/${id}/register`, method: "POST", data });
}

function listNotifications(params) {
  return request({ url: "/notifications", data: params });
}

function markNotificationRead(id) {
  return request({ url: `/notifications/${id}/read`, method: "PUT" });
}

function planActivity(data) {
  return request({ url: "/ai/plan-activity", method: "POST", data });
}

module.exports = {
  listActivities,
  listRegisteredActivities,
  listTemplates,
  getActivity,
  createActivity,
  registerActivity,
  listNotifications,
  markNotificationRead,
  planActivity
};
