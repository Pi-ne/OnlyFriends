const state = {
  baseUrl: localStorage.getItem("of.baseUrl") || "http://localhost:8080/api/v1",
  adminToken: localStorage.getItem("of.adminToken") || "",
  adminInfo: JSON.parse(localStorage.getItem("of.adminInfo") || "null"),
  userToken: localStorage.getItem("of.userToken") || "",
  userId: localStorage.getItem("of.userId") || "",
  userTokenB: localStorage.getItem("of.userTokenB") || "",
  userIdB: localStorage.getItem("of.userIdB") || "",
  teamTest: {
    tokenA: localStorage.getItem("of.team.tokenA") || "",
    userIdA: localStorage.getItem("of.team.userIdA") || "",
    nicknameA: localStorage.getItem("of.team.nicknameA") || "",
    tokenB: localStorage.getItem("of.team.tokenB") || "",
    userIdB: localStorage.getItem("of.team.userIdB") || "",
    nicknameB: localStorage.getItem("of.team.nicknameB") || "",
    activeSlot: localStorage.getItem("of.team.activeSlot") || "A",
    selectedTeamId: "",
    selectedTeamName: "",
    activeChatTeamId: "",
    activeChatTeamName: ""
  },
  chat: {
    convId: "",
    peerUserId: "",
    peerNickname: "",
    lastMsgId: "",
    convType: 1,
    teamId: ""
  }
};

const els = {};

function $(id) {
  return document.getElementById(id);
}

function initElements() {
  [
    "baseUrlInput", "adminUsername", "adminPassword", "adminLoginBtn", "adminLogoutBtn",
    "loginState", "adminName", "userTokenState", "lastAction", "dashboardCards",
    "loadUsersBtn", "userKeyword", "userStatus", "usersBody",
    "loadActivitiesBtn", "activityKeyword", "activityStatus", "activitiesBody",
    "loadTeamsBtn", "teamKeyword", "teamStatus", "teamsBody",
    "teamLoginAForm", "teamLoginBForm", "teamEmailA", "teamEmailB", "teamUserAInfo", "teamUserBInfo",
    "teamTestActiveUser", "teamTestUserInfo", "createTeamForm", "teamBrowseKeyword", "loadTeamBrowseBtn",
    "teamSelectedInfo", "teamBrowseList", "joinSelectedTeamBtn", "refreshJoinedTeamsBtn", "joinedTeamChatList",
    "teamGroupChatTitle", "teamGroupChatMeta", "loadTeamGroupMessagesBtn", "teamChatMessages",
    "sendTeamGroupMessageForm", "teamChatInput", "createUserForm", "loginUserForm",
    "createActivityForm", "activityCreatorToken", "normalEmail", "seedEmail",
    "seedNickname", "apiMethod", "apiPath", "apiBody", "apiUseAdminToken",
    "apiSendBtn", "clearLogBtn", "logOutput", "pageTitle",
    "socialActiveUser", "socialLoginAForm", "socialLoginBForm", "socialEmailA",
    "socialEmailB", "socialUserAInfo", "socialUserBInfo", "friendApplyForm",
    "friendTargetUserId", "friendApplyType", "loadFriendAppliesBtn", "friendAppliesBody",
    "loadFriendsBtn", "friendsBody", "loadConversationsBtn", "conversationList",
    "chatTitle", "chatMeta", "loadMessagesBtn", "chatMessages", "sendMessageForm",
    "chatReceiverId", "chatConvId", "chatInput", "markReadBtn"
  ].forEach((id) => {
    els[id] = $(id);
  });
}

function setLastAction(text) {
  els.lastAction.textContent = text;
}

function log(title, data) {
  const time = new Date().toLocaleTimeString();
  const text = typeof data === "string" ? data : JSON.stringify(data, null, 2);
  els.logOutput.textContent = `[${time}] ${title}\n${text}\n\n${els.logOutput.textContent}`;
  setLastAction(title);
}

function getBaseUrl() {
  state.baseUrl = els.baseUrlInput.value.trim().replace(/\/$/, "");
  localStorage.setItem("of.baseUrl", state.baseUrl);
  return state.baseUrl;
}

function buildHeaders(token, body) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json; charset=utf-8";
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

