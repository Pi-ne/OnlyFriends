# OnlyFriends（趣聚平台）验收标准

> **文档版本**：AC-1.0  
> **状态**：已定稿

---

## 1. 文档说明

| 项 | 说明 |
|---|---|
| 编写原则 | SMART（具体、可衡量、可达成、相关、有时限） |
| 场景格式 | **Given**（前置条件）→ **When**（操作）→ **Then**（预期结果） |
| 关联标注 | 每条 AC 标注对应用户故事编号（US-xxx） |

---

## 2. 全局 Definition of Done（DoD）

以下条件全部满足，方可将用户故事标记为「已完成」：

- [ ] 功能代码已合并至主分支，通过 Code Review
- [ ] 对应 REST/WebSocket 接口符合 `/api/v1` 统一响应格式（`code` / `message` / `data` / `timestamp`）
- [ ] 单元测试覆盖率：核心业务逻辑 ≥ 60%
- [ ] 关键路径具备集成测试或端到端测试用例
- [ ] API 文档（OpenAPI/Swagger）已更新
- [ ] 无 P0/P1 级未关闭缺陷

---

## 3. 功能验收标准

### 3.1 AC-E01：身份认证与用户资料

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-U-001 | US-U-001 | **Given** 访客填写合法邮箱（未注册）、密码（≥8 位含字母数字）、唯一昵称，**When** 调用 `POST /auth/register`，**Then** 返回 `code=200`，数据库写入 `status=0`（未激活），并触发激活邮件。 |
| AC-U-002 | US-U-001 | **Given** 邮箱已注册或昵称已存在，**When** 重复注册，**Then** 返回 `code=400` 及明确错误信息，不创建重复记录。 |
| AC-U-003 | US-U-002 | **Given** 有效 `activate_token`，**When** 调用 `GET /auth/activate?token=xxx`，**Then** 用户 `status` 变为 1，`activate_token` 清空，同一 token 不可重复使用。 |
| AC-U-004 | US-U-003 | **Given** 已激活用户，**When** 正确凭据调用 `POST /auth/login`，**Then** 返回 Access Token（有效期 2 小时）与 Refresh Token（有效期 7 天），密码以 BCrypt 校验。 |
| AC-U-005 | US-U-004 | **Given** Access Token 过期且 Refresh Token 有效，**When** 调用 `POST /auth/refresh`，**Then** 返回新 Access Token；Refresh Token 无效时返回 `401`。 |
| AC-U-006 | US-U-005 | **Given** 已登录用户，**When** 调用 `PUT /users/me/profile` 更新资料，**Then** 修改即时生效；昵称冲突时返回 `code=400`。 |
| AC-U-007 | US-U-005 | **Given** 已登录用户，**When** 调用 `POST /users/me/avatar` 上传 jpg/png/webp（≤10MB），**Then** 返回头像 URL 并更新用户记录。 |
| AC-U-008 | US-U-007 | **Given** 已登录个人用户，**When** 上传证照并提交 `POST /merchant/apply`，**Then** 申请记录创建，证照存储至 MinIO/OSS，状态为待审核。 |
| AC-U-009 | US-U-008 | **Given** 已提交商家申请的用户，**When** 调用 `GET /merchant/apply/status`，**Then** 返回当前审核状态（待审核/通过/驳回）及驳回原因（如有）。 |

---

