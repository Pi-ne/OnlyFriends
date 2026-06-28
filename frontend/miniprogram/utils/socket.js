function createImSocket(options) {
  const app = getApp({ allowDefault: true })
  const token = wx.getStorageSync('accessToken')
  const wsUrl = app && app.globalData && app.globalData.wsUrl ? app.globalData.wsUrl : 'ws://localhost:8080/ws/im'
  let socketTask = null

  function connect() {
    if (!token) return null
    socketTask = wx.connectSocket({
      url: `${wsUrl}?token=${encodeURIComponent(token)}`
    })

    socketTask.onOpen(() => {
      if (options && options.onOpen) options.onOpen()
    })
    socketTask.onMessage((event) => {
      let data = event.data
      try {
        data = JSON.parse(event.data)
      } catch (error) {}
      if (options && options.onMessage) options.onMessage(data)
    })
    socketTask.onClose(() => {
      if (options && options.onClose) options.onClose()
    })
    socketTask.onError((error) => {
      if (options && options.onError) options.onError(error)
    })
    return socketTask
  }

  function send(data) {
    if (!socketTask) return
    socketTask.send({
      data: typeof data === 'string' ? data : JSON.stringify(data)
    })
  }

  function close() {
    if (socketTask) {
      socketTask.close({})
      socketTask = null
    }
  }

  return { connect, send, close }
}

module.exports = {
  createImSocket
}