async function request(path, options = {}) {
  const url = path.startsWith("http") ? path : `${getBaseUrl()}${path}`;
  const body = options.body === undefined ? undefined : JSON.stringify(options.body);
  const response = await fetch(url, {
    method: options.method || "GET",
    headers: buildHeaders(options.token, body),
    body
  });
  const text = await response.text();
  let payload = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch (error) {
    payload = text;
  }

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}: ${text}`);
  }
  if (payload && payload.code && payload.code !== 200) {
    throw new Error(`${payload.code}: ${payload.message || "请求失败"}`);
  }
  return payload ? payload.data : null;
}

function pageData(data) {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  return Array.isArray(data.list) ? data.list : [];
}

function valueOf(item, keys, fallback = "") {
  for (const key of keys) {
    if (item && item[key] !== undefined && item[key] !== null) return item[key];
  }
  return fallback;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function splitTags(value) {
  return String(value || "")
    .split(/[,\s，、]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

function updateAuthUi() {
  els.baseUrlInput.value = state.baseUrl;
  els.adminName.textContent = state.adminInfo ? `${state.adminInfo.nickname || state.adminInfo.username}` : "未登录";
  els.loginState.textContent = state.adminToken ? "管理员已登录" : "未登录";
  els.userTokenState.textContent = state.userToken ? "已保存" : "未准备";
  els.activityCreatorToken.value = state.userToken;
  updateSocialUserInfo();
  updateTeamTestUi();
}

function testUserSlot() {
  const stored = localStorage.getItem("of.testUserSlot") || "A";
  if (els.socialActiveUser) els.socialActiveUser.value = stored;
  return stored;
}

function setTestUserSlot(slot) {
  localStorage.setItem("of.testUserSlot", slot);
  if (els.socialActiveUser) els.socialActiveUser.value = slot;
}

function socialActiveKey() {
  return testUserSlot();
}

function inactiveTestUserSlot() {
  return socialActiveKey() === "A" ? "B" : "A";
}

function getSocialToken(key = socialActiveKey()) {
  return key === "B" ? state.userTokenB : state.userToken;
}

function getSocialUserId(key = socialActiveKey()) {
  return key === "B" ? state.userIdB : state.userId;
}

function getTeamTestSlot() {
  return state.teamTest.activeSlot || "A";
}

function setTeamTestSlot(slot) {
  state.teamTest.activeSlot = slot;
  localStorage.setItem("of.team.activeSlot", slot);
  if (els.teamTestActiveUser) els.teamTestActiveUser.value = slot;
}

function getTeamTestToken(slot = getTeamTestSlot()) {
  return slot === "B" ? state.teamTest.tokenB : state.teamTest.tokenA;
}

function getTeamTestUserId(slot = getTeamTestSlot()) {
  return slot === "B" ? state.teamTest.userIdB : state.teamTest.userIdA;
}

function getTeamTestNickname(slot = getTeamTestSlot()) {
  return slot === "B" ? state.teamTest.nicknameB : state.teamTest.nicknameA;
}

function persistTeamTestLogin(slot, result) {
  const userInfo = result.userInfo || {};
  const userId = userInfo.userId ? String(userInfo.userId) : "";
  const nickname = userInfo.nickname || "";
  const token = result.accessToken || "";
  if (slot === "B") {
    state.teamTest.tokenB = token;
    state.teamTest.userIdB = userId;
    state.teamTest.nicknameB = nickname;
    localStorage.setItem("of.team.tokenB", token);
    localStorage.setItem("of.team.userIdB", userId);
    localStorage.setItem("of.team.nicknameB", nickname);
  } else {
    state.teamTest.tokenA = token;
    state.teamTest.userIdA = userId;
    state.teamTest.nicknameA = nickname;
    localStorage.setItem("of.team.tokenA", token);
    localStorage.setItem("of.team.userIdA", userId);
    localStorage.setItem("of.team.nicknameA", nickname);
  }
  setTeamTestSlot(slot);
}

function updateTeamTestUi() {
  if (!els.teamUserAInfo) return;
  els.teamUserAInfo.textContent = state.teamTest.userIdA
    ? `已登录 · ${state.teamTest.nicknameA || "用户A"} · id=${state.teamTest.userIdA}`
    : "未登录";
  els.teamUserBInfo.textContent = state.teamTest.userIdB
    ? `已登录 · ${state.teamTest.nicknameB || "用户B"} · id=${state.teamTest.userIdB}`
    : "未登录";
  if (els.teamTestActiveUser) els.teamTestActiveUser.value = getTeamTestSlot();
  const slot = getTeamTestSlot();
  const userId = getTeamTestUserId(slot);
  const nickname = getTeamTestNickname(slot);
  if (els.teamTestUserInfo) {
    els.teamTestUserInfo.textContent = userId
      ? `当前以用户 ${slot}（${nickname || "未命名"}，id=${userId}）操作`
      : `当前操作身份：用户 ${slot}（未登录）`;
  }
  if (els.teamSelectedInfo && state.teamTest.selectedTeamId) {
    els.teamSelectedInfo.textContent = `已选小队：${state.teamTest.selectedTeamName}（id=${state.teamTest.selectedTeamId}）`;
  }
}

function updateSocialUserInfo() {
  if (!els.socialUserAInfo) return;
  els.socialUserAInfo.textContent = state.userId
    ? `已登录 · userId=${state.userId}`
    : "未登录";
  els.socialUserBInfo.textContent = state.userIdB
    ? `已登录 · userId=${state.userIdB}`
    : "未登录";
}

function friendApplyStatusText(status) {
  const map = { 0: "待处理", 1: "已同意", 2: "已拒绝" };
  return map[status] ?? status;
}

function formatChatTime(value) {
  if (!value) return "";
  return String(value).replace("T", " ").slice(0, 16);
}

function setActiveChat(conv) {
  if (!conv) return;
  const convType = Number(conv.convType || 1);
  state.chat.convId = String(conv.convId || "");
  state.chat.convType = convType;
  state.chat.teamId = conv.teamId ? String(conv.teamId) : "";
  state.chat.peerUserId = conv.peerUserId ? String(conv.peerUserId) : "";
  state.chat.peerNickname = conv.peerNickname || conv.title || "";
  state.chat.lastMsgId = conv.lastMsgId ? String(conv.lastMsgId) : "";
  els.chatConvId.value = state.chat.convId;
  els.chatReceiverId.value = state.chat.peerUserId;
  els.chatTitle.textContent = convType === 2
    ? `群聊 · ${conv.title || conv.teamId || "小队"}`
    : `私聊 · ${state.chat.peerNickname || state.chat.peerUserId || "未命名"}`;
  const metaParts = [];
  if (state.chat.convId) metaParts.push(`convId=${state.chat.convId}`);
  if (state.chat.peerUserId) metaParts.push(`peerUserId=${state.chat.peerUserId}`);
  if (state.chat.teamId) metaParts.push(`teamId=${state.chat.teamId}`);
  els.chatMeta.textContent = metaParts.length
    ? metaParts.join(" · ")
    : "选择会话或从好友列表发起聊天";
  document.querySelectorAll(".conv-item").forEach((node) => {
    node.classList.toggle("active", node.dataset.convId === state.chat.convId);
  });
}

function resetChatState(message = "选择会话或从好友列表发起聊天") {
  state.chat = {
    convId: "",
    peerUserId: "",
    peerNickname: "",
    lastMsgId: "",
    convType: 1,
    teamId: ""
  };
  els.chatConvId.value = "";
  els.chatReceiverId.value = "";
  els.chatInput.value = "";
  els.chatTitle.textContent = "私聊";
  els.chatMeta.textContent = message;
  els.chatMessages.innerHTML = `<p class="hint empty-hint">${escapeHtml(message)}</p>`;
  document.querySelectorAll(".conv-item").forEach((node) => node.classList.remove("active"));
}

async function onSocialIdentityChange() {
  setTestUserSlot(els.socialActiveUser.value);
  const otherId = socialActiveKey() === "A" ? state.userIdB : state.userId;
  if (otherId) els.friendTargetUserId.value = otherId;
  resetChatState("已切换身份，请刷新会话或从好友列表选择聊天对象");
  els.conversationList.innerHTML = `<p class="hint empty-hint">正在加载当前用户的会话...</p>`;
  log("已切换操作身份", socialActiveKey());
  await Promise.allSettled([loadFriendApplies(), loadFriends(), loadConversations()]);
}

function openChatWithFriend(userId, nickname) {
  const currentUserId = getSocialUserId();
  if (currentUserId && String(userId) === String(currentUserId)) {
    log("无法与自己聊天", "请切换操作身份后再选择好友");
    return;
  }
  setActiveChat({
    convId: "",
    peerUserId: userId,
    peerNickname: nickname,
    convType: 1
  });
  els.chatMessages.innerHTML = `<p class="hint empty-hint">与 ${escapeHtml(nickname || userId)} 的会话将在首次发消息后创建，或点击「刷新会话」打开已有会话</p>`;
}

function switchView(name) {
  document.querySelectorAll(".view").forEach((view) => {
    view.classList.toggle("active", view.id === `view-${name}`);
  });
  document.querySelectorAll(".nav button").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === name);
  });
  const active = document.getElementById(`view-${name}`);
  els.pageTitle.textContent = active.dataset.title;
  if (name === "teams" && getTeamTestToken()) {
    loadTeamBrowseList().catch(() => {});
    loadJoinedTeamChats().catch(() => {});
  }
}

async function loginAdmin() {
  try {
    const data = await request("/admin/auth/login", {
      method: "POST",
      body: {
        username: els.adminUsername.value.trim(),
        password: els.adminPassword.value
      }
    });
    state.adminToken = data.accessToken;
    state.adminInfo = data;
    localStorage.setItem("of.adminToken", state.adminToken);
    localStorage.setItem("of.adminInfo", JSON.stringify(data));
    updateAuthUi();
    log("管理员登录成功", data);
    refreshDashboard();
  } catch (error) {
    log("管理员登录失败", error.message);
  }
}

function logoutAdmin() {
  state.adminToken = "";
  state.adminInfo = null;
  localStorage.removeItem("of.adminToken");
  localStorage.removeItem("of.adminInfo");
  updateAuthUi();
  log("已清除管理员 Token", "需要管理操作时请重新登录。");
}

async function refreshDashboard() {
  const cards = [
    { title: "用户", action: () => request("/admin/users?page=1&size=1", { token: state.adminToken }) },
    { title: "活动", action: () => request("/admin/activities?page=1&size=1", { token: state.adminToken }) },
    { title: "小队", action: () => request("/admin/teams?page=1&size=1", { token: state.adminToken }) },
    { title: "公开活动", action: () => request("/activities?page=1&size=1") }
  ];

  els.dashboardCards.innerHTML = "";
  for (const card of cards) {
    const node = document.createElement("article");
    node.className = "mini-card";
    try {
      const data = await card.action();
      node.innerHTML = `<span>${card.title}</span><strong>${valueOf(data, ["total"], pageData(data).length)}</strong><small>接口正常</small>`;
    } catch (error) {
      node.innerHTML = `<span>${card.title}</span><strong>失败</strong><small>${escapeHtml(error.message)}</small>`;
    }
    els.dashboardCards.appendChild(node);
  }
  log("刷新概览完成", "概览接口已请求。");
}

async function loadUsers() {
  try {
    const params = new URLSearchParams({ page: "1", size: "20" });
    const keyword = els.userKeyword.value.trim();
    const status = els.userStatus.value;
    if (keyword) params.set("keyword", keyword);
    if (status) params.set("status", status);
    const data = await request(`/admin/users?${params}`, { token: state.adminToken });
    renderUsers(pageData(data));
    log("用户查询成功", data);
  } catch (error) {
    renderEmpty(els.usersBody, 7);
    log("用户查询失败", error.message);
  }
}

function renderUsers(users) {
  if (!users.length) {
    renderEmpty(els.usersBody, 7);
    return;
  }
  els.usersBody.innerHTML = users.map((user) => {
    const id = valueOf(user, ["userId", "id"]);
    return `
      <tr>
        <td>${escapeHtml(id)}</td>
        <td>${escapeHtml(valueOf(user, ["email"]))}</td>
        <td>${escapeHtml(valueOf(user, ["nickname"]))}</td>
        <td>${escapeHtml(valueOf(user, ["userType"]))}</td>
        <td>${escapeHtml(valueOf(user, ["status"]))}</td>
        <td>${escapeHtml(valueOf(user, ["creditScore"]))}</td>
        <td class="actions">
          <button data-action="ban-user" data-id="${escapeHtml(id)}">封禁</button>
          <button data-action="unban-user" data-id="${escapeHtml(id)}">解封</button>
          <button data-action="user-detail" data-id="${escapeHtml(id)}">详情</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function loadActivities() {
  try {
    const params = new URLSearchParams({ page: "1", size: "20" });
    const keyword = els.activityKeyword.value.trim();
    const status = els.activityStatus.value;
    if (keyword) params.set("keyword", keyword);
    if (status) params.set("status", status);
    const data = await request(`/admin/activities?${params}`, { token: state.adminToken });
    renderActivities(pageData(data));
    log("活动查询成功", data);
  } catch (error) {
    renderEmpty(els.activitiesBody, 7);
    log("活动查询失败", error.message);
  }
}

