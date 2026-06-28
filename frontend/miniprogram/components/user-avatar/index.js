Component({
  properties: {
    src: { type: String, value: '' },
    name: { type: String, value: '' },
    size: { type: Number, value: 72 }
  },
  observers: {
    name(value) {
      this.setData({ letter: (value || '趣').slice(0, 1) })
    }
  },
  data: {
    letter: '趣'
  }
})
