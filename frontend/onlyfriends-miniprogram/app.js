const config = require("./config/index");
const auth = require("./utils/auth");
const imRealtime = require("./utils/im-realtime");

App({
  globalData: {
    apiBase: config.apiBase,
    wsBase: config.wsBase,
    tencentMapKey: "G4QBZ-4HNKC-FDR2V-AF4YR-A2PPF-M2BNC",
    user: null
  },

  onLaunch() {
    if (auth.hasSession()) {
      imRealtime.ensureConnected();
    }
  },

  onShow() {
    if (auth.hasSession()) {
      imRealtime.ensureConnected();
    }
  }
});
