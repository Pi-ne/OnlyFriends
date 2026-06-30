# OnlyFriends 小程序前端规划与需求说明

> 文档定位：描述小程序前端目标、页面结构与接口映射。  
> **当前实现**：`frontend/onlyfriends-miniprogram/` 为当前小程序工程，可与网关 `http://localhost:8080/api/v1` 联调。

---

## 1. 前端目标

OnlyFriends 小程序的前端需要承担普通用户和商家用户的核心使用入口，主要目标如下：

1. 用户可以注册、登录、维护个人资料。
2. 用户可以浏览、搜索、筛选、查看活动详情。
3. 用户可以报名活动、取消报名、查看报名状态、签到、评价、查看总结。
4. 用户可以创建活动，并支持保存草稿、提交审核、AI 辅助策划。
5. 用户可以关注、添加好友、管理好友申请。
6. 用户可以创建和加入兴趣小队，查看小队成员、公告、相册、文件、积分、投票。
7. 用户可以进行私聊和小队群聊。
8. 用户可以查看通知、个人活动记录、商家申请状态。

设计风格要求：

- 简洁、清晰、低学习成本。
- 所有按钮必须语义明确，不使用含糊文案。
- 页面层级不宜过深，用户应能在 2 到 3 次点击内到达主要功能。
- 主导航固定使用底部 Tab。
- 页面顶部只保留当前页面标题、搜索、筛选、必要操作入口。
- 表单页面采用分组布局，减少一次性信息压力。

---

## 2. 技术路线

建议采用原生微信小程序开发。

### 2.1 推荐技术栈

| 类型 | 选择 | 说明 |
|---|---|---|
| 小程序框架 | 原生微信小程序 | 与后端接口直连简单，适合课程项目和真实演示 |
| UI 组件库 | Vant Weapp | 表单、弹窗、Toast、Tabs、Uploader、Dialog 成熟 |
| 请求封装 | 自定义 `utils/request.js` | 统一 baseURL、Token、错误处理、刷新 Token |
| 状态管理 | `app.globalData` + 本地缓存 | 项目规模中等，暂不引入复杂状态库 |
| 地图能力 | 腾讯地图小程序 SDK | 活动位置选择、附近活动、地图展示 |
| 即时通讯 | `wx.connectSocket` + STOMP 协议封装 | 对接后端 `/ws/im` |

### 2.2 当前工程目录

实际小程序工程位于 `frontend/onlyfriends-miniprogram/`：

```text
frontend/onlyfriends-miniprogram/
  app.js
  app.json
  app.wxss
  project.config.json

  config/
    index.js              # API Base（dev / release）
    local.example.js      # 本机覆盖示例（复制为 local.js，已 gitignore）

  pages/
    index/                # 活动首页（Tab）
    search/
    activity/
      detail/
      create/             # 发布活动（Tab）
      map/
    social/               # 社群与小队（Tab）
    im/
      index/              # 消息列表（Tab）
      chat/
    profile/
      index/              # 我的（Tab）
      my-activities/
      registrations/
      follows/
    auth/
      login/
      register/

  api/                    # user、activity、social、im 等接口封装
  utils/                  # request.js、auth.js 等
  assets/                 # tabbar 图标等
```

Web 开发管理台为 `frontend/server.js`（端口 5173），与小程序并列。

---

## 3. 页面导航结构

### 3.1 底部 TabBar

当前实现保留 5 个主 Tab（见 `app.json` → `tabBar`）：

| Tab | 页面路径 | 作用 |
|---|---|---|
| 活动 | `pages/index/index` | 活动列表、搜索、筛选、附近活动入口 |
| 发布 | `pages/activity/create/index` | 创建活动、AI 策划、草稿保存 |
| 社群 | `pages/social/index` | 兴趣小队列表、我的小队、创建小队 |
| 消息 | `pages/im/index` | 私聊、群聊会话列表 |
| 我的 | `pages/profile/index` | 个人资料、我的活动、关注/好友等 |

### 3.2 登录拦截规则

未登录用户可以访问：

- 登录页
- 注册页
- 首页活动列表
- 活动详情页的公开信息
- 小队详情页的公开信息

需要登录后访问：

- 发起活动
- 报名活动
- 签到
- 评价
- 关注、好友、小队加入
- 消息
- 我的
- 商家申请

跳转策略：

