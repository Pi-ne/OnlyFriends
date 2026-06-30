const config = require("./config/index");

App({
  globalData: {
    apiBase: config.apiBase,
    tencentMapKey: "",
    user: null
  }
});
