const app = getApp();

function isAuthExpired(res, body) {
  const message = body && body.message ? String(body.message) : "";
  return res.statusCode === 401 || message === "Invalid or expired token";
}

function clearAuthAndRedirect() {
  wx.removeStorageSync("accessToken");
  wx.removeStorageSync("refreshToken");
  wx.removeStorageSync("userInfo");
  wx.showToast({ title: "登录已过期，请重新登录", icon: "none" });
  setTimeout(() => {
    wx.navigateTo({ url: "/pages/auth/login/index" });
  }, 600);
}

function request(options) {
  const token = wx.getStorageSync("accessToken");
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.apiBase}${options.url}`,
      method: options.method || "GET",
      data: options.data || {},
      timeout: options.timeout || 15000,
      header: {
        "content-type": "application/json",
        Authorization: token ? `Bearer ${token}` : ""
      },
      success(res) {
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && (body.code === 200 || body.code === undefined)) {
          resolve(body.data === undefined ? body : body.data);
          return;
        }
        if (isAuthExpired(res, body)) {
          clearAuthAndRedirect();
          reject(new Error("登录已过期，请重新登录"));
          return;
        }
        reject(new Error(body.message || "请求失败"));
      },
      fail(err) {
        reject(new Error(err.errMsg || "网络请求失败"));
      }
    });
  });
}

module.exports = {
  request
};