```text
用户点击需要登录的按钮
  -> 检查 accessToken
  -> 无 Token：跳转到 /pages/auth/login/index，并携带 redirect 参数
  -> 登录成功：返回原目标页面
```

示例：

```text
/pages/auth/login/index?redirect=/pages/activity/detail/index?id=10001
```

---

## 4. 页面规划

## 4.1 登录页

路径：

```text
/pages/auth/login/index
```

页面目标：

- 完成邮箱和密码登录。
- 登录成功后保存 `accessToken`、`refreshToken`、`userInfo`。
- 如果 URL 中带有 `redirect`，登录后回到原页面。

页面布局：

```text
顶部：品牌名 + 欢迎文案
中部：邮箱输入框、密码输入框、登录按钮
底部：注册入口、忘记密码入口
```

主要按钮：

- `登录`
- `去注册`

接口：

```text
POST /auth/login
```

---

## 4.2 注册页

路径：

```text
/pages/auth/register/index
```

页面目标：

- 用户填写邮箱、密码、昵称。
- 提交注册。
- 提示用户查看邮箱激活。

主要按钮：

- `注册账号`
- `返回登录`

接口：

```text
POST /auth/register
```

---

## 4.3 首页：活动发现

路径：

```text
/pages/index/index
```

页面目标：

- 展示推荐、最新、附近三个活动流。
- 支持关键词搜索。
- 支持分类、日期、费用、人数等筛选。
- 支持跳转活动详情。
- 支持跳转地图模式。

页面布局：

```text
顶部：
  当前城市 / 搜索框 / 筛选按钮

Tab：
  推荐 / 最新 / 附近

内容：
  活动卡片列表

悬浮或顶部入口：
  地图模式
```

活动卡片字段：

- 封面图
- 活动标题
- 标签
- 时间
- 地点
- 报名人数 / 人数上限
- 费用
- 状态：可报名、候补、已截止、进行中、已结束
- 明确按钮：`查看详情`

接口：

```text
GET /activities?tab=recommend&page=1&size=20
GET /activities?tab=latest&page=1&size=20
GET /activities?tab=nearby&lat={lat}&lng={lng}&radius=5000
```

交互：

```text
点击活动卡片
  -> wx.navigateTo('/pages/activity/detail/index?id=xxx')

点击筛选
  -> 打开筛选弹层
  -> 用户确认
  -> 重新请求活动列表

点击附近
  -> 请求定位权限
  -> 获取 lat/lng
  -> 请求附近活动
```

---

## 4.4 活动详情页

路径：

```text
/pages/activity/detail/index
```

页面目标：

- 展示活动完整信息。
- 根据活动状态显示不同操作。
- 用户可以报名、取消报名、加入候补、查看评论、发起签到或参与签到。

页面布局：

```text
顶部：活动封面
主体：
  标题、状态、标签
  时间、地点、费用、人数
  发起人信息
  活动说明
  报名成员
  评论列表
底部固定操作栏：
  根据状态显示按钮
```

底部按钮规则：

| 状态 | 用户身份 | 按钮 |
|---|---|---|
| 可报名 | 未报名用户 | `立即报名` |
| 已满员 | 未报名用户 | `加入候补` |
| 已报名 | 参与者 | `取消报名` |
| 活动当天 | 参与者 | `扫码签到` |
| 活动进行中 | 发起人 | `生成签到码` |
| 已结束 | 参与者 | `发布评价` |
| 已结束 | 发起人 | `发布总结` |

接口：

```text
GET /activities/{id}
GET /activities/{id}/comments?page=1&size=10
POST /activities/{id}/register
DELETE /activities/{id}/register
GET /activities/{id}/registration/me
```

---

## 4.5 发起活动页

路径：

```text
/pages/activity/create/index
```

页面目标：

- 用户可以手动创建活动。
- 可以使用 AI 策划辅助生成标题、说明、流程建议。
- 可以保存草稿或提交审核。

页面布局建议采用分步表单：

```text
步骤 1：基础信息
  活动标题
  活动类型
  标签
  封面图

步骤 2：时间地点
  开始时间
  结束时间
  报名截止时间
  地点选择

步骤 3：报名规则
  人数上限
  费用
  是否需要位置签到

步骤 4：活动说明
  活动介绍
  注意事项
  AI 帮我完善

底部：
  保存草稿 / 提交审核
```