### 3.2 AC-E02：活动全生命周期

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-A-001 | US-A-001 | **Given** 已登录用户，**When** 调用 `POST /activities`（`isDraft=true`），**Then** 活动状态为「草稿」，仅创建者可 `PUT /activities/{id}` 修改。 |
| AC-A-002 | US-A-002 | **Given** 草稿活动必填字段完整，**When** 调用 `POST /activities/{id}/submit`，**Then** 状态变为「审核中」，触发 AI 内容审核流程。 |
| AC-A-003 | US-AI-001 | **Given** 活动标题/简介命中关键词黑名单，**When** 提交审核，**Then** 直接驳回并返回违规原因，不进入 LLM 审核层。 |
| AC-A-004 | US-AI-001 | **Given** 内容未命中黑名单且 LLM 返回 `result=pass`（confidence≥0.7）且人数上限≤50，**When** 审核完成，**Then** 活动自动发布，写入 `activity_review_record`，通知发起人。 |
| AC-A-005 | US-AI-001 | **Given** LLM 返回 `result=risk` 或 confidence<0.7 或人数上限>50，**When** 审核完成，**Then** 转入人工审核队列，管理员可见 AI 审核结论。 |
| AC-A-006 | US-AI-001 | **Given** LLM 返回 `result=reject`（confidence≥0.9），**When** 审核完成，**Then** 活动驳回，通知发起人并附原因。 |
| AC-A-007 | US-A-003 | **Given** 活动审核完成（通过/驳回/要求修改），**When** 结果产生，**Then** 写入 `notification` 表，发起人可在通知列表查看。 |
| AC-A-008 | US-A-006 | **Given** 访客或未登录用户，**When** 调用 `GET /activities?tab=recommend`，**Then** 返回已发布活动分页列表（封面、标题、时间、地点、报名数等精简字段）。 |
| AC-A-009 | US-A-007 | **Given** 已登录用户授权定位，**When** 调用 `GET /activities/nearby?lat&lng&radius`，**Then** 返回指定半径内活动，按距离排序。 |
| AC-A-010 | US-A-008 | **Given** 已登录用户，**When** 调用带筛选参数的 `GET /activities`，**Then** 按标签、日期等条件过滤并分页返回。 |
| AC-A-011 | US-A-009 | **Given** 任意用户，**When** 调用 `GET /activities/{id}`，**Then** 返回活动完整信息（含发起人、状态、名额、地点坐标等）。 |
| AC-A-012 | US-A-010 | **Given** 活动处于报名中、名额未满、用户信誉分≥阈值且未封禁，**When** 调用 `POST /activities/{id}/register`，**Then** 报名成功，名额减 1，写入 `activity_registration`。 |
| AC-A-013 | US-A-010 | **Given** 用户信誉分不足或已封禁或名额已满，**When** 尝试报名，**Then** 返回 `400`/`403` 及具体原因。 |
| AC-A-014 | US-A-011~012 | **Given** 名额已满，**When** 用户调用 `POST /activities/{id}/waitlist` 加入等待队列，**Then** 写入 `activity_waitlist` 并显示当前等待人数。 |
| AC-A-015 | US-A-011~012 | **Given** 等待队列中有人且名额释放，**When** 系统通知队列首位，**Then** 用户须在 30 分钟内确认；确认则报名成功，超时则顺延下一位。 |
| AC-A-016 | US-A-013 | **Given** 已报名用户且在报名截止前，**When** 调用 `DELETE /activities/{id}/register`，**Then** 取消报名，名额释放，触发等待队列递补逻辑。 |
| AC-A-017 | US-A-014 | **Given** 活动发起人，**When** 调用 `GET /activities/{id}/registrations`，**Then** 返回报名用户列表（仅创建者可访问）。 |
| AC-A-018 | US-A-015~016 | **Given** 发起人调用 `GET /activities/{id}/checkin/qrcode`，**When** 参与者扫码并调用 `POST /activities/{id}/checkin`，**Then** 验证 HMAC-SHA256 签名与时间戳有效性。 |
| AC-A-019 | US-A-016 | **Given** 活动开启位置校验，**When** 参与者签到位置距活动地点超过阈值（默认 500m），**Then** 拒绝签到并返回距离超限提示。 |
| AC-A-020 | US-A-005 | **Given** 已登录发起人，**When** 调用 `POST /activities/{id}/clone`，**Then** 复制活动信息为新草稿，创建者为当前用户。 |
| AC-A-021 | US-A-017 | **Given** 活动已结束且用户为发起人，**When** 调用 `POST /activities/{id}/summary` 发布图文总结，**Then** 总结写入 `activity_summary`，图片经 AI 分类辅助整理。 |
| AC-A-022 | US-A-018 | **Given** 活动已结束且用户为参与者，**When** 调用 `POST /activities/{id}/comments`，**Then** 评价写入 `activity_comment`，可通过 `GET` 公开查看。 |
| AC-A-023 | US-A-001 | **Given** 活动到达各时间节点，**When** 定时任务每分钟扫描，**Then** 状态按状态机自动流转：已发布→报名中→报名截止→进行中→已结束。 |

