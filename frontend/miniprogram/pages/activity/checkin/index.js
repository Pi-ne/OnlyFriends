const activityService = require('../../../services/activity')
Page({
  data: { id: '', qrcode: null },
  onLoad(options) { this.setData({ id: options.id }) },
  async loadQrcode() {
    const qrcode = await activityService.getCheckinQrcode(this.data.id)
    this.setData({ qrcode })
  },
  scan() {
    wx.scanCode({
      success: async (res) => {
        await activityService.checkin(this.data.id, { qrcodeContent: res.result })
        wx.showToast({ title: '签到成功', icon: 'success' })
      }
    })
  }
})