主要按钮：

- `AI 帮我策划`
- `保存草稿`
- `提交审核`

接口：

```text
GET /activities/templates
POST /ai/plan-activity
POST /activities
POST /activities/{id}/submit
POST /activities/images
```

交互：

```text
点击 AI 帮我策划
  -> 弹出策划输入框
  -> 用户填写主题、地点、时长、人数
  -> 调用 POST /ai/plan-activity
  -> 返回后填充表单

点击保存草稿
  -> POST /activities，isDraft=true

点击提交审核
  -> 表单校验
  -> POST /activities，isDraft=false
  -> 成功后跳转到我的活动
```

---

## 4.6 地图模式页

路径：

```text
/pages/activity/map/index
```

页面目标：

- 在地图上展示附近活动。
- 点击标记后展示活动简要卡片。
- 支持跳转活动详情。

布局：

```text
全屏地图
顶部：返回 / 搜索附近
地图标记：活动地点
底部：当前选中活动卡片
```

接口：

```text
GET /activities/nearby?lat={lat}&lng={lng}&radius=5000
```

---

## 4.7 签到页

路径：

```text
/pages/activity/checkin/index
```

页面目标：

- 发起人生成签到二维码。
- 参与者扫码签到。

页面分支：

```text
发起人进入：
  显示二维码
  显示已签到人数

参与者进入：
  显示扫码签到按钮
  显示签到结果
```

接口：

```text
GET /activities/{id}/checkin/qrcode
POST /activities/{id}/checkin
```

---

## 4.8 小队列表页

路径：

```text
/pages/team/list/index
```

页面目标：

- 展示我的小队。
- 展示推荐小队。
- 支持搜索小队。
- 支持创建小队。

页面布局：

```text
顶部：搜索框 / 创建小队按钮
区域 1：我的小队
区域 2：推荐小队
```

接口：

```text
GET /teams?joined=true
GET /teams?keyword=xxx
POST /teams
```

---

## 4.9 小队详情页

路径：

```text
/pages/team/detail/index
```

页面目标：

- 展示小队资料、成员、公告、活动、相册、文件、积分、投票。
- 用户可以申请加入或退出。
- 队长和管理员可以管理成员。

页面结构：

```text
顶部：
  小队名称、标签、成员数、加入按钮

Tabs：
  动态 / 成员 / 相册 / 文件 / 投票 / 积分
```

接口：

```text
GET /teams/{id}
GET /teams/{id}/members
GET /teams/{id}/album?page=1
GET /teams/{id}/files
GET /teams/{id}/votes
GET /teams/{id}/scores
POST /teams/{id}/join
DELETE /teams/{id}/members/me
```

---

## 4.10 社交好友页

路径：

```text
/pages/social/friends/index
```

页面目标：

- 展示好友列表。
- 展示关注和粉丝。
- 处理好友申请。

布局：

```text
顶部：搜索用户
Tabs：好友 / 关注 / 粉丝 / 申请
列表：用户头像、昵称、关系状态、操作按钮
```

接口：

```text
GET /friends
GET /friends/applies
PUT /friends/applies/{id}
GET /follows/following
GET /follows/followers
POST /follows/{userId}
DELETE /follows/{userId}
```

---

## 4.11 消息页

路径：

```text
/pages/im/conversations/index
```

页面目标：

- 展示私聊和小队群聊会话。
- 展示未读数。
- 点击会话进入聊天页。

接口：

```text
GET /im/conversations
```

跳转：

```text
点击会话
  -> /pages/im/chat/index?convId=xxx&type=private
  -> /pages/im/chat/index?teamId=xxx&type=group
```

---

## 4.12 聊天页

路径：

```text
/pages/im/chat/index
```

页面目标：

- 展示历史消息。
- 支持发送文本消息。
- 支持接收 WebSocket 实时消息。
- 支持撤回消息。

页面布局：

```text
顶部：会话名称
主体：消息列表
底部：输入框 / 发送按钮
```

接口：

```text
GET /im/messages/{convId}?page=1&size=30
GET /im/groups/{teamId}/messages?page=1&size=30
POST /im/messages/{msgId}/recall
POST /im/conversations/{convId}/read
```

WebSocket：