---

### 3.3 AC-E03：社群与小队

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-S-001 | US-S-001 | **Given** 已登录用户 A，**When** 调用 `POST /follows/{userId}` 关注 B，**Then** A 的关注列表含 B；调用 `DELETE` 可取消关注。 |
| AC-S-002 | US-S-001 | **Given** A 关注 B 且 B 关注 A，**When** 双方互关完成，**Then** 自动建立好友关系。 |
| AC-S-003 | US-S-002 | **Given** 非好友用户，**When** A 发起 `POST /friends/apply` 且 B 通过 `PUT /friends/applies/{id}` 同意，**Then** 双方出现在 `GET /friends` 列表。 |
| AC-S-004 | US-S-003 | **Given** 好友关系，**When** 调用 `PUT /friends/{id}/remark` 设置备注，**Then** 备注即时生效；`DELETE /friends/{id}` 可删除好友。 |
| AC-S-005 | US-S-004 | **Given** 已登录用户，**When** 调用 `POST /teams` 创建小队，**Then** 创建者成为队长，自动生成对应群聊 Channel。 |
| AC-S-006 | US-S-005 | **Given** 已登录用户，**When** 调用 `GET /teams` 搜索小队，**Then** 返回匹配的小队列表（支持关键词/标签）。 |
| AC-S-007 | US-S-006~007 | **Given** 公开加入模式的小队，**When** 用户调用 `POST /teams/{id}/join`，**Then** 直接成为成员；审核模式下需等待队长/管理员审批。 |
| AC-S-008 | US-S-007 | **Given** 队长或管理员，**When** 调用 `PUT /teams/{id}/applies/{applyId}` 处理申请，**Then** 同意则用户加入，拒绝则通知申请人。 |
| AC-S-009 | US-S-008 | **Given** 队长，**When** 调用 `PUT /teams/{id}/members/{userId}/role` 修改角色，**Then** 仅队长可操作；`DELETE /teams/{id}` 解散小队后成员全部退出。 |
| AC-S-010 | US-S-009 | **Given** 小队成员，**When** 上传相册/文件、发起/参与投票，**Then** 数据正确写入，积分按规则变更并反映在 `GET /teams/{id}/scores`。 |
| AC-S-011 | US-S-010 | **Given** 成员参与队内活动签到，**When** 签到成功，**Then** 积分 +10；发动态 +2；动态被精选 +5（按积分规则表）。 |

---

### 3.4 AC-E04：即时通讯

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-I-001 | US-I-001 | **Given** 互为好友的在线用户 A、B，**When** A 通过 WebSocket 向 B 发送私聊，**Then** B 在 3 秒内收到推送，消息持久化至 `im_message`。 |
| AC-I-002 | US-I-001 | **Given** 非好友用户，**When** 尝试发起私聊，**Then** IM 服务通过 OpenFeign 校验好友关系失败，拒绝发送。 |
| AC-I-003 | US-I-002 | **Given** 小队成员订阅 `/topic/team/{teamId}`，**When** 发送群聊消息，**Then** 在线成员实时收到，消息持久化至 `im_group_message`。 |
| AC-I-004 | US-I-003 | **Given** 已登录用户，**When** 调用 `GET /im/conversations`，**Then** 返回会话列表，含最新消息摘要与未读数。 |
| AC-I-005 | US-I-004 | **Given** 接收方离线时收到消息，**When** 接收方上线并调用 REST 接口，**Then** 可拉取离线期间的历史与未读消息。 |
| AC-I-006 | US-I-005 | **Given** 消息发送未满 2 分钟，**When** 发送者调用撤回，**Then** 双方客户端显示「已撤回」，数据库标记撤回状态。 |
| AC-I-007 | US-I-005 | **Given** 消息发送已超过 2 分钟，**When** 尝试撤回，**Then** 返回错误，消息不可撤回。 |
| AC-I-008 | US-I-006 | **Given** 已登录用户，**When** 调用 `POST /im/messages/{msgId}/forward`，**Then** 消息转发至目标会话。 |
| AC-I-009 | US-I-001 | **Given** 用户建立 WebSocket 连接，**When** 每 30 秒发送心跳，**Then** Redis 中在线状态 TTL 续期；断线后支持客户端重连。 |

