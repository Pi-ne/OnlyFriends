const { request } = require("../utils/request");

function login(data) {
  return request({ url: "/auth/login", method: "POST", data });
}

function register(data) {
  return request({ url: "/auth/register", method: "POST", data });
}

function getProfile() {
  return request({ url: "/users/me/profile" });
}

module.exports = {
  login,
  register,
  getProfile
};
