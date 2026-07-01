const config = require("../config/index");

const MAX_RECONNECT = 3;
const RECONNECT_DELAY_MS = 2000;
const HEARTBEAT_MS = 10000;
const PRIVATE_DESTINATION = "/user/queue/messages";

let socketReady = false;
let stompConnected = false;
let connecting = false;
let manualClose = false;
let reconnectAttempts = 0;
let reconnectTimer = null;
let heartbeatTimer = null;
let frameBuffer = "";
let subscriptionSeq = 0;
let handlersBound = false;

const destinationSubscriptions = new Map();
const eventListeners = new Set();

function emit(event) {
  eventListeners.forEach((listener) => {
    try {
      listener(event);
    } catch (err) {
      console.warn("im-realtime listener failed", err);
    }
  });
}

function isConnected() {
  return socketReady && stompConnected;
}

function onEvent(listener) {
  eventListeners.add(listener);
  return () => eventListeners.delete(listener);
}

function buildWsUrl() {
  const token = wx.getStorageSync("accessToken");
  if (!token || !config.wsBase) {
    return "";
  }
  return `${config.wsBase}?token=${encodeURIComponent(token)}`;
}

function sendRaw(data) {
  if (!socketReady) {
    return;
  }
  wx.sendSocketMessage({
    data,
    fail(err) {
      console.warn("im-realtime send failed", err);
    }
  });
}

function sendFrame(command, headers, body) {
  let frame = `${command}\n`;
  Object.keys(headers || {}).forEach((key) => {
    frame += `${key}:${headers[key]}\n`;
  });
  frame += `\n${body || ""}\0`;
  sendRaw(frame);
}

function subscribeDestination(destination) {
  if (!destination || destinationSubscriptions.has(destination)) {
    return;
  }
  subscriptionSeq += 1;
  const subId = `sub-${subscriptionSeq}`;
  destinationSubscriptions.set(destination, subId);
  if (stompConnected) {
    sendFrame("SUBSCRIBE", {
      id: subId,
      destination
    });
  }
}

function unsubscribeDestination(destination) {
  const subId = destinationSubscriptions.get(destination);
  if (!subId) {
    return;
  }
  destinationSubscriptions.delete(destination);
  if (stompConnected) {
    sendFrame("UNSUBSCRIBE", { id: subId });
  }
}

function subscribePrivate() {
  subscribeDestination(PRIVATE_DESTINATION);
}

function subscribeTeam(teamId) {
  if (!teamId) {
    return;
  }
  subscribeDestination(`/topic/team/${teamId}`);
}

function unsubscribeTeam(teamId) {
  if (!teamId) {
    return;
  }
  unsubscribeDestination(`/topic/team/${teamId}`);
}

function resubscribeAll() {
  destinationSubscriptions.forEach((subId, destination) => {
    sendFrame("SUBSCRIBE", {
      id: subId,
      destination
    });
  });
}

function clearHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}

function startHeartbeat() {
  clearHeartbeat();
  heartbeatTimer = setInterval(() => {
    if (stompConnected) {
      sendRaw("\n");
    }
  }, HEARTBEAT_MS);
}

function handleConnected() {
  stompConnected = true;
  reconnectAttempts = 0;
  resubscribeAll();
  startHeartbeat();
}

function parseEventBody(body) {
  if (!body) {
    return null;
  }
  try {
    return JSON.parse(body);
  } catch (err) {
    console.warn("im-realtime invalid event body", body);
    return null;
  }
}

