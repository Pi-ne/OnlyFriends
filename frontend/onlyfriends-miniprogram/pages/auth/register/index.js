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

  validateForm() {
    const email = (this.data.email || "").trim();
    const nickname = (this.data.nickname || "").trim();
    const password = this.data.password || "";
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return "请输入正确的邮箱格式";
    }
    if (!nickname || nickname.length < 2 || nickname.length > 20) {
      return "昵称长度需在2-20个字符之间";
    }
    if (!password || password.length < 8 || password.length > 20) {
      return "密码长度需在8-20位之间";
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(password)) {
      return "密码需同时包含字母和数字";
    }
    if (password !== this.data.confirmPassword) {
      return "两次输入的密码不一致";
    }
    return "";
  },

  register() {
    const validationError = this.validateForm();
    if (validationError) {
      wx.showToast({ title: validationError, icon: "none" });
      return;
    }
    this.setData({ loading: true });
    userApi.register({
      email: this.data.email.trim(),
      nickname: this.data.nickname.trim(),
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
