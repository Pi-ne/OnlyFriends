const userApi = require("../../../api/user");

Page({
  data: {
    email: "",
    nickname: "",
    password: "",
    confirmPassword: "",
    loading: false
  },

  update(event) {
    this.setData({ [event.currentTarget.dataset.key]: event.detail.value });
  },

  register() {
    if (!this.data.email || !this.data.nickname || this.data.password.length < 8 || this.data.password !== this.data.confirmPassword) {
      wx.showToast({ title: "请检查邮箱、昵称和密码", icon: "none" });
      return;
    }
    this.setData({ loading: true });
    userApi.register({
      email: this.data.email,
      nickname: this.data.nickname,
      password: this.data.password
    }).then((res) => {
      wx.showModal({
        title: "注册成功",
        content: `用户 ID：${res.userId}。请查看后端日志中的激活链接后再登录。`,
        showCancel: false,
        success: () => wx.navigateBack()
      });
    }).catch((err) => {
      wx.showToast({ title: err.message || "注册失败", icon: "none" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  }
});
