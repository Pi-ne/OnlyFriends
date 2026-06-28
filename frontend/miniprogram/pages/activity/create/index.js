const activityService = require('../../../services/activity')
const aiService = require('../../../services/ai')
const route = require('../../../utils/route')
const format = require('../../../utils/format')

function today() {
  return format.formatDate(new Date())
}

Page({
  data: {
    steps: [
      { key: 'base', name: '基础' },
      { key: 'time', name: '时间地点' },
      { key: 'rule', name: '规则' },
      { key: 'desc', name: '说明' }
    ],
    step: 'base',
    date: today(),
    startClock: '09:00',
    endClock: '11:00',
    deadlineClock: '08:00',
    tagText: '',
    form: {
      title: '',
      description: '',
      tags: [],
      coverUrl: '',
      locationName: '',
      locationDetail: '',
      locationLat: 30.2741,
      locationLng: 120.1551,
      maxParticipants: 12,
      fee: 0,
      locationVerify: false,
      locationRadius: 200
    },
    submitting: false,
    aiLoading: false
  },

  onShow() {
    route.requireLogin('/pages/activity/create/index')
  },

  switchStep(event) {
    this.setData({ step: event.currentTarget.dataset.key })
  },

  onTitleInput(event) { this.setData({ 'form.title': event.detail.value }) },
  onTagsInput(event) {
    const tagText = event.detail.value
    this.setData({ tagText, 'form.tags': format.splitTags(tagText) })
  },
  onCoverInput(event) { this.setData({ 'form.coverUrl': event.detail.value }) },
  onDateChange(event) { this.setData({ date: event.detail.value }) },
  onStartClockChange(event) { this.setData({ startClock: event.detail.value }) },
  onEndClockChange(event) { this.setData({ endClock: event.detail.value }) },
  onDeadlineClockChange(event) { this.setData({ deadlineClock: event.detail.value }) },
  onLocationNameInput(event) { this.setData({ 'form.locationName': event.detail.value }) },
  onLocationDetailInput(event) { this.setData({ 'form.locationDetail': event.detail.value }) },
  onLatInput(event) { this.setData({ 'form.locationLat': Number(event.detail.value) }) },
  onLngInput(event) { this.setData({ 'form.locationLng': Number(event.detail.value) }) },
  onMaxInput(event) { this.setData({ 'form.maxParticipants': Number(event.detail.value) }) },
  onFeeInput(event) { this.setData({ 'form.fee': Number(event.detail.value || 0) }) },
  onDescInput(event) { this.setData({ 'form.description': event.detail.value }) },
  toggleLocationVerify() { this.setData({ 'form.locationVerify': !this.data.form.locationVerify }) },

  useLocation() {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        this.setData({
          'form.locationLat': res.latitude,
          'form.locationLng': res.longitude
        })
      }
    })
  },

  async aiPlan() {
    this.setData({ aiLoading: true })
    try {
      const result = await aiService.planActivity({
        theme: this.data.form.title || this.data.tagText || '兴趣活动',
        locationName: this.data.form.locationName,
        startTime: this.combineDateTime(this.data.startClock),
        durationHours: 2,
        maxParticipants: Number(this.data.form.maxParticipants || 12),
        preferences: this.data.form.tags
      })
      this.applyAiResult(result)
      wx.showToast({ title: '已生成建议', icon: 'success' })
    } finally {
      this.setData({ aiLoading: false })
    }
  },

  applyAiResult(result) {
    if (!result) return
    const title = result.title || this.data.form.title
    const description = result.description || result.plan || this.data.form.description
    const tags = result.tags || this.data.form.tags
    this.setData({
      'form.title': title,
      'form.description': description,
      'form.tags': tags,
      tagText: tags.join('，')
    })
  },

  validateForSubmit() {
    const form = this.data.form
    if (!form.title || !form.locationName || !form.maxParticipants) {
      wx.showToast({ title: '请补充标题、地点和人数', icon: 'none' })
      return false
    }
    return true
  },

  buildPayload(isDraft) {
    const form = this.data.form
    return Object.assign({}, form, {
      tags: format.splitTags(this.data.tagText),
      startTime: this.combineDateTime(this.data.startClock),
      endTime: this.combineDateTime(this.data.endClock),
      regDeadline: this.combineDateTime(this.data.deadlineClock),
      locationVerify: form.locationVerify ? 1 : 0,
      isDraft
    })
  },

  combineDateTime(clock) {
    return `${this.data.date}T${clock}:00`
  },

  async saveDraft() {
    if (!route.requireLogin('/pages/activity/create/index')) return
    this.setData({ submitting: true })
    try {
      await activityService.createActivity(this.buildPayload(true))
      wx.showToast({ title: '草稿已保存', icon: 'success' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async submitReview() {
    if (!route.requireLogin('/pages/activity/create/index')) return
    if (!this.validateForSubmit()) return
    this.setData({ submitting: true })
    try {
      await activityService.createActivity(this.buildPayload(false))
      wx.showToast({ title: '已提交审核', icon: 'success' })
      wx.switchTab({ url: '/pages/profile/index/index' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
