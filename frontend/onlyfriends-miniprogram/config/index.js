// dev：微信开发者工具本地联调（request 走本机网关 8080）
// release：体验版/正式版（须填写已在微信公众平台备案的 HTTPS 域名）
const ENV = "dev";

const CONFIG = {
  dev: {
    apiBase: "http://localhost:8080/api/v1"
  },
  release: {
    apiBase: ""
  }
};

let localOverride = {};
try {
  localOverride = require("./local");
} catch (e) {
  // optional: config/local.js for machine-specific overrides (gitignored)
}

const selected = CONFIG[ENV] || CONFIG.dev;
const apiBase = localOverride.apiBase || selected.apiBase;

if (!apiBase) {
  throw new Error("请在 config/index.js 的 release.apiBase 或 config/local.js 中配置 API 地址");
}

module.exports = {
  apiBase
};
