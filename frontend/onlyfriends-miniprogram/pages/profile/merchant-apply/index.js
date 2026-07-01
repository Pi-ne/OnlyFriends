const merchantApi = require("../../../api/merchant");
const userApi = require("../../../api/user");

const FOCUS_TAG_OPTIONS = [
  "户外运动",
  "桌游",
  "音乐演出",
  "美食探店",
  "文化展览",
  "亲子活动",
  "运动健身",
  "电竞游戏",
  "摄影旅拍",
  "学习交流"
];

const STATUS_TEXT = {
  0: "待审核",
  1: "已通过",
  2: "已驳回"
};

Page({
  data: {
    loading: true,
    submitting: false,
    uploading: false,
    editing: false,
    error: "",
    viewMode: "form",
    statusText: "",
    applyStatus: null,
    merchantInfo: null,
    form: {
      merchantName: "",
      licenseUrl: "",
      licensePreview: ""
    },
    focusTags: [],
    tagInput: "",
    presetTagOptions: []
  },

  onLoad() {
    this.loadPage();
  },

  onShow() {
    if (this.data.editing) {
      return;
    }
    if (this._loadedOnce) {
      this.loadPage({ silent: true });
    }
  },

  syncPresetOptions(focusTags) {
    const selected = new Set(focusTags || []);
    return FOCUS_TAG_OPTIONS.map((name) => ({
      name,
      selected: selected.has(name)
    }));
  },

  setFocusTags(focusTags) {
    const tags = focusTags || [];
    this.setData({
      focusTags: tags,
      presetTagOptions: this.syncPresetOptions(tags),
      editing: true
    });
  },

  getSelectedTags() {
    return this.data.focusTags || [];
  },

  loadPage(options = {}) {
    const silent = Boolean(options.silent);
    const token = wx.getStorageSync("accessToken");
    if (!token) {
      wx.navigateTo({ url: "/pages/auth/login/index?redirect=/pages/profile/merchant-apply/index" });
      return;
    }

    if (!silent) {
      this.setData({ loading: true, error: "" });
    }

    Promise.all([
      userApi.getProfile(),
      merchantApi.getApplyStatus().catch(() => null),
      merchantApi.getMerchantInfo().catch(() => null)
    ]).then(([profile, applyStatus, merchantInfo]) => {
      const isMerchant = profile.userType === 1 || Boolean(merchantInfo);
      if (isMerchant) {
        this.setData({
          editing: false,
          viewMode: "merchant",
          merchantInfo: merchantInfo || {
            merchantName: applyStatus && applyStatus.merchantName,
            focusTags: (applyStatus && applyStatus.focusTags) || []
          },
          applyStatus: null
        });
        return;
      }

      if (!applyStatus) {
        this.setData({
          editing: true,
          viewMode: "form",
          applyStatus: null,
          form: {
            merchantName: "",
            licenseUrl: "",
            licensePreview: ""
          },
          focusTags: [],
          tagInput: "",
          presetTagOptions: this.syncPresetOptions([])
        });
        return;
      }

      const status = applyStatus.status;
      if (status === 0) {
        this.setData({
          editing: false,
          viewMode: "pending",
          applyStatus: this.normalizeApply(applyStatus),
          statusText: STATUS_TEXT[0]
        });
        return;
      }

      if (status === 1) {
        this.setData({
          editing: false,
          viewMode: "approved",
          applyStatus: this.normalizeApply(applyStatus),
          statusText: STATUS_TEXT[1]
        });
        return;
      }

      this.setData({
        editing: false,
        viewMode: "rejected",
        applyStatus: this.normalizeApply(applyStatus),
        statusText: STATUS_TEXT[2]
      });
    }).catch((err) => {
      this.setData({ error: err.message || "加载失败，请稍后重试" });
    }).finally(() => {
      this._loadedOnce = true;
      if (!silent) {
        this.setData({ loading: false });
      }
    });
  },

  normalizeApply(applyStatus) {
    return {
      ...applyStatus,
      statusText: STATUS_TEXT[applyStatus.status] || "未知",
      createdAtText: this.formatTime(applyStatus.createdAt),
      reviewedAtText: this.formatTime(applyStatus.reviewedAt),
      focusTags: applyStatus.focusTags || []
    };
  },

  formatTime(value) {
    if (!value) {
      return "";
    }
    const normalized = String(value).replace("T", " ");
    const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/);
    if (!match) {
      return normalized;
    }
    return `${match[1]}年${Number(match[2])}月${Number(match[3])}日 ${match[4]}:${match[5]}`;
  },

  updateMerchantName(event) {
    this.setData({
      "form.merchantName": event.detail.value,
      editing: true
    });
  },

  toggleTag(event) {
    const tag = event.currentTarget.dataset.tag;
    const focusTags = (this.data.focusTags || []).slice();
    const index = focusTags.indexOf(tag);
    if (index >= 0) {
      focusTags.splice(index, 1);
      this.setFocusTags(focusTags);
      return;
    }
    if (focusTags.length >= 10) {
      wx.showToast({ title: "最多添加 10 个领域标签", icon: "none" });
      return;
    }
    focusTags.push(tag);
    this.setFocusTags(focusTags);
  },

  onTagInput(event) {
    this.setData({
      tagInput: event.detail.value,
      editing: true
    });
  },

  addCustomTag() {
    const raw = (this.data.tagInput || "").trim();
    if (!raw) {
      wx.showToast({ title: "请输入领域标签", icon: "none" });
      return;
    }
    if (raw.length > 20) {
      wx.showToast({ title: "单个标签最长 20 字", icon: "none" });
      return;
    }

    const focusTags = (this.data.focusTags || []).slice();
    if (focusTags.includes(raw)) {
      wx.showToast({ title: "该标签已添加", icon: "none" });
      return;
    }
    if (focusTags.length >= 10) {
      wx.showToast({ title: "最多添加 10 个领域标签", icon: "none" });
      return;
    }

    focusTags.push(raw);
    this.setData({
      focusTags,
      tagInput: "",
      presetTagOptions: this.syncPresetOptions(focusTags),
      editing: true
    });
  },

  removeTag(event) {
    const tag = event.currentTarget.dataset.tag;
    const focusTags = (this.data.focusTags || []).filter((item) => item !== tag);
    this.setFocusTags(focusTags);
  },

  chooseLicense() {
    if (this.data.uploading || this.data.submitting) {
      return;
    }
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0];
        if (!file || !file.tempFilePath) {
          return;
        }
        this.uploadLicenseFile(file.tempFilePath);
      }
    });
  },

  uploadLicenseFile(filePath) {
    this.setData({ uploading: true });
    merchantApi.uploadLicense(filePath).then((licenseUrl) => {
      this.setData({
        "form.licenseUrl": licenseUrl,
        "form.licensePreview": filePath,
        editing: true
      });
      wx.showToast({ title: "凭证已上传", icon: "success" });
    }).catch((err) => {
      wx.showToast({ title: err.message || "上传失败", icon: "none" });
    }).finally(() => {
      this.setData({ uploading: false });
    });
  },

  validateForm() {
    const name = (this.data.form.merchantName || "").trim();
    if (!name) {
      wx.showToast({ title: "请填写商家名称", icon: "none" });
      return false;
    }
    if (name.length > 100) {
      wx.showToast({ title: "商家名称最长 100 字", icon: "none" });
      return false;
    }
    if (!this.data.form.licenseUrl) {
      wx.showToast({ title: "请上传营业凭证", icon: "none" });
      return false;
    }
    return true;
  },

  submitApply() {
    if (this.data.submitting || !this.validateForm()) {
      return;
    }

    const payload = {
      merchantName: this.data.form.merchantName.trim(),
      licenseUrl: this.data.form.licenseUrl.trim(),
      focusTags: this.getSelectedTags()
    };

    this.setData({ submitting: true });
    merchantApi.apply(payload).then(() => {
      wx.showToast({ title: "申请已提交", icon: "success" });
      this.setData({ editing: false });
      this.loadPage();
    }).catch((err) => {
      wx.showToast({ title: err.message || "提交失败", icon: "none" });
    }).finally(() => {
      this.setData({ submitting: false });
    });
  },

  retryApply() {
    const tags = (this.data.applyStatus && this.data.applyStatus.focusTags) || [];
    this.setData({
      editing: true,
      viewMode: "form",
      form: {
        merchantName: (this.data.applyStatus && this.data.applyStatus.merchantName) || "",
        licenseUrl: "",
        licensePreview: ""
      },
      focusTags: tags,
      tagInput: "",
      presetTagOptions: this.syncPresetOptions(tags)
    });
  }
});