---

### 3.5 AC-E05：AI 能力

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-AI-001 | US-AI-001 | **Given** 活动服务提交审核内容，**When** AI 服务处理，**Then** 按三层架构（规则引擎→LLM→人工）输出结构化审核结果，含 `result`、`confidence`、`reason`。 |
| AC-AI-002 | US-AI-001 | **Given** LLM API 调用超时，**When** 超过配置超时时间，**Then** 自动转入人工审核队列，不阻塞用户提交。 |
| AC-AI-003 | US-AI-002 | **Given** 已登录发起人输入活动主题，**When** 调用 `POST /ai/plan-activity`，**Then** 返回结构化策划草稿，支持 SSE 流式输出。 |
| AC-AI-004 | US-AI-003 | **Given** 活动总结含多张图片，**When** 调用图片分类接口，**Then** 返回每张图片的分类标签（如人物/风景/美食等）。 |
| AC-AI-005 | US-AI-001 | **Given** AI 服务仅监听内网 :8001，**When** 外部直接访问 AI 接口，**Then** 请求被拒绝，仅 Activity Service 可通过内部网络调用。 |

---

### 3.6 AC-E06：后台管理

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-ADM-001 | US-ADM-001 | **Given** 管理员凭据，**When** 调用 `POST /admin/auth/login`，**Then** 返回带 `ROLE_ADMIN` 的 JWT；普通用户 Token 访问 `/api/v1/admin/**` 返回 `403`。 |
| AC-ADM-002 | US-ADM-002 | **Given** 审核中活动，**When** 管理员执行通过/驳回/要求修改，**Then** 活动状态正确变更，通知发起人，操作写入 `admin_operation_log`。 |
| AC-ADM-003 | US-ADM-003 | **Given** 正常用户，**When** 管理员调用 `POST /admin/users/{id}/ban`（含原因+期限），**Then** `user.status=2`；封禁期内无法登录和报名。 |
| AC-ADM-004 | US-ADM-003 | **Given** 已封禁用户，**When** 管理员调用 `POST /admin/users/{id}/unban`，**Then** 用户恢复正常状态。 |
| AC-ADM-005 | US-ADM-004 | **Given** 待审核商家申请，**When** 管理员通过 `PUT /admin/merchant-applies/{id}` 审核，**Then** 通过后用户 `user_type` 变为商家，驳回则附原因通知用户。 |
| AC-ADM-006 | US-ADM-005 | **Given** 已发布活动，**When** 管理员调用 `POST /admin/activities/{id}/offline`，**Then** 状态变为「已下架」，前台不可报名；`restore` 可恢复。 |
| AC-ADM-007 | US-ADM-006 | **Given** 正常小队，**When** 管理员调用 `POST /admin/teams/{id}/disable`，**Then** 停止新增成员和内容，原有成员可查看历史。 |

---

### 3.7 AC-E07：系统通知

| AC 编号 | 关联故事 | 验收标准 |
|---------|----------|----------|
| AC-N-001 | US-N-001 | **Given** 系统产生通知事件（审核结果/等待队列递补），**When** 写入 `notification` 表，**Then** 用户可通过 `GET /notifications` 分页查看。 |
| AC-N-002 | US-N-001 | **Given** 用户有未读通知，**When** 调用 `PUT /notifications/{id}/read`，**Then** 该通知标记为已读，未读数相应减少。 |

---

## 4. 非功能性验收标准（NFR）

