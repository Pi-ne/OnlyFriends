Component({
  properties: {
    title: { type: String, value: '暂无内容' },
    description: { type: String, value: '' },
    buttonText: { type: String, value: '' },
    mark: { type: String, value: '空' }
  },
  methods: {
    handleTap() {
      this.triggerEvent('tap-action')
    }
  }
})
