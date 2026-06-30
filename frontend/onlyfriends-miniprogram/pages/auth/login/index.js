const auth = require("../../../utils/auth");

Page({
  data: {
    loading: false,
    redirect: ""
  },

  onLoad(options) {
    if (options.redirect) {
      this.setData({ redirect: decodeURIComponent(options.redirect) });
    }
    if (auth.hasSession()) {
      this.navigateAfterLogin();
    }
  },

  wechatLogin() {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    auth.loginWithWeChat().then((res) => {
      auth.saveSession(res);
      wx.showToast({ title: "登录成功", icon: "success" });
      setTimeout(() => this.navigateAfterLogin(), 400);
    }).catch((err) => {
      wx.showToast({ title: err.message || "登录失败，请重试", icon: "none" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  navigateAfterLogin() {
    const redirect = this.data.redirect;
    if (redirect) {
      wx.redirectTo({
        url: redirect,
        fail: () => wx.switchTab({ url: "/pages/index/index" })
      });
      return;
    }
    wx.switchTab({ url: "/pages/index/index" });
  }
});
