const socialApi = require("../../api/social");

Page({
  data: {
    loading: false,
    error: "",
    loggedIn: false,
    teams: [],
    showCreate: false,
    creating: false,
    joinTypes: ["直接加入", "需要审核"],
    form: {
      name: "",
      description: "",
      tags: "",
      joinType: 0,
      maxMembers: ""
    }
  },

  onShow() {
    this.loadTeams();
  },

  loadTeams() {
    const token = wx.getStorageSync("accessToken");
    if (!token) {
      this.setData({
        loading: false,
        error: "",
        loggedIn: false,
        teams: []
      });
      return;
    }

    this.setData({ loading: true, error: "", loggedIn: true });
    socialApi.listTeams().then((list) => {
      const teams = (list || []).map((item) => this.normalizeTeam(item));
      this.setData({ teams });
    }).catch((err) => {
      this.setData({
        teams: [],
        error: err.message || "小队加载失败"
      });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  normalizeTeam(item) {
    return {
      id: item.teamId,
      name: item.name || "未命名小队",
      topic: item.description || "暂无简介",
      tags: item.tags || [],
      members: item.memberCount || 0,
      joined: Boolean(item.joined)
    };
  },

  createTeam() {
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    this.setData({ showCreate: true });
  },

  closeCreate() {
    if (this.data.creating) {
      return;
    }
    this.setData({ showCreate: false });
  },

  stopBubble() {},

  updateForm(event) {
    const key = event.currentTarget.dataset.key;
    this.setData({ [`form.${key}`]: event.detail.value });
  },

  updateJoinType(event) {
    this.setData({ "form.joinType": Number(event.detail.value) });
  },

  submitCreateTeam() {
    const form = this.data.form;
    if (!form.name || !form.maxMembers) {
      wx.showToast({ title: "请填写小队名称和人数上限", icon: "none" });
      return;
    }
    const maxMembers = Number(form.maxMembers);
    if (!maxMembers || maxMembers < 1) {
      wx.showToast({ title: "人数上限至少为 1", icon: "none" });
      return;
    }

    this.setData({ creating: true });
    socialApi.createTeam({
      name: form.name,
      description: form.description,
      tags: this.parseTags(form.tags),
      joinType: Number(form.joinType),
      maxMembers
    }).then(() => {
      wx.showToast({ title: "创建成功", icon: "success" });
      this.setData({
        showCreate: false,
        form: {
          name: "",
          description: "",
          tags: "",
          joinType: 0,
          maxMembers: ""
        }
      });
      this.loadTeams();
    }).catch((err) => {
      wx.showToast({ title: err.message || "创建失败", icon: "none" });
    }).finally(() => {
      this.setData({ creating: false });
    });
  },

  parseTags(value) {
    if (!value) {
      return [];
    }
    return value.split(/[,，\s]+/).map((item) => item.trim()).filter(Boolean).slice(0, 8);
  },

  joinTeam(event) {
    const id = Number(event.currentTarget.dataset.id);
    const name = event.currentTarget.dataset.name;
    const team = this.data.teams.find((item) => item.id === id);
    if (team && team.joined) {
      wx.showToast({ title: "你已加入该小队", icon: "none" });
      return;
    }
    socialApi.joinTeam(id, { message: "" }).then((res) => {
      const applyId = res && res.applyId;
      wx.showToast({
        title: applyId ? `已申请加入${name}` : `已加入${name}`,
        icon: "none"
      });
      this.loadTeams();
    }).catch((err) => {
      wx.showToast({
        title: err.message || "加入失败",
        icon: "none"
      });
    });
  }
});