```text
连接：ws://localhost:8080/ws/im?token={accessToken}
私聊发送：/app/chat.private
群聊发送：/app/chat.group
撤回：/app/chat.recall
私聊订阅：/user/{userId}/queue/messages
群聊订阅：/topic/team/{teamId}
```

---

## 4.13 我的页面

路径：

```text
/pages/profile/index/index
```

页面目标：

- 展示用户资料。
- 展示我的活动、报名记录、小队、通知、商家申请入口。

布局：

```text
顶部：头像、昵称、简介、信誉分
快捷数据：已报名、已发布、我的小队
菜单：
  编辑资料
  我的活动
  报名记录
  我的通知
  商家申请
  设置
```

接口：

```text
GET /users/me/profile
GET /activities?creatorId={myId}
GET /activities/registered
GET /teams?joined=true
GET /notifications?page=1&size=20
```

---

## 5. 路由跳转规范

### 5.1 Tab 页面跳转

Tab 页面必须使用：

```javascript
wx.switchTab({ url: '/pages/index/index' })
```

适用页面：

- 首页
- 发起
- 小队
- 消息
- 我的

### 5.2 普通详情页跳转

详情页使用：

```javascript
wx.navigateTo({ url: '/pages/activity/detail/index?id=10001' })
```

适用页面：

- 活动详情
- 小队详情
- 聊天页
- 编辑资料
- 商家申请

### 5.3 返回上一页

```javascript
wx.navigateBack()
```

### 5.4 登录后重定向

登录页读取 `redirect` 参数：

```javascript
const redirect = options.redirect
```

登录成功后：

```javascript
if (redirect) {
  wx.redirectTo({ url: decodeURIComponent(redirect) })
} else {
  wx.switchTab({ url: '/pages/index/index' })
}
```

---

## 6. 统一请求封装

建议在 `utils/request.js` 中统一封装：

```javascript
const BASE_URL = 'http://localhost:8080/api/v1'

function request(options) {
  const token = wx.getStorageSync('accessToken')

  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      },
      success(res) {
        const body = res.data

        if (body.code === 200) {
          resolve(body.data)
          return
        }

        if (body.code === 401) {
          wx.removeStorageSync('accessToken')
          wx.removeStorageSync('refreshToken')
          wx.navigateTo({
            url: '/pages/auth/login/index'
          })
          reject(body)
          return
        }

        wx.showToast({
          title: body.message || '请求失败',
          icon: 'none'
        })
        reject(body)
      },
      fail(error) {
        wx.showToast({
          title: '网络异常，请稍后重试',
          icon: 'none'
        })
        reject(error)
      }
    })
  })
}

module.exports = request
```

所有业务接口禁止直接调用 `wx.request`，必须通过 `request.js`。

---

## 7. API 服务层设计

每个业务域单独建立 service 文件。

示例：`services/activity.js`

```javascript
const request = require('../utils/request')

function getActivities(params) {
  return request({
    url: '/activities',
    method: 'GET',
    data: params
  })
}

function getActivityDetail(id) {
  return request({
    url: `/activities/${id}`,
    method: 'GET'
  })
}

function registerActivity(id) {
  return request({
    url: `/activities/${id}/register`,
    method: 'POST'
  })
}

module.exports = {
  getActivities,
  getActivityDetail,
  registerActivity
}
```

页面只调用 service，不直接拼接口路径。

---

## 8. 组件规划

### 8.1 ActivityCard

路径：

```text
components/activity-card/
```

入参：

```javascript
{
  activity: Object
}
```

事件：

```text
tap-detail
tap-register
```

用途：

- 首页活动流
- 我的活动
- 小队活动

### 8.2 TeamCard

用途：

- 小队列表
- 我的页面小队预览

字段：

- 小队名称
- 标签
- 成员数
- 加入方式
- 操作按钮

### 8.3 EmptyState

用途：

- 无活动
- 无消息
- 无好友
- 无通知

字段：

- 标题
- 描述
- 按钮文案

### 8.4 LoadingState

用途：

- 列表加载
- 提交审核中
- AI 生成中

---

## 9. 页面状态规范

每个列表页都要处理 4 种状态：

| 状态 | 展示 |
|---|---|
| loading | 骨架屏或加载中 |
| empty | 空状态组件 |
| error | 错误提示和重试按钮 |
| success | 正常内容 |

每个提交按钮都要处理：

```text
未提交 -> 提交中 -> 成功 / 失败
```

