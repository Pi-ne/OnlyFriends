Component({
  properties: {
    team: { type: Object, value: {} }
  },
  observers: {
    team(value) {
      this.setData({
        letter: (value && value.name ? value.name : '队').slice(0, 1),
        joinTypeText: value && value.joinType === 1 ? '需审核' : '可加入'
      })
    }
  },
  data: {
    letter: '队',
    joinTypeText: '可加入'
  },
  methods: {
    handleDetail() {
      this.triggerEvent('tap-detail', { team: this.data.team })
    }
  }
})
