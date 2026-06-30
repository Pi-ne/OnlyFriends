const app = getApp();

const PUBLIC_AUTH_PATHS = [
  "/auth/wx-login",
  "/auth/login",
  "/auth/register",
  "/auth/refresh",
  "/auth/activate"
];

function isPublicAuthRequest(url) {
  if (!url) {
    return false;
  }
  return PUBLIC_AUTH_PATHS.some((path) => url === path || url.startsWith(path + "?"));
}

function isAuthExpired(res, body) {
  const message = body && body.message ? String(body.message) : "";
  return res.statusCode === 401 || message === "Invalid or expired token";
}

function extractErrorMessage(body) {
  if (!body) {
    return "请求失败";
  }
  const errors = body.data && body.data.errors;
  if (Array.isArray(errors) && errors.length) {
    return errors.map((item) => item.message).filter(Boolean).join("；");
  }
  return body.message || "请求失败";
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
  const publicAuth = isPublicAuthRequest(options.url);
  const token = publicAuth ? "" : wx.getStorageSync("accessToken");
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
          if (!publicAuth) {
            clearAuthAndRedirect();
          }
          reject(new Error(extractErrorMessage(body) || "登录失败，请重试"));
          return;
        }
        reject(new Error(extractErrorMessage(body)));
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
