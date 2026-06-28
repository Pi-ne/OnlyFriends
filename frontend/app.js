const state = {
  baseUrl: localStorage.getItem("of.baseUrl") || "http://localhost:8080/api/v1",
  adminToken: localStorage.getItem("of.adminToken") || "",
  adminInfo: JSON.parse(localStorage.getItem("of.adminInfo") || "null"),
  userToken: localStorage.getItem("of.userToken") || ""
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
    "createTeamForm", "teamCreatorToken", "createUserForm", "loginUserForm",
    "createActivityForm", "activityCreatorToken", "normalEmail", "seedEmail",
    "seedNickname", "apiMethod", "apiPath", "apiBody", "apiUseAdminToken",
    "apiSendBtn", "clearLogBtn", "logOutput", "pageTitle"
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
  els.teamCreatorToken.value = state.userToken;
  els.activityCreatorToken.value = state.userToken;
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
    localStorage.setItem("of.userToken", state.userToken);
    updateAuthUi();
    log("普通用户登录成功", result.userInfo || result);
  } catch (error) {
    log("普通用户登录失败", error.message);
  }
}

async function createTeam(event) {
  event.preventDefault();
  const data = formData(event.currentTarget);
  const token = els.teamCreatorToken.value.trim();
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
    log("兴趣组创建成功", result);
    loadTeams();
  } catch (error) {
    log("兴趣组创建失败", error.message);
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
  els.createActivityForm.addEventListener("submit", createActivity);
  els.apiSendBtn.addEventListener("click", sendApiRequest);
  els.clearLogBtn.addEventListener("click", () => {
    els.logOutput.textContent = "Ready.";
  });
}

function init() {
  initElements();
  setupDefaults();
  updateAuthUi();
  setupEvents();
  renderEmpty(els.usersBody, 7);
  renderEmpty(els.activitiesBody, 7);
  renderEmpty(els.teamsBody, 5);
  refreshDashboard();
}

init();
