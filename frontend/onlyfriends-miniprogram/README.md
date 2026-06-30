# OnlyFriends 微信小程序

微信小程序主工程，通过微信开发者工具导入本目录进行开发与联调。

## 导入方式

1. 打开微信开发者工具。
2. 选择「导入项目」。
3. 项目目录选择本仓库下的 `frontend/onlyfriends-miniprogram/`（即当前目录）。
4. AppID 可使用测试号；`project.config.json` 默认已配置开发者 AppID。

## 本地联调

在 `app.js` 中确认 API 地址：

```javascript
globalData: {
  apiBase: "http://localhost:8080/api/v1",
  // ...
}
```

启动后端后，在开发者工具中关闭「校验合法域名、web-view、TLS 版本以及 HTTPS 证书」。

## 文档

- [小程序使用与测试教程](../../docs/frontend/miniprogram-guide.md)
- [小程序前端规划与需求](../../docs/frontend/miniprogram-spec.md)
- [文档索引](../../docs/README.md)
