# OnlyFriends Frontend

包含微信小程序与 Web 开发管理台。

## 目录

| 目录/文件 | 说明 |
|-----------|------|
| `onlyfriends-miniprogram/` | 微信小程序主产品 |
| `server.js` | Web 开发管理台（端口 5173） |
| `index.html` / `app.js` | 管理台静态页面 |

## 快速启动

### Web 开发管理台

```powershell
.\scripts\start-frontend.ps1
```

访问：http://127.0.0.1:5173

### 微信小程序

1. 微信开发者工具导入 `onlyfriends-miniprogram/` 目录（勿导入 `frontend` 根目录）
2. 在 `onlyfriends-miniprogram/app.js` 中确认：
   - `globalData.apiBase`：`http://localhost:8080/api/v1`
3. 开发者工具中关闭域名校验（本地联调）

## 文档

- [小程序使用与测试教程](../docs/frontend/miniprogram-guide.md)
- [小程序前端规划与需求](../docs/frontend/miniprogram-spec.md)
- [Web 开发管理台说明](../docs/frontend/dev-console.md)
- [API 联调指南](../docs/api/overview-zh.md)