function handleStompFrame(rawFrame) {
  if (!rawFrame) {
    return;
  }
  if (rawFrame === "\n" || rawFrame === "\r\n") {
    return;
  }

  const lines = rawFrame.split("\n");
  const command = lines[0];
  const headers = {};
  let bodyStart = 1;

  for (let i = 1; i < lines.length; i += 1) {
    if (lines[i] === "") {
      bodyStart = i + 1;
      break;
    }
    const colonIndex = lines[i].indexOf(":");
    if (colonIndex > 0) {
      headers[lines[i].slice(0, colonIndex)] = lines[i].slice(colonIndex + 1);
    }
  }

  const body = lines.slice(bodyStart).join("\n");

  if (command === "CONNECTED") {
    handleConnected();
    return;
  }

  if (command === "MESSAGE") {
    const event = parseEventBody(body);
    if (event) {
      emit(event);
    }
    return;
  }

  if (command === "ERROR") {
    console.warn("im-realtime stomp error", body || headers.message);
  }
}

function consumeIncoming(data) {
  if (typeof data !== "string") {
    return;
  }
  if (data === "\n") {
    return;
  }
  frameBuffer += data;
  while (frameBuffer.includes("\0")) {
    const endIndex = frameBuffer.indexOf("\0");
    const frame = frameBuffer.slice(0, endIndex);
    frameBuffer = frameBuffer.slice(endIndex + 1);
    handleStompFrame(frame);
  }
}

function bindSocketHandlers() {
  if (handlersBound) {
    return;
  }
  handlersBound = true;

  wx.onSocketOpen(() => {
    socketReady = true;
    connecting = false;
    sendFrame("CONNECT", {
      "accept-version": "1.1,1.2",
      "heart-beat": `${HEARTBEAT_MS},${HEARTBEAT_MS}`
    });
  });

  wx.onSocketMessage((res) => {
    consumeIncoming(res.data);
  });

  wx.onSocketClose(() => {
    socketReady = false;
    stompConnected = false;
    connecting = false;
    clearHeartbeat();
    if (!manualClose) {
      scheduleReconnect();
    }
  });

  wx.onSocketError((err) => {
    console.warn("im-realtime socket error", err);
    socketReady = false;
    stompConnected = false;
    connecting = false;
    clearHeartbeat();
    if (!manualClose) {
      scheduleReconnect();
    }
  });
}

function scheduleReconnect() {
  if (manualClose || reconnectTimer) {
    return;
  }
  if (reconnectAttempts >= MAX_RECONNECT) {
    return;
  }
  if (!wx.getStorageSync("accessToken")) {
    return;
  }
  reconnectAttempts += 1;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect(true);
  }, RECONNECT_DELAY_MS);
}

function connect(forceReconnect) {
  const token = wx.getStorageSync("accessToken");
  if (!token) {
    return Promise.resolve(false);
  }
  if (isConnected()) {
    return Promise.resolve(true);
  }
  if (connecting && !forceReconnect) {
    return Promise.resolve(false);
  }

  const url = buildWsUrl();
  if (!url) {
    return Promise.resolve(false);
  }

  manualClose = false;
  connecting = true;
  frameBuffer = "";

  if (forceReconnect || socketReady) {
    try {
      wx.closeSocket({});
    } catch (err) {
      // ignore close errors during reconnect
    }
    socketReady = false;
    stompConnected = false;
  }

  return new Promise((resolve) => {
    wx.connectSocket({
      url,
      success() {
        resolve(true);
      },
      fail(err) {
        connecting = false;
        console.warn("im-realtime connect failed", err);
        scheduleReconnect();
        resolve(false);
      }
    });
  });
}

function disconnect() {
  manualClose = true;
  reconnectAttempts = 0;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  clearHeartbeat();
  frameBuffer = "";
  socketReady = false;
  stompConnected = false;
  connecting = false;
  destinationSubscriptions.clear();
  subscriptionSeq = 0;
  try {
    wx.closeSocket({});
  } catch (err) {
    // ignore
  }
}

function ensureConnected() {
  if (!wx.getStorageSync("accessToken")) {
    return;
  }
  if (!isConnected()) {
    subscribePrivate();
    connect();
    return;
  }
  subscribePrivate();
}

bindSocketHandlers();

module.exports = {
  connect,
  disconnect,
  ensureConnected,
  isConnected,
  onEvent,
  subscribeTeam,
  unsubscribeTeam
};
