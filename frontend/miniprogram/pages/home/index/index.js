const activityService = require('../../../services/activity')
const format = require('../../../utils/format')

Page({
  data: {
    city: '当前城市',
    tabs: [
      { key: 'recommend', name: '推荐' },
      { key: 'latest', name: '最新' },
      { key: 'nearby', name: '附近' }
    ],
    activeTab: 'recommend',
    keyword: '',
    filters: {
      tags: '',
      minFee: '',
      maxFee: ''
    },
    showFilter: false,
    activities: [],
    page: 1,
    size: 20,
    total: 0,
    hasMore: true,
    loading: false,
    state: 'loading',
    location: null
  },

  onLoad() {
    this.refresh()
  },

  onPullDownRefresh() {
    this.refresh().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadActivities(false)
    }
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value })
  },

  onTagsInput(event) {
    this.setData({ 'filters.tags': event.detail.value })
  },

  onMinFeeInput(event) {
    this.setData({ 'filters.minFee': event.detail.value })
  },

  onMaxFeeInput(event) {
    this.setData({ 'filters.maxFee': event.detail.value })
  },

  toggleFilter() {
    this.setData({ showFilter: !this.data.showFilter })
  },

  applyFilter() {
    this.setData({ showFilter: false })
    this.refresh()
  },

  switchTab(event) {
    const key = event.currentTarget.dataset.key
    if (key === this.data.activeTab) return
    this.setData({ activeTab: key })
    this.refresh()
  },

  refresh() {
    this.setData({ page: 1, hasMore: true })
    return this.loadActivities(true)
  },

  async loadActivities(reset) {
    this.setData({ loading: true, state: reset ? 'loading' : this.data.state })
    try {
      const params = await this.buildParams()
      const data = this.data.activeTab === 'nearby'
        ? await activityService.getNearbyActivities(params)
        : await activityService.getActivities(params)
      const pageResult = format.normalizePageResult(data)
      const nextList = reset ? pageResult.list : this.data.activities.concat(pageResult.list)
      const loaded = nextList.length
      this.setData({
        activities: nextList,
        total: pageResult.total,
        page: this.data.page + 1,
        hasMore: loaded < pageResult.total && pageResult.list.length > 0,
        state: nextList.length ? 'success' : 'empty'
      })
    } catch (error) {
      this.setData({ state: this.data.activities.length ? 'success' : 'error' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async buildParams() {
    const params = {
      tab: this.data.activeTab,
      keyword: this.data.keyword.trim(),
      tags: this.data.filters.tags.trim(),
      minFee: this.data.filters.minFee,
      maxFee: this.data.filters.maxFee,
      page: this.data.page,
      size: this.data.size
    }

    if (this.data.activeTab === 'nearby') {
      const location = await this.getLocation()
      params.lat = location.latitude
      params.lng = location.longitude
      params.radius = 5000
    }

    Object.keys(params).forEach((key) => {
      if (params[key] === '' || params[key] === null || typeof params[key] === 'undefined') {
        delete params[key]
      }
    })
    return params
  },

  getLocation() {
    if (this.data.location) {
      return Promise.resolve(this.data.location)
    }
    return new Promise((resolve, reject) => {
      wx.getLocation({
        type: 'gcj02',
        success: (res) => {
          this.setData({ location: res })
          resolve(res)
        },
        fail: reject
      })
    })
  },

  goDetail(event) {
    const activity = event.detail.activity
    wx.navigateTo({ url: `/pages/activity/detail/index?id=${activity.activityId}` })
  },

  goMap() {
    wx.navigateTo({ url: '/pages/activity/map/index' })
  }
})
