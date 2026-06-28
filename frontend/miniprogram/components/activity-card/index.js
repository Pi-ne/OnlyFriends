const format = require('../../utils/format')

Component({
  properties: {
    activity: { type: Object, value: {} }
  },
  observers: {
    activity(value) {
      const tags = value && value.tags ? value.tags : []
      this.setData({
        firstTag: tags[0] || '活动',
        timeText: format.formatDateTime(value && value.startTime),
        feeText: format.formatFee(value && value.fee),
        distanceText: format.formatDistance(value && value.distanceMeters)
      })
    }
  },
  data: {
    firstTag: '活动',
    timeText: '',
    feeText: '',
    distanceText: ''
  },
  methods: {
    handleDetail() {
      this.triggerEvent('tap-detail', { activity: this.data.activity })
    }
  }
})
