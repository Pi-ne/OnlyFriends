const userApi = require("../../../api/user");

Page({
  data: {
    email: "",
    password: "",
    loading: false
  },

  update(event) {
    this.setData({ [event.currentTarget.dataset.key]: event.detail.value });
  },

  login() {
    if (!this.data.email || !this.data.password) {
      wx.showToast({ title: "请输入邮箱和密码", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    userApi.login({
      email: this.data.email,
      password: this.data.password
    }).then((res) => {
      wx.setStorageSync("accessToken", res.accessToken);
      wx.setStorageSync("refreshToken", res.refreshToken);
      wx.setStorageSync("userInfo", res.userInfo);
      wx.showToast({ title: "登录成功", icon: "success" });
      setTimeout(() => wx.switchTab({ url: "/pages/index/index" }), 500);
    }).catch((err) => {
      wx.showToast({ title: err.message || "登录失败", icon: "none" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  goRegister() {
    wx.navigateTo({ url: "/pages/auth/register/index" });
  }
});
