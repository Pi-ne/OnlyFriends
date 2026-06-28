const activityService = require('../../../services/activity')
const format = require('../../../utils/format')
Page({
  data: { lat: 30.2741, lng: 120.1551, activities: [], markers: [], selected: null },
  onLoad() { this.load() },
  load() {
    wx.getLocation({
      type: 'gcj02',
      success: async (loc) => {
        const result = await activityService.getNearbyActivities({ lat: loc.latitude, lng: loc.longitude, radius: 5000, page: 1, size: 20 })
        const activities = format.normalizePageResult(result).list
        this.setData({
          lat: loc.latitude,
          lng: loc.longitude,
          activities,
          markers: activities.map((item, index) => ({
            id: index,
            latitude: Number(item.locationLat),
            longitude: Number(item.locationLng),
            title: item.title
          }))
        })
      }
    })
  },
  onMarkerTap(e) { this.setData({ selected: this.data.activities[e.detail.markerId] }) },
  goDetail() { wx.navigateTo({ url: `/pages/activity/detail/index?id=${this.data.selected.activityId}` }) }
})
