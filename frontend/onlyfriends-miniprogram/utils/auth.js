const userApi = require("../api/user");
const imRealtime = require("./im-realtime");

function saveSession(res) {
  wx.setStorageSync("accessToken", res.accessToken);
  wx.setStorageSync("refreshToken", res.refreshToken);
  wx.setStorageSync("userInfo", res.userInfo);
  imRealtime.ensureConnected();
}

function clearSession() {
  imRealtime.disconnect();
  wx.removeStorageSync("accessToken");
  wx.removeStorageSync("refreshToken");
  wx.removeStorageSync("userInfo");
}

function hasSession() {
  return !!wx.getStorageSync("accessToken");
}

function requestWxCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          resolve(res.code);
          return;
        }
        reject(new Error("微信登录失败，请重试"));
      },
      fail() {
        reject(new Error("微信登录失败，请重试"));
      }
    });
  });
}

function loginWithWeChat() {
  clearSession();
  return requestWxCode().then((code) => userApi.wxLogin({ code }));
}

module.exports = {
  saveSession,
  clearSession,
  hasSession,
  loginWithWeChat
};