提交中按钮禁用，避免重复请求。

---

## 10. 和后端联调流程

### 10.1 本地后端地址

```text
Gateway: http://localhost:8080
API Base: http://localhost:8080/api/v1
WebSocket: ws://localhost:8080/ws/im
AI Service: http://localhost:8001
```

小程序开发者工具中需要配置合法域名。开发阶段可以开启：

```text
不校验合法域名、web-view、TLS 版本以及 HTTPS 证书
```

### 10.2 推荐联调顺序

1. 登录注册
2. 首页活动列表
3. 活动详情
4. 报名与取消报名
5. 创建活动和提交审核
6. 我的活动和通知
7. 小队列表和小队详情
8. 好友关系
9. IM 会话列表
10. WebSocket 聊天
11. 签到、评价、总结
12. 商家申请

---

## 11. 开发里程碑

### 第一阶段：小程序骨架

目标：

- 创建 `frontend/onlyfriends-miniprogram` 工程。
- 配置 `app.json`、TabBar、全局样式。
- 完成登录拦截和请求封装。
- 首页（活动）、发布、社群、消息、我的 5 个 Tab 可以切换。

验收：

- 微信开发者工具可以启动。
- TabBar 跳转正常。
- 未登录访问受限页面会跳到登录页。

### 第二阶段：活动核心闭环

目标：

- 首页活动列表。
- 活动详情。
- 活动报名、取消报名。
- 创建活动、保存草稿、提交审核。
- AI 策划入口。

验收：

- 用户可以完成“登录 -> 浏览活动 -> 报名活动 -> 查看我的报名”。
- 用户可以完成“创建活动 -> 提交审核”。

### 第三阶段：社交和小队

目标：

- 好友列表、关注、好友申请。
- 小队列表、小队详情、加入小队。
- 小队成员、公告、相册、文件、投票。

验收：

- 用户可以加入小队。
- 用户可以处理好友申请。
- 小队详情功能区完整展示。

### 第四阶段：消息和 WebSocket

目标：

- 会话列表。
- 聊天页。
- WebSocket 连接、订阅、发送、接收。
- 未读数更新。

验收：

- 两个用户可以私聊。
- 小队成员可以群聊。

### 第五阶段：完善体验

目标：

- 签到二维码。
- 活动评价和总结。
- 通知中心。
- 商家申请。
- 全局错误、空状态、加载状态。

验收：

- 主要业务流程完整。
- 页面无明显死链。
- 按钮文案清楚。
- 后端接口联调通过。

---

## 12. 近期建议先做的文件

下一步开发时，建议优先创建这些文件：

```text
frontend/onlyfriends-miniprogram/app.js
frontend/onlyfriends-miniprogram/app.json
frontend/onlyfriends-miniprogram/app.wxss
frontend/onlyfriends-miniprogram/utils/request.js
frontend/onlyfriends-miniprogram/utils/auth.js
frontend/onlyfriends-miniprogram/utils/route.js
frontend/onlyfriends-miniprogram/services/auth.js
frontend/onlyfriends-miniprogram/services/activity.js
frontend/onlyfriends-miniprogram/pages/auth/login/
frontend/onlyfriends-miniprogram/pages/index/
frontend/onlyfriends-miniprogram/pages/activity/detail/
frontend/onlyfriends-miniprogram/pages/activity/create/
frontend/onlyfriends-miniprogram/pages/profile/index/
```

第一批页面不要一次性追求全部功能，应先跑通：

```text
登录 -> 首页活动列表 -> 活动详情 -> 报名 -> 我的报名
```

这是整个平台的最小可用闭环。

---

## 13. 关键设计原则

1. 首页只做发现，不塞太多个人功能。
2. 发起活动必须清晰分步，不让用户一次面对过多字段。
3. 活动详情底部按钮必须随状态变化，不能所有按钮都同时出现。
4. 小队详情用 Tabs 管理复杂信息，避免页面过长。
5. 消息页只展示会话，具体聊天进入二级页面。
6. 所有后端接口统一从 service 层调用。
7. 所有需要登录的动作统一走登录拦截。
8. 所有列表都必须有加载、空、错误、成功四种状态。
9. 所有提交动作都必须防止重复点击。
10. 后续若要打包上线，再统一处理 HTTPS 域名、图片 CDN、地图 Key 和隐私协议。