function renderActivities(items) {
  if (!items.length) {
    renderEmpty(els.activitiesBody, 7);
    return;
  }
  els.activitiesBody.innerHTML = items.map((item) => {
    const id = valueOf(item, ["activityId", "id"]);
    return `
      <tr>
        <td>${escapeHtml(id)}</td>
        <td>${escapeHtml(valueOf(item, ["title"]))}</td>
        <td>${escapeHtml(valueOf(item, ["creatorNickname", "creatorId"]))}</td>
        <td>${escapeHtml(valueOf(item, ["startTime"]))}</td>
        <td>${escapeHtml(valueOf(item, ["currentCount"], 0))}/${escapeHtml(valueOf(item, ["maxParticipants"], 0))}</td>
        <td>${escapeHtml(valueOf(item, ["statusText", "status"]))}</td>
        <td class="actions">
          <button data-action="approve-activity" data-id="${escapeHtml(id)}">通过</button>
          <button data-action="reject-activity" data-id="${escapeHtml(id)}">驳回</button>
          <button data-action="modify-activity" data-id="${escapeHtml(id)}">要求修改</button>
          <button data-action="offline-activity" data-id="${escapeHtml(id)}">下架</button>
          <button data-action="restore-activity" data-id="${escapeHtml(id)}">恢复</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function loadTeams() {
  try {
    const params = new URLSearchParams({ page: "1", size: "20" });
    const keyword = els.teamKeyword.value.trim();
    const status = els.teamStatus.value;
    if (keyword) params.set("keyword", keyword);
    if (status) params.set("status", status);
    const data = await request(`/admin/teams?${params}`, { token: state.adminToken });
    renderTeams(pageData(data));
    log("小队查询成功", data);
  } catch (error) {
    renderEmpty(els.teamsBody, 5);
    log("小队查询失败", error.message);
  }
}

function renderTeams(items) {
  if (!items.length) {
    renderEmpty(els.teamsBody, 5);
    return;
  }
  els.teamsBody.innerHTML = items.map((item) => {
    const id = valueOf(item, ["teamId", "id"]);
    return `
      <tr>
        <td>${escapeHtml(id)}</td>
        <td>${escapeHtml(valueOf(item, ["name"]))}</td>
        <td>${escapeHtml(valueOf(item, ["memberCount"], 0))}/${escapeHtml(valueOf(item, ["maxMembers"], 0))}</td>
        <td>${escapeHtml(valueOf(item, ["status"]))}</td>
        <td class="actions">
          <button data-action="disable-team" data-id="${escapeHtml(id)}">停用</button>
          <button data-action="restore-team" data-id="${escapeHtml(id)}">恢复</button>
          <button data-action="team-members" data-id="${escapeHtml(id)}">成员</button>
          <button data-action="team-group-chat" data-id="${escapeHtml(id)}" data-name="${escapeHtml(valueOf(item, ["name"]))}">选中</button>
        </td>
      </tr>
    `;
  }).join("");
}

function renderEmpty(body, colspan) {
  body.innerHTML = `<tr><td colspan="${colspan}" class="empty-cell">暂无数据</td></tr>`;
}

async function handleTableAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  const id = button.dataset.id;
  try {
    if (action === "ban-user") {
      const reason = prompt("封禁原因", "开发测试封禁");
      if (!reason) return;
      const days = Number(prompt("封禁天数", "7") || "7");
      await request(`/admin/users/${id}/ban`, {
        method: "POST",
        token: state.adminToken,
        body: {
          reason,
          banExpireAt: futureIso(days)
        }
      });
      await loadUsers();
    }
    if (action === "unban-user") {
      await request(`/admin/users/${id}/unban`, { method: "POST", token: state.adminToken });
      await loadUsers();
    }
    if (action === "user-detail") {
      const data = await request(`/admin/users/${id}`, { token: state.adminToken });
      log(`用户详情 ${id}`, data);
    }
    if (action === "approve-activity" || action === "reject-activity" || action === "modify-activity") {
      const reviewAction = {
        "approve-activity": 0,
        "reject-activity": 1,
        "modify-activity": 2
      }[action];
      await request(`/admin/activities/${id}/review`, {
        method: "PUT",
        token: state.adminToken,
        body: {
          action: reviewAction,
          comment: action === "approve-activity" ? "开发管理台审核通过" : "开发管理台审核意见"
        }
      });
      await loadActivities();
    }
    if (action === "offline-activity") {
      await request(`/admin/activities/${id}/offline`, {
        method: "POST",
        token: state.adminToken,
        body: { reason: prompt("下架原因", "开发测试下架") || "开发测试下架" }
      });
      await loadActivities();
    }
    if (action === "restore-activity") {
      await request(`/admin/activities/${id}/restore`, { method: "POST", token: state.adminToken });
      await loadActivities();
    }
    if (action === "disable-team") {
      await request(`/admin/teams/${id}/disable`, {
        method: "POST",
        token: state.adminToken,
        body: { reason: prompt("停用原因", "开发测试停用") || "开发测试停用" }
      });
      await loadTeams();
    }
    if (action === "restore-team") {
      await request(`/admin/teams/${id}/restore`, { method: "POST", token: state.adminToken });
      await loadTeams();
    }
    if (action === "team-members") {
      const data = await request(`/admin/teams/${id}/members`, { token: state.adminToken });
      log(`小队成员 ${id}`, data);
    }
    if (action === "team-group-chat") {
      selectTeamForJoin(id, button.dataset.name || `小队 ${id}`, true);
      switchView("teams");
      log("已从管理端选择小队", { teamId: id });
    }
    log(`操作成功: ${action}`, { id });
  } catch (error) {
    log(`操作失败: ${action}`, error.message);
  }
}

function futureIso(days) {
  const date = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 19);
}

async function createUser(event) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  try {
    const result = await request("/auth/register", { method: "POST", body: data });
    log("测试用户注册成功", result);
  } catch (error) {
    log("测试用户注册失败", error.message);
  }
}

async function loginUser(event) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  try {
    const result = await request("/auth/login", { method: "POST", body: data });
    state.userToken = result.accessToken;
    state.userId = result.userInfo ? String(result.userInfo.userId) : "";
    localStorage.setItem("of.userToken", state.userToken);
    localStorage.setItem("of.userId", state.userId);
    updateAuthUi();
    log("普通用户登录成功", result.userInfo || result);
  } catch (error) {
    log("普通用户登录失败", error.message);
  }
}

async function loginSocialUser(event, slot) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  try {
    const result = await request("/auth/login", { method: "POST", body: data });
    const userId = result.userInfo ? String(result.userInfo.userId) : "";
    if (slot === "B") {
      state.userTokenB = result.accessToken;
      state.userIdB = userId;
      localStorage.setItem("of.userTokenB", state.userTokenB);
      localStorage.setItem("of.userIdB", state.userIdB);
    } else {
      state.userToken = result.accessToken;
      state.userId = userId;
      localStorage.setItem("of.userToken", state.userToken);
      localStorage.setItem("of.userId", state.userId);
    }
    updateAuthUi();
    log(`用户 ${slot} 登录成功`, result.userInfo || result);
  } catch (error) {
    log(`用户 ${slot} 登录失败`, error.message);
  }
}

async function applyFriend(event) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  const targetUserId = data.targetUserId.trim();
  if (!targetUserId) {
    log("好友申请失败", "请填写目标用户 ID");
    return;
  }
  try {
    const result = await request(`/friends/${targetUserId}/applies`, {
      method: "POST",
      token: getSocialToken(),
      body: { message: data.message || "开发管理台测试加好友" }
    });
    log("好友申请已发送", result);
    loadFriendApplies();
  } catch (error) {
    log("好友申请失败", error.message);
  }
}

async function loadFriendApplies() {
  const token = getSocialToken();
  if (!token) {
    renderEmpty(els.friendAppliesBody, 6);
    log("好友申请查询失败", "请先登录当前操作身份对应的用户");
    return;
  }
  try {
    const type = els.friendApplyType.value;
    const data = await request(`/friends/applies?type=${encodeURIComponent(type)}`, { token });
    renderFriendApplies(Array.isArray(data) ? data : pageData(data));
    log("好友申请查询成功", data);
  } catch (error) {
    renderEmpty(els.friendAppliesBody, 6);
    log("好友申请查询失败", error.message);
  }
}

function renderFriendApplies(items) {
  if (!items.length) {
    renderEmpty(els.friendAppliesBody, 6);
    return;
  }
  els.friendAppliesBody.innerHTML = items.map((item) => {
    const id = valueOf(item, ["applyId", "id"]);
    const status = valueOf(item, ["status"]);
    const canReview = Number(status) === 0 && els.friendApplyType.value === "received";
    return `
      <tr>
        <td>${escapeHtml(id)}</td>
        <td>${escapeHtml(valueOf(item, ["applicantNickname"]))} (${escapeHtml(valueOf(item, ["applicantId"]))})</td>
        <td>${escapeHtml(valueOf(item, ["targetNickname"]))} (${escapeHtml(valueOf(item, ["targetId"]))})</td>
        <td>${escapeHtml(valueOf(item, ["message"]))}</td>
        <td>${escapeHtml(friendApplyStatusText(status))}</td>
        <td class="actions">
          ${canReview ? `
            <button data-action="approve-friend" data-id="${escapeHtml(id)}">同意</button>
            <button data-action="reject-friend" data-id="${escapeHtml(id)}">拒绝</button>
          ` : `<span class="hint">-</span>`}
        </td>
      </tr>
    `;
  }).join("");
}

async function loadFriends() {
  const token = getSocialToken();
  if (!token) {
    renderEmpty(els.friendsBody, 4);
    log("好友列表查询失败", "请先登录当前操作身份对应的用户");
    return;
  }
  try {
    const data = await request("/friends", { token });
    renderFriends(Array.isArray(data) ? data : pageData(data));
    log("好友列表查询成功", data);
  } catch (error) {
    renderEmpty(els.friendsBody, 4);
    log("好友列表查询失败", error.message);
  }
}

function renderFriends(items) {
  if (!items.length) {
    renderEmpty(els.friendsBody, 4);
    return;
  }
  els.friendsBody.innerHTML = items.map((item) => {
    const id = valueOf(item, ["userId", "id"]);
    const nickname = valueOf(item, ["nickname"]);
    return `
      <tr>
        <td>${escapeHtml(id)}</td>
        <td>${escapeHtml(nickname)}</td>
        <td>${escapeHtml(valueOf(item, ["remark"]))}</td>
        <td class="actions">
          <button data-action="chat-friend" data-id="${escapeHtml(id)}" data-name="${escapeHtml(nickname)}">聊天</button>
          <button data-action="delete-friend" data-id="${escapeHtml(id)}">删除</button>
        </td>
      </tr>
    `;
  }).join("");
}

async function loadConversations() {
  const token = getSocialToken();
  if (!token) {
    els.conversationList.innerHTML = `<p class="hint empty-hint">请先登录用户</p>`;
    log("会话列表查询失败", "请先登录当前操作身份对应的用户");
    return;
  }
  try {
    const data = await request("/im/conversations", { token });
    renderConversations(Array.isArray(data) ? data : pageData(data));
    log("会话列表查询成功", data);
  } catch (error) {
    els.conversationList.innerHTML = `<p class="hint empty-hint">${escapeHtml(error.message)}</p>`;
    log("会话列表查询失败", error.message);
  }
}

function renderConversations(items) {
  if (!items.length) {
    els.conversationList.innerHTML = `<p class="hint empty-hint">暂无会话，成为好友后发送首条消息即可创建</p>`;
    return;
  }
  els.conversationList.innerHTML = items.map((item) => {
    const convId = valueOf(item, ["convId", "id"]);
    const convType = Number(valueOf(item, ["convType"], 1));
    const title = convType === 2
      ? valueOf(item, ["title"], `小队 ${valueOf(item, ["teamId"])}`)
      : valueOf(item, ["peerNickname"], `用户 ${valueOf(item, ["peerUserId"])}`);
    const preview = valueOf(item, ["lastMsgPreview"], "暂无消息");
    const unread = Number(valueOf(item, ["unreadCount"], 0));
    return `
      <button class="conv-item${state.chat.convId === String(convId) ? " active" : ""}" type="button"
        data-action="open-conversation"
        data-conv-id="${escapeHtml(convId)}"
        data-peer-user-id="${escapeHtml(valueOf(item, ["peerUserId"]))}"
        data-peer-nickname="${escapeHtml(title)}"
        data-conv-type="${escapeHtml(convType)}"
        data-team-id="${escapeHtml(valueOf(item, ["teamId"]))}"
        data-last-msg-id="${escapeHtml(valueOf(item, ["lastMsgId"]))}">
        <strong>${escapeHtml(title)}${unread > 0 ? ` (${unread})` : ""}</strong>
        <span>${convType === 2 ? "群聊" : "私聊"} · convId=${escapeHtml(convId)}</span>
        <small>${escapeHtml(preview)}</small>
      </button>
    `;
  }).join("");
}

async function loadMessages() {
  const token = getSocialToken();
  const convId = els.chatConvId.value.trim();
  if (!token) {
    log("消息加载失败", "请先登录当前操作身份对应的用户");
    return;
  }
  if (!convId) {
    els.chatMessages.innerHTML = `<p class="hint empty-hint">请先选择会话，或从好友列表点击「聊天」后发送首条消息</p>`;
    return;
  }
  try {
    const data = await request(`/im/messages/${convId}?page=1&size=50`, { token });
    const messages = pageData(data);
    renderChatMessages(messages);
    if (messages.length) {
      const last = messages[messages.length - 1];
      state.chat.lastMsgId = String(valueOf(last, ["msgId", "id"]));
    }
    log("消息加载成功", data);
  } catch (error) {
    els.chatMessages.innerHTML = `<p class="hint empty-hint">${escapeHtml(error.message)}</p>`;
    log("消息加载失败", error.message);
  }
}

function renderChatMessages(messages, container = els.chatMessages) {
  if (!messages.length) {
    container.innerHTML = `<p class="hint empty-hint">暂无消息</p>`;
    return;
  }
  container.innerHTML = messages.map((item) => {
    const recalled = Number(valueOf(item, ["status"])) === 2;
    const mine = Boolean(item.mine);
    const sender = valueOf(item, ["senderNickname"], valueOf(item, ["senderId"]));
    const content = recalled ? "消息已撤回" : valueOf(item, ["content"]);
    return `
      <article class="chat-bubble ${mine ? "mine" : "theirs"}${recalled ? " recalled" : ""}">
        ${sender && !mine ? `<strong class="chat-sender">${escapeHtml(sender)}</strong>` : ""}
        ${escapeHtml(content)}
        <time>${escapeHtml(formatChatTime(valueOf(item, ["createdAt"])))}</time>
      </article>
    `;
  }).join("");
  container.scrollTop = container.scrollHeight;
}

async function sendPrivateMessage(event) {
  event.preventDefault();
  const token = getSocialToken();
  const content = els.chatInput.value.trim();
  const receiverId = els.chatReceiverId.value.trim() || els.friendTargetUserId.value.trim();
  const currentUserId = getSocialUserId();
  if (!token) {
    log("发送失败", "请先登录当前操作身份对应的用户");
    return;
  }
  if (!receiverId) {
    log("发送失败", "请从好友列表或会话列表选择聊天对象");
    return;
  }
  if (currentUserId && String(receiverId) === String(currentUserId)) {
    log("发送失败", "不能给自己发私聊，请切换操作身份或重新选择聊天对象");
    return;
  }
  if (!content) {
    log("发送失败", "消息内容不能为空");
    return;
  }
  try {
    const result = await request("/im/messages/private", {
      method: "POST",
      token,
      body: {
        receiverId: Number(receiverId),
        msgType: 1,
        content
      }
    });
    if (result.convId) {
      setActiveChat({
        convId: result.convId,
        peerUserId: receiverId,
        peerNickname: state.chat.peerNickname,
        lastMsgId: result.msgId
      });
    }
    els.chatInput.value = "";
    await loadMessages();
    await loadConversations();
    log("私聊发送成功", result);
  } catch (error) {
    log("私聊发送失败", error.message);
  }
}

async function markConversationRead() {
  const token = getSocialToken();
  const convId = els.chatConvId.value.trim();
  const lastReadMsgId = state.chat.lastMsgId;
  if (!token || !convId || !lastReadMsgId) {
    log("标记已读失败", "需要会话 ID 和最新消息 ID");
    return;
  }
  try {
    await request(`/im/conversations/${convId}/read`, {
      method: "POST",
      token,
      body: { lastReadMsgId: Number(lastReadMsgId) }
    });
    await loadConversations();
    log("标记已读成功", { convId, lastReadMsgId });
  } catch (error) {
    log("标记已读失败", error.message);
  }
}

async function handleSocialAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  const id = button.dataset.id;
  const token = getSocialToken();
  try {
    if (action === "approve-friend") {
      await request(`/friends/applies/${id}`, {
        method: "PUT",
        token,
        body: { action: 1, reason: "开发管理台同意" }
      });
      await loadFriendApplies();
      await loadFriends();
    }
    if (action === "reject-friend") {
      await request(`/friends/applies/${id}`, {
        method: "PUT",
        token,
        body: { action: 2, reason: "开发管理台拒绝" }
      });
      await loadFriendApplies();
    }
    if (action === "delete-friend") {
      if (!confirm(`确认删除好友 ${id}？`)) return;
      await request(`/friends/${id}`, { method: "DELETE", token });
      await loadFriends();
      await loadConversations();
    }
    if (action === "chat-friend") {
      openChatWithFriend(id, button.dataset.name || "");
      const convs = await request("/im/conversations", { token });
      const list = Array.isArray(convs) ? convs : pageData(convs);
      const matched = list.find((item) => String(valueOf(item, ["peerUserId"])) === String(id));
      if (matched) {
        setActiveChat({
          convId: valueOf(matched, ["convId"]),
          peerUserId: id,
          peerNickname: button.dataset.name || "",
          lastMsgId: valueOf(matched, ["lastMsgId"]),
          convType: valueOf(matched, ["convType"], 1)
        });
        await loadMessages();
        renderConversations(list);
      }
    }
    if (action === "open-conversation") {
      const convType = Number(button.dataset.convType || 1);
      setActiveChat({
        convId: button.dataset.convId,
        peerUserId: button.dataset.peerUserId,
        peerNickname: button.dataset.peerNickname,
        lastMsgId: button.dataset.lastMsgId,
        convType,
        teamId: button.dataset.teamId,
        title: button.dataset.peerNickname
      });
      await loadMessages();
    }
    log(`操作成功: ${action}`, { id });
  } catch (error) {
    log(`操作失败: ${action}`, error.message);
  }
}

async function loginTeamUser(event, slot) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  try {
    const result = await request("/auth/login", { method: "POST", body: data });
    persistTeamTestLogin(slot, result);
    updateTeamTestUi();
    log(`小队页 · 用户 ${slot} 登录成功`, result.userInfo || result);
    await loadTeamBrowseList().catch(() => {});
    await loadJoinedTeamChats().catch(() => {});
  } catch (error) {
    log(`小队页 · 用户 ${slot} 登录失败`, error.message);
  }
}

async function onTeamTestActiveChange() {
  setTeamTestSlot(els.teamTestActiveUser.value);
  updateTeamTestUi();
  state.teamTest.activeChatTeamId = "";
  state.teamTest.activeChatTeamName = "";
  if (els.teamGroupChatTitle) els.teamGroupChatTitle.textContent = "群聊";
  if (els.teamGroupChatMeta) els.teamGroupChatMeta.textContent = "从「已加入的群聊」中选择一个小队";
  if (els.teamChatMessages) {
    els.teamChatMessages.innerHTML = `<p class="hint empty-hint">已切换身份，请从「已加入的群聊」选择小队</p>`;
  }
  log("小队页 · 已切换操作身份", getTeamTestSlot());
  await loadTeamBrowseList().catch(() => {});
  await loadJoinedTeamChats().catch(() => {});
}

function selectTeamForJoin(teamId, name, joined = false) {
  state.teamTest.selectedTeamId = String(teamId);
  state.teamTest.selectedTeamName = name || `小队 ${teamId}`;
  if (els.teamSelectedInfo) {
    const joinedText = joined ? "（已加入）" : "（未加入）";
    els.teamSelectedInfo.textContent = `已选小队：${state.teamTest.selectedTeamName} · id=${teamId} ${joinedText}`;
  }
  document.querySelectorAll(".team-pick-item").forEach((node) => {
    node.classList.toggle("active", node.dataset.teamId === state.teamTest.selectedTeamId);
  });
}

async function loadTeamBrowseList() {
  const token = getTeamTestToken();
  if (!token) {
    els.teamBrowseList.innerHTML = `<p class="hint empty-hint">请先登录用户 A 或 B</p>`;
    return;
  }
  try {
    const params = new URLSearchParams();
    const keyword = els.teamBrowseKeyword.value.trim();
    if (keyword) params.set("keyword", keyword);
    const query = params.toString();
    const data = await request(`/teams${query ? `?${query}` : ""}`, { token });
    renderTeamBrowseList(Array.isArray(data) ? data : pageData(data));
    log("小队列表加载成功", data);
  } catch (error) {
    els.teamBrowseList.innerHTML = `<p class="hint empty-hint">${escapeHtml(error.message)}</p>`;
    log("小队列表加载失败", error.message);
  }
}

function renderTeamBrowseList(items) {
  if (!items.length) {
    els.teamBrowseList.innerHTML = `<p class="hint empty-hint">暂无小队，可先创建或调整搜索条件</p>`;
    return;
  }
  els.teamBrowseList.innerHTML = items.map((item) => {
    const id = valueOf(item, ["teamId", "id"]);
    const name = valueOf(item, ["name"]);
    const joined = Boolean(item.joined);
    const joinType = Number(valueOf(item, ["joinType"], 0)) === 1 ? "需审核" : "直接加入";
    return `
      <button class="team-pick-item${state.teamTest.selectedTeamId === String(id) ? " active" : ""}" type="button"
        data-action="select-team"
        data-team-id="${escapeHtml(id)}"
        data-team-name="${escapeHtml(name)}"
        data-joined="${joined ? "1" : "0"}">
        <strong>${escapeHtml(name)}</strong>
        <span>id=${escapeHtml(id)} · ${escapeHtml(valueOf(item, ["memberCount"], 0))}/${escapeHtml(valueOf(item, ["maxMembers"], 0))} 人</span>
        <small>${joined ? "已加入" : "未加入"} · ${joinType} · 队长 ${escapeHtml(valueOf(item, ["ownerNickname", "ownerId"]))}</small>
      </button>
    `;
  }).join("");
}

async function joinSelectedTeam() {
  const teamId = state.teamTest.selectedTeamId;
  const token = getTeamTestToken();
  const slot = getTeamTestSlot();
  if (!token) {
    log("加入小队失败", `请先登录用户 ${slot}`);
    return;
  }
  if (!teamId) {
    log("加入小队失败", "请先在「选择小队」列表中点击一个小队");
    return;
  }
  try {
    const result = await request(`/teams/${teamId}/join`, {
      method: "POST",
      token,
      body: { message: "开发管理台加入小队" }
    });
    const applyId = valueOf(result, ["applyId"], 0);
    if (applyId && Number(applyId) > 0) {
      log(`用户 ${slot} 已提交加入申请，需队长审核`, result);
    } else {
      log(`用户 ${slot} 已加入小队`, result);
      await openTeamGroupChat(teamId, state.teamTest.selectedTeamName);
    }
    await loadTeamBrowseList();
    await loadJoinedTeamChats();
  } catch (error) {
    log("加入小队失败", error.message);
  }
}

async function loadJoinedTeamChats() {
  const token = getTeamTestToken();
  if (!token) {
    els.joinedTeamChatList.innerHTML = `<p class="hint empty-hint">请先登录用户 A 或 B</p>`;
    return;
  }
  try {
    const data = await request("/teams?joined=true", { token });
    renderJoinedTeamChats(Array.isArray(data) ? data : pageData(data));
    log("已加入群聊列表加载成功", data);
  } catch (error) {
    els.joinedTeamChatList.innerHTML = `<p class="hint empty-hint">${escapeHtml(error.message)}</p>`;
    log("已加入群聊列表加载失败", error.message);
  }
}

function renderJoinedTeamChats(items) {
  if (!items.length) {
    els.joinedTeamChatList.innerHTML = `<p class="hint empty-hint">当前用户尚未加入任何小队</p>`;
    return;
  }
  els.joinedTeamChatList.innerHTML = items.map((item) => {
    const id = valueOf(item, ["teamId", "id"]);
    const name = valueOf(item, ["name"]);
    const role = Number(valueOf(item, ["myRole"], 0));
    const roleText = role === 2 ? "队长" : role === 1 ? "管理员" : "成员";
    return `
      <button class="team-pick-item joined${state.teamTest.activeChatTeamId === String(id) ? " active" : ""}" type="button"
        data-action="open-team-chat"
        data-team-id="${escapeHtml(id)}"
        data-team-name="${escapeHtml(name)}">
        <strong>${escapeHtml(name)}</strong>
        <span>id=${escapeHtml(id)} · ${roleText}</span>
        <small>${escapeHtml(valueOf(item, ["memberCount"], 0))} 人在线群内 · 点击进入聊天</small>
      </button>
    `;
  }).join("");
}

async function openTeamGroupChat(teamId, name) {
  state.teamTest.activeChatTeamId = String(teamId);
  state.teamTest.activeChatTeamName = name || `小队 ${teamId}`;
  selectTeamForJoin(teamId, state.teamTest.activeChatTeamName, true);
  els.teamGroupChatTitle.textContent = `群聊 · ${state.teamTest.activeChatTeamName}`;
  els.teamGroupChatMeta.textContent = `teamId=${teamId} · 当前用户 ${getTeamTestSlot()}（id=${getTeamTestUserId()}）`;
  document.querySelectorAll(".team-pick-item.joined").forEach((node) => {
    node.classList.toggle("active", node.dataset.teamId === state.teamTest.activeChatTeamId);
  });
  await loadTeamGroupMessages();
}

async function handleTeamUserAction(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  try {
    if (action === "select-team") {
      selectTeamForJoin(button.dataset.teamId, button.dataset.teamName, button.dataset.joined === "1");
      log("已选择小队", { teamId: button.dataset.teamId });
    }
    if (action === "open-team-chat") {
      await openTeamGroupChat(button.dataset.teamId, button.dataset.teamName);
      log("已进入群聊", { teamId: button.dataset.teamId });
    }
  } catch (error) {
    log(`小队操作失败: ${action}`, error.message);
  }
}

async function createTeam(event) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  const token = getTeamTestToken();
  const slot = getTeamTestSlot();
  if (!token) {
    log("兴趣组创建失败", `请先登录用户 ${slot}`);
    return;
  }
  try {
    const result = await request("/teams", {
      method: "POST",
      token,
      body: {
        name: data.name,
        description: data.description,
        tags: splitTags(data.tags),
        joinType: Number(data.joinType),
        maxMembers: Number(data.maxMembers || 30)
      }
    });
    const teamId = valueOf(result, ["teamId", "id"]);
    const teamName = valueOf(result, ["name"], data.name);
    selectTeamForJoin(teamId, teamName, true);
    log("兴趣组创建成功（已自动建立群聊）", result);
    loadTeams();
    await loadTeamBrowseList();
    await loadJoinedTeamChats();
    await openTeamGroupChat(teamId, teamName);
  } catch (error) {
    log("兴趣组创建失败", error.message);
  }
}

async function loadTeamGroupMessages() {
  const teamId = state.teamTest.activeChatTeamId;
  const token = getTeamTestToken();
  if (!teamId) {
    els.teamChatMessages.innerHTML = `<p class="hint empty-hint">请从「已加入的群聊」选择一个小队</p>`;
    return;
  }
  if (!token) {
    log("群消息加载失败", "请先登录当前操作身份");
    return;
  }
  try {
    const data = await request(`/im/groups/${teamId}/messages?page=1&size=50`, { token });
    renderChatMessages(pageData(data), els.teamChatMessages);
    log("群消息加载成功", data);
  } catch (error) {
    els.teamChatMessages.innerHTML = `<p class="hint empty-hint">${escapeHtml(error.message)}</p>`;
    log("群消息加载失败", error.message);
  }
}

async function sendTeamGroupMessage(event) {
  event.preventDefault();
  const teamId = state.teamTest.activeChatTeamId;
  const token = getTeamTestToken();
  const content = els.teamChatInput.value.trim();
  if (!teamId) {
    log("群聊发送失败", "请先从「已加入的群聊」选择一个小队");
    return;
  }
  if (!token) {
    log("群聊发送失败", "请先登录当前操作身份");
    return;
  }
  if (!content) {
    log("群聊发送失败", "消息内容不能为空");
    return;
  }
  try {
    const result = await request("/im/messages/group", {
      method: "POST",
      token,
      body: {
        teamId: Number(teamId),
        msgType: 1,
        content,
        mentionAll: false,
        mentionUserIds: []
      }
    });
    els.teamChatInput.value = "";
    await loadTeamGroupMessages();
    log("群聊发送成功", result);
  } catch (error) {
    log("群聊发送失败", error.message);
  }
}

async function createActivity(event) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  const token = els.activityCreatorToken.value.trim();
  try {
    const result = await request("/activities", {
      method: "POST",
      token,
      body: {
        title: data.title,
        description: data.description,
        tags: splitTags(data.tags),
        coverUrl: "",
        startTime: data.startTime,
        endTime: data.endTime,
        regDeadline: data.regDeadline,
        locationName: data.locationName,
        locationLat: Number(data.locationLat),
        locationLng: Number(data.locationLng),
        locationDetail: "开发管理台创建",
        maxParticipants: Number(data.maxParticipants),
        fee: Number(data.fee || 0),
        locationVerify: 0,
        locationRadius: 200,
        isDraft: false
      }
    });
    log("测试活动创建成功", result);
    loadActivities();
  } catch (error) {
    log("测试活动创建失败", error.message);
  }
}

async function sendApiRequest() {
  const method = els.apiMethod.value;
  const path = els.apiPath.value.trim();
  const bodyText = els.apiBody.value.trim();
  let body;
  if (bodyText) {
    try {
      body = JSON.parse(bodyText);
    } catch (error) {
      log("Body JSON 格式错误", error.message);
      return;
    }
  }
  try {
    const data = await request(path, {
      method,
      body: ["GET", "DELETE"].includes(method) && !bodyText ? undefined : body,
      token: els.apiUseAdminToken.checked ? state.adminToken : state.userToken
    });
    log(`${method} ${path}`, data);
  } catch (error) {
    log(`${method} ${path} 失败`, error.message);
  }
}

function setupDefaults() {
  const stamp = new Date().toISOString().replace(/\D/g, "").slice(0, 14);
  els.seedEmail.value = `dev.${stamp}@example.com`;
  els.seedNickname.value = `dev${stamp.slice(-6)}`;
  els.normalEmail.value = "demo.a@example.com";
}

function setupEvents() {
  document.querySelectorAll(".nav button").forEach((button) => {
    button.addEventListener("click", () => switchView(button.dataset.view));
  });
  els.baseUrlInput.addEventListener("change", getBaseUrl);
  els.adminLoginBtn.addEventListener("click", loginAdmin);
  els.adminLogoutBtn.addEventListener("click", logoutAdmin);
  $("refreshDashboardBtn").addEventListener("click", refreshDashboard);
  els.loadUsersBtn.addEventListener("click", loadUsers);
  els.loadActivitiesBtn.addEventListener("click", loadActivities);
  els.loadTeamsBtn.addEventListener("click", loadTeams);
  els.usersBody.addEventListener("click", handleTableAction);
  els.activitiesBody.addEventListener("click", handleTableAction);
  els.teamsBody.addEventListener("click", handleTableAction);
  els.createUserForm.addEventListener("submit", createUser);
  els.loginUserForm.addEventListener("submit", loginUser);
  els.createTeamForm.addEventListener("submit", createTeam);
  els.teamLoginAForm.addEventListener("submit", (event) => loginTeamUser(event, "A"));
  els.teamLoginBForm.addEventListener("submit", (event) => loginTeamUser(event, "B"));
  els.teamTestActiveUser.addEventListener("change", onTeamTestActiveChange);
  els.loadTeamBrowseBtn.addEventListener("click", loadTeamBrowseList);
  els.teamBrowseKeyword.addEventListener("change", loadTeamBrowseList);
  els.joinSelectedTeamBtn.addEventListener("click", joinSelectedTeam);
  els.refreshJoinedTeamsBtn.addEventListener("click", loadJoinedTeamChats);
  els.teamBrowseList.addEventListener("click", handleTeamUserAction);
  els.joinedTeamChatList.addEventListener("click", handleTeamUserAction);
  els.loadTeamGroupMessagesBtn.addEventListener("click", loadTeamGroupMessages);
  els.sendTeamGroupMessageForm.addEventListener("submit", sendTeamGroupMessage);
  els.createActivityForm.addEventListener("submit", createActivity);
  els.apiSendBtn.addEventListener("click", sendApiRequest);
  els.socialLoginAForm.addEventListener("submit", (event) => loginSocialUser(event, "A"));
  els.socialLoginBForm.addEventListener("submit", (event) => loginSocialUser(event, "B"));
  els.friendApplyForm.addEventListener("submit", applyFriend);
  els.loadFriendAppliesBtn.addEventListener("click", loadFriendApplies);
  els.friendApplyType.addEventListener("change", loadFriendApplies);
  els.loadFriendsBtn.addEventListener("click", loadFriends);
  els.loadConversationsBtn.addEventListener("click", loadConversations);
  els.loadMessagesBtn.addEventListener("click", loadMessages);
  els.sendMessageForm.addEventListener("submit", sendPrivateMessage);
  els.markReadBtn.addEventListener("click", markConversationRead);
  els.friendAppliesBody.addEventListener("click", handleSocialAction);
  els.friendsBody.addEventListener("click", handleSocialAction);
  els.conversationList.addEventListener("click", handleSocialAction);
  els.socialActiveUser.addEventListener("change", onSocialIdentityChange);
  els.clearLogBtn.addEventListener("click", () => {
    els.logOutput.textContent = "Ready.";
  });
}

function init() {
  initElements();
  testUserSlot();
  setTeamTestSlot(state.teamTest.activeSlot || "A");
  setupDefaults();
  updateAuthUi();
  updateTeamTestUi();
  setupEvents();
  renderEmpty(els.usersBody, 7);
  renderEmpty(els.activitiesBody, 7);
  renderEmpty(els.teamsBody, 5);
  renderEmpty(els.friendAppliesBody, 6);
  renderEmpty(els.friendsBody, 4);
  refreshDashboard();
}

init();
