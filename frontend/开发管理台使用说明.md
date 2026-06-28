# OnlyFriends 开发管理台使用说明

该页面是给开发和测试使用的 Web 管理台，入口为：

```text
http://127.0.0.1:5173
```

源码文件：

```text
frontend/index.html
frontend/app.js
frontend/styles.css
frontend/server.js
```

## 启动方式

先启动后端：

```powershell
.\scripts\start-all.ps1 -WithAi -Background
```

再启动前端静态服务：

```powershell
.\scripts\start-frontend.ps1
```

打开：

```text
http://127.0.0.1:5173
```

默认 API Base：

```text
http://localhost:8080/api/v1
```

## 默认管理员

```text
username: admin
password: Admin123456
```

登录成功后，管理台会把管理员 Token 缓存在浏览器 localStorage 中。

## 已支持功能

- 用户查询
- 用户封禁
- 用户解封
- 用户详情查看
- 活动查询
- 活动审核通过
- 活动审核驳回
- 活动要求修改
- 活动下架
- 活动恢复
- 小队查询
- 小队停用
- 小队恢复
- 小队成员查看
- 创建测试用户
- 普通用户登录并保存用户 Token
- 使用普通用户 Token 创建测试活动
- 使用普通用户 Token 创建兴趣组/小队
- 原始接口调试

## 当前后端未提供的能力

管理台不会伪造后端不存在的功能。当前后端公开管理接口中暂未提供：

- 删除用户
- 重置普通用户密码
- 直接编辑任意用户资料

这些能力如果后续需要，可以先在后端补充对应接口，再在 `frontend/app.js` 中接入按钮。

## 使用建议

1. 先登录管理员。
2. 在“概览”确认用户、活动、小队接口可访问。
3. 在“测试造数”登录一个普通用户，获得用户 Token。
4. 使用该用户 Token 创建活动或兴趣组。
5. 回到“活动管理”或“兴趣组/小队”做审核、下架、停用、恢复等操作。