| NFR 编号 | 类别 | 验收标准 | 测量方法 |
|----------|------|----------|----------|
| NFR-P-001 | 性能 | 小程序首页冷启动至首屏可交互 ≤ 5 秒（4G 网络、中端机） | 真机计时 + 微信性能面板 |
| NFR-P-002 | 性能 | 平台自有 REST 接口 P95 响应时间 ≤ 2000ms（不含 AI/地图/邮件等第三方） | APM 监控 / 压测报告 |
| NFR-P-003 | 性能 | 活动列表接口返回精简字段，单页 20 条 P95 ≤ 1500ms（缓存命中场景） | JMeter / k6 压测 |
| NFR-P-004 | 性能 | 小程序主包体积 < 2MB；封面图 WebP 格式 ≤ 200KB | 微信开发者工具体积分析 |
| NFR-S-001 | 安全 | 密码 BCrypt（cost=12）存储，禁止明文；全站 HTTPS 传输 | 代码审查 + 安全扫描 |
| NFR-S-002 | 安全 | JWT 在网关层统一校验；Access Token 2h / Refresh Token 7d | 接口测试 |
| NFR-S-003 | 安全 | 文件上传限制类型（图片 jpg/png/webp，文档 pdf/doc/docx/zip）与大小（图片≤10MB，文档≤50MB） | 渗透测试用例 |
| NFR-S-004 | 安全 | 网关层接口限流，超限返回 `code=429` | 限流压测 |
| NFR-S-005 | 安全 | 活动签到二维码 HMAC-SHA256 签名验证，防止截图复用 | 签到接口测试 |
| NFR-A-001 | 可用性 | 核心服务（Gateway/User/Activity）单实例故障时，Docker 环境 5 分钟内可重启恢复 | 故障演练 |
| NFR-A-002 | 可用性 | WebSocket 心跳间隔 30 秒，断线后客户端自动重连（最多 3 次） | 长连接稳定性测试 |
| NFR-C-001 | 兼容性 | 微信小程序基础库 ≥ 2.25.0 | 兼容性矩阵 |
| NFR-C-002 | 兼容性 | 管理后台支持 Chrome / Edge 最新两个大版本 | 浏览器测试 |
| NFR-M-001 | 可维护性 | 各服务提供 OpenAPI 文档；OpenFeign 契约与实现一致 | 文档审查 |
| NFR-D-001 | 数据 | 微服务逻辑分库：禁止跨服务直接查询他库表，必须通过 Feign 接口 | 架构审查 |
| NFR-D-002 | 数据 | Redis 缓存：活动列表 TTL 5 分钟，用户信息 TTL 30 分钟 | 缓存命中率监控 |

---

## 5. 版本级验收门禁（Release Gate）

| 门禁 | 适用版本 | 通过条件 |
|------|----------|----------|
| Alpha 门禁 | v0.1 | 全部 P0 用户故事对应 AC 通过；主流程可端到端演示（注册→建活动→审核→浏览→报名） |
| Beta 门禁 | v0.5 | P0+P1 故事 AC 通过率 ≥ 95%；NFR-P-001、NFR-P-002 达标；无阻塞性 P0/P1 缺陷 |
| GA 门禁 | v1.0 | 全部 P1 故事 AC 通过；NFR 全项达标；生产环境部署验证通过；运维手册齐备 |
| 增强门禁 | v1.1 | P2 故事 AC 通过；用户反馈 Top 10 问题关闭率 ≥ 80% |

---

## 6. 测试类型与 AC 映射

| 测试类型 | 覆盖范围 | 责任方 |
|----------|----------|--------|
| 单元测试 | 业务逻辑层、工具类、状态机 | 各服务开发 |
| 集成测试 | OpenFeign 调用链、数据库读写、Redis 缓存 | 各服务开发 |
| API 测试 | 全部 REST 接口正向/异常路径 | QA / 开发 |
| E2E 测试 | 主流程用户旅程（小程序 + 管理后台） | QA |
| 性能测试 | NFR-P 系列指标 | 基础设施组 |
| 安全测试 | NFR-S 系列指标 | 基础设施组 |
