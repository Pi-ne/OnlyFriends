const activityApi = require("../../../api/activity");
const drawQrcode = require("../../../utils/weapp-qrcode");

const CHECKIN_STATUSES = [2, 3, 4, 5];

Page({
  data: {
    activityId: null,
    title: "活动签到",
    isOwner: false,
    registered: false,
    locationVerify: false,
    loading: false,
    error: "",
    qrcodeContent: "",
    expiresAt: "",
    generating: false,
    checkingIn: false,
    checkedIn: false,
    checkinTime: "",
    pasteCode: "",
    showPaste: false
  },

  onLoad(options) {
    const activityId = Number(options.id);
    const isOwner = options.owner === "1";
    const title = options.title ? decodeURIComponent(options.title) : "活动签到";
    this.setData({ activityId, isOwner, title });
    wx.setNavigationBarTitle({ title: `${title} · 签到` });
    this.loadPage();
  },

  loadPage() {
    if (!this.data.activityId) {
      this.setData({ error: "活动不存在" });
      return;
    }
    if (!wx.getStorageSync("accessToken")) {
      wx.navigateTo({ url: "/pages/auth/login/index" });
      return;
    }
    this.setData({ loading: true, error: "" });
    activityApi.getActivity(this.data.activityId).then((item) => {
      const isOwner = this.isOwner(item.creatorId);
      const canCheckin = CHECKIN_STATUSES.includes(item.status);
      if (!canCheckin) {
        this.setData({ error: "当前活动状态不支持签到" });
        return;
      }
      this.setData({
        isOwner,
        title: item.title || this.data.title,
        locationVerify: item.locationVerify === true || item.locationVerify === 1
      });
      if (isOwner) {
        return this.generateQrcode(false);
      }
      return activityApi.getMyRegistrationStatus(this.data.activityId).then((status) => {
        const registered = this.isRegistered(status);
        if (!registered) {
          this.setData({ error: "仅已报名用户可签到" });
          return;
        }
        this.setData({ registered: true });
      });
    }).catch((err) => {
      this.setData({ error: err.message || "加载失败" });
    }).finally(() => {
      this.setData({ loading: false });
    });
  },

  isOwner(creatorId) {
    const user = wx.getStorageSync("userInfo") || {};
    const currentUserId = user.userId || user.id;
    return Boolean(creatorId && currentUserId && Number(creatorId) === Number(currentUserId));
  },

  isRegistered(status) {
    return Boolean(status && (status.registrationStatus === 1 || status.registrationStatusText === "registered"));
  },

  generateQrcode(showToast) {
    if (!this.data.isOwner || this.data.generating) {
      return Promise.resolve();
    }
    this.setData({ generating: true, error: "" });
    return activityApi.getCheckinQrcode(this.data.activityId).then((res) => {
      this.setData({
        qrcodeContent: res.qrcodeContent || "",
        expiresAt: this.formatTime(res.expiresAt)
      }, () => {
        this.renderQrcode(res.qrcodeContent || "");
      });
      if (showToast) {
        wx.showToast({ title: "签到码已刷新", icon: "success" });
      }
    }).catch((err) => {
      this.setData({ error: err.message || "生成签到码失败" });
    }).finally(() => {
      this.setData({ generating: false });
    });
  },

  refreshQrcode() {
    this.generateQrcode(true);
  },

  renderQrcode(text) {
    if (!text) {
      return;
    }
    drawQrcode({
      canvasId: "checkinQrcode",
      text,
      width: 260,
      height: 260,
      _this: this,
      callback: () => {}
    });
  },

  copyQrcode() {
    if (!this.data.qrcodeContent) {
      return;
    }
    wx.setClipboardData({
      data: this.data.qrcodeContent,
      success: () => {
        wx.showToast({ title: "签到码已复制", icon: "success" });
      }
    });
  },

  scanCheckin() {
    if (this.data.checkingIn || this.data.checkedIn) {
      return;
    }
    wx.scanCode({
      onlyFromCamera: false,
      scanType: ["qrCode"],
      success: (res) => {
        this.submitCheckin(res.result || "");
      },
      fail: () => {
        wx.showToast({ title: "扫码取消", icon: "none" });
      }
    });
  },

  togglePaste() {
    this.setData({ showPaste: !this.data.showPaste });
  },

  updatePasteCode(event) {
    this.setData({ pasteCode: event.detail.value });
  },

  submitPasteCheckin() {
    const content = this.data.pasteCode.trim();
    if (!content) {
      wx.showToast({ title: "请粘贴签到码", icon: "none" });
      return;
    }
    this.submitCheckin(content);
  },

  submitCheckin(qrcodeContent) {
    if (!qrcodeContent || this.data.checkingIn) {
      return;
    }
    const payload = { qrcodeContent };
    const submit = (data) => {
      this.setData({ checkingIn: true });
      activityApi.checkinActivity(this.data.activityId, data).then((res) => {
        this.setData({
          checkedIn: true,
          checkinTime: this.formatTime(res.checkinTime),
          pasteCode: ""
        });
        wx.showToast({ title: "签到成功", icon: "success" });
      }).catch((err) => {
        wx.showToast({ title: err.message || "签到失败", icon: "none" });
      }).finally(() => {
        this.setData({ checkingIn: false });
      });
    };

    if (!this.data.locationVerify) {
      submit(payload);
      return;
    }

    wx.getLocation({
      type: "gcj02",
      success: (loc) => {
        payload.lat = loc.latitude;
        payload.lng = loc.longitude;
        submit(payload);
      },
      fail: () => {
        wx.showModal({
          title: "需要位置权限",
          content: "该活动开启了位置签到，请授权定位后重试。",
          showCancel: false
        });
      }
    });
  },

  formatTime(value) {
    if (!value) {
      return "";
    }
    const normalized = String(value).replace("T", " ");
    const match = normalized.match(/^\d{4}-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})/);
    if (!match) {
      return normalized;
    }
    return `${Number(match[1])}月${Number(match[2])}日 ${match[3]}:${match[4]}`;
  }
});
