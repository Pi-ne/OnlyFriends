const { request } = require("../utils/request");

function listTeams(params) {
  return request({ url: "/teams", data: params });
}

function createTeam(data) {
  return request({ url: "/teams", method: "POST", data });
}

function joinTeam(id, data) {
  return request({ url: `/teams/${id}/join`, method: "POST", data });
}

function followUser(userId) {
  return request({ url: `/follows/${userId}`, method: "POST" });
}

function unfollowUser(userId) {
  return request({ url: `/follows/${userId}`, method: "DELETE" });
}

function listFollowing() {
  return request({ url: "/follows/following" });
}

function listFollowers() {
  return request({ url: "/follows/followers" });
}

module.exports = {
  listTeams,
  createTeam,
  joinTeam,
  followUser,
  unfollowUser,
  listFollowing,
  listFollowers
};
