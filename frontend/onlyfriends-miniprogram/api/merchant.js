const { request } = require("../utils/request");

function getApplyStatus() {
  return request({ url: "/merchant/apply/status" });
}

function apply(data) {
  return request({ url: "/merchant/apply", method: "POST", data });
}

function getMerchantInfo() {
  return request({ url: "/merchant/me" });
}

function uploadLicense(filePath) {
  const app = getApp();
  const token = wx.getStorageSync("accessToken");
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${app.globalData.apiBase}/merchant/license`,
      filePath,
      name: "file",
      header: {
        Authorization: token ? `Bearer ${token}` : ""
      },
      success(res) {
        let body = {};
        try {
          body = JSON.parse(res.data || "{}");
        } catch (e) {
          reject(new Error("上传响应解析失败"));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && (body.code === 200 || body.code === undefined)) {
          const data = body.data || body;
          resolve(data.licenseUrl || "");
          return;
        }
        reject(new Error(body.message || "营业执照上传失败"));
      },
      fail(err) {
        reject(new Error((err && err.errMsg) || "营业执照上传失败"));
      }
    });
  });
}

module.exports = {
  getApplyStatus,
  apply,
  getMerchantInfo,
  uploadLicense
};
