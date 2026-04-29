const STORAGE_KEY = "campus_exchange_data_v2";
const USER_KEY = "campus_exchange_user_v1";
const TOKEN_KEY = "campus_exchange_token_v1";
const MESSAGE_READ_KEY = "campus_exchange_message_read_v1";
const ADMIN_STUDENT_IDS = new Set(["99990000"]);
const APP_CONFIG = window.__APP_CONFIG__ || {};
const API_BASE_URL = resolveApiBaseUrl();
const PREVIEW_MODE = Boolean(APP_CONFIG.previewMode);

const seedData = {
  goods: [
    {
      id: "g1",
      title: "《高等数学》同济版 九成新",
      category: "教材",
      price: 20,
      desc: "无笔记，封面完整，可当面交易。",
      imageUrl: "",
      sellerId: "u20230001",
      sellerName: "老曹",
      tradeType: "当面交易",
      status: "sale",
      createdAt: "2026-04-19T11:00:00+08:00",
      views: 23,
    },
    {
      id: "g2",
      title: "小米蓝牙耳机",
      category: "数码",
      price: 65,
      desc: "功能正常，含充电线。",
      imageUrl: "",
      sellerId: "u20230123",
      sellerName: "小徐",
      tradeType: "宿舍自提",
      status: "sale",
      createdAt: "2026-04-18T16:20:00+08:00",
      views: 36,
    },
    {
      id: "g3",
      title: "篮球 7号 训练用",
      category: "运动",
      price: 30,
      desc: "轻微磨损，适合训练。",
      imageUrl: "",
      sellerId: "u20230555",
      sellerName: "阿豪",
      tradeType: "当面交易",
      status: "sold",
      createdAt: "2026-04-16T13:00:00+08:00",
      views: 41,
    },
  ],
  messages: [
    {
      id: "m1",
      from: "系统通知",
      toId: "u20230001",
      item: "信用分更新",
      text: "你收到一条好评，信用分 +3。",
      time: "昨天 18:00",
    },
  ],
  intents: [],
  reports: [],
  auditLogs: [],
  credits: {
    u20230001: 92,
    u20230123: 85,
    u20230555: 88,
    u99990000: 100,
  },
  ratings: [
    {
      id: "r1",
      fromId: "u20230123",
      toId: "u20230001",
      score: 5,
      itemId: "g1",
      time: "2026-04-18T10:00:00+08:00",
    },
  ],
  favorites: {
    u20230001: ["g2"],
  },
  users: {
    u99990000: {
      id: "u99990000",
      name: "王老师",
      studentId: "99990000",
      email: "admin@campus.edu.cn",
      role: "admin",
      status: "active",
    },
  },
};

const state = {
  tab: "home",
  previousTab: "home",
  detailItemId: "",
  search: "",
  category: "all",
  sort: "latest",
  intentFilter: "all",
  intentFocusId: "",
  messageSearch: "",
  messageFocusKey: "",
  adminGoodsKeyword: "",
  adminGoodsStatus: "all",
  adminSelectedGoods: new Set(),
  data: normalizeData(structuredClone(seedData)),
  currentUser: loadUser(),
  authToken: loadToken(),
  backendMode: PREVIEW_MODE ? "preview" : "loading",
};

const appEl = document.getElementById("app");
const tabs = document.querySelectorAll(".tab");
const tabbar = document.getElementById("tabbar");
const adminTabBtn = document.getElementById("adminTabBtn");
const quickSellBtn = document.getElementById("quickSellBtn");
const installBtn = document.getElementById("installBtn");
const detailDialog = document.getElementById("detailDialog");
const shell = document.getElementById("shell");
const eyebrow = document.getElementById("eyebrow");
const appTitle = document.getElementById("appTitle");
const overlayRoot = document.getElementById("overlayRoot");
const toastRoot = document.getElementById("toastRoot");

let deferredInstallPrompt = null;
let saveQueue = Promise.resolve();
let toastTimer = null;

tabs.forEach((tabBtn) => {
  tabBtn.addEventListener("click", () => {
    if (
      state.currentUser &&
      isBlockedCurrentUser() &&
      !isAdmin() &&
      (tabBtn.dataset.tab === "publish" || tabBtn.dataset.tab === "messages")
    ) {
      showToast("当前账号已被封禁，仅可浏览与查看个人信息。");
      return;
    }
    state.tab = tabBtn.dataset.tab;
    setActiveTab();
    render();
  });
});

quickSellBtn.addEventListener("click", () => {
  if (!ensureActiveUser("发布商品")) return;
  state.tab = "publish";
  setActiveTab();
  render();
});

installBtn.addEventListener("click", async () => {
  if (!deferredInstallPrompt) return;
  deferredInstallPrompt.prompt();
  await deferredInstallPrompt.userChoice;
  deferredInstallPrompt = null;
  installBtn.classList.add("hidden");
});

window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  deferredInstallPrompt = event;
  installBtn.classList.remove("hidden");
});

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    if (PREVIEW_MODE) {
      navigator.serviceWorker.getRegistrations().then((registrations) => {
        registrations.forEach((registration) => registration.unregister());
      }).catch(() => null);
      return;
    }
    navigator.serviceWorker.register("./sw.js").catch(() => null);
  });
}

function normalizeData(data) {
  const normalized = data || {};
  normalized.goods = Array.isArray(normalized.goods) ? normalized.goods : [];
  normalized.messages = Array.isArray(normalized.messages) ? normalized.messages : [];
  normalized.intents = Array.isArray(normalized.intents) ? normalized.intents : [];
  normalized.reports = Array.isArray(normalized.reports) ? normalized.reports : [];
  normalized.auditLogs = Array.isArray(normalized.auditLogs) ? normalized.auditLogs : [];
  normalized.ratings = Array.isArray(normalized.ratings) ? normalized.ratings : [];
  normalized.credits = normalized.credits || {};
  normalized.favorites = normalized.favorites || {};
  normalized.users = normalized.users || {};
  normalized.messages = normalized.messages.map((msg) => normalizeMessageRecord(msg, normalized.users));
  return normalized;
}

function findUserIdByName(name, users = {}) {
  const target = String(name || "").trim();
  if (!target) return "";
  const matched = Object.values(users).filter((user) => String(user?.name || "").trim() === target);
  if (matched.length === 1) return String(matched[0].id || "");
  return "";
}

function normalizeMessageRecord(message, users = {}) {
  const normalized = { ...(message || {}) };
  if (normalized.from === "系统通知" || normalized.fromId === "system") {
    normalized.from = "系统通知";
    normalized.fromId = "system";
  }
  if (!normalized.fromId && normalized.from) {
    const userId = findUserIdByName(normalized.from, users);
    if (userId) normalized.fromId = userId;
  }
  if (!normalized.createdAt) {
    const ts = Date.parse(String(normalized.time || ""));
    if (Number.isFinite(ts)) normalized.createdAt = new Date(ts).toISOString();
  }
  return normalized;
}

function normalizeUser(user) {
  if (!user) return null;
  const studentId = String(user.studentId || "");
  return {
    ...user,
    role: user.role || (ADMIN_STUDENT_IDS.has(studentId) ? "admin" : "user"),
    status: user.status || "active",
  };
}

function isAdmin() {
  return state.currentUser?.role === "admin";
}

function isBlockedCurrentUser() {
  return state.currentUser?.status === "blocked";
}

function ensureActiveUser(actionName) {
  if (!isBlockedCurrentUser()) return true;
  showToast(`当前账号已被封禁，无法${actionName}。`);
  return false;
}

function loadUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return normalizeUser(JSON.parse(raw));
  } catch {
    return null;
  }
}

function saveUser() {
  localStorage.setItem(USER_KEY, JSON.stringify(state.currentUser));
}

function resetUser() {
  localStorage.removeItem(USER_KEY);
}

function loadToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

function saveToken() {
  localStorage.setItem(TOKEN_KEY, state.authToken || "");
}

function resetToken() {
  localStorage.removeItem(TOKEN_KEY);
}

function loadMessageReadMap() {
  const raw = localStorage.getItem(MESSAGE_READ_KEY);
  if (!raw) return {};
  try {
    const data = JSON.parse(raw);
    return data && typeof data === "object" ? data : {};
  } catch {
    return {};
  }
}

function saveMessageReadMap(map) {
  localStorage.setItem(MESSAGE_READ_KEY, JSON.stringify(map || {}));
}

function getReadSetForCurrentUser() {
  if (!state.currentUser) return new Set();
  const map = loadMessageReadMap();
  const list = Array.isArray(map[state.currentUser.id]) ? map[state.currentUser.id] : [];
  return new Set(list);
}

function markMessagesRead(ids = []) {
  if (!state.currentUser || !ids.length) return;
  const map = loadMessageReadMap();
  const prev = new Set(Array.isArray(map[state.currentUser.id]) ? map[state.currentUser.id] : []);
  ids.forEach((id) => prev.add(id));
  map[state.currentUser.id] = Array.from(prev);
  saveMessageReadMap(map);
}

function getUnreadCount() {
  if (!state.currentUser) return 0;
  const readSet = getReadSetForCurrentUser();
  return state.data.messages.filter((msg) => msg.toId === state.currentUser.id && !readSet.has(msg.id)).length;
}

function loadDataLocal() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return normalizeData(structuredClone(seedData));
  try {
    return normalizeData(JSON.parse(raw));
  } catch {
    return normalizeData(structuredClone(seedData));
  }
}

function saveDataLocal() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state.data));
}

function resolveApiBaseUrl() {
  const configured = String(APP_CONFIG.apiBaseUrl || "").trim();
  return configured.replace(/\/+$/, "");
}

function buildApiUrl(path) {
  if (/^https?:\/\//i.test(path)) return path;
  if (!API_BASE_URL) return path;
  return `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

async function apiRequest(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (state.authToken) headers.Authorization = `Bearer ${state.authToken}`;

  const response = await fetch(buildApiUrl(path), {
    ...options,
    headers,
  });
  if (!response.ok) throw new Error(`API ${response.status}`);
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function bootstrapData() {
  if (PREVIEW_MODE) {
    state.data = loadDataLocal();
    state.backendMode = "preview";
    return;
  }
  if (!state.authToken) {
    state.data = loadDataLocal();
    state.backendMode = "online";
    return;
  }
  try {
    const remote = await apiRequest("/api/state");
    state.data = normalizeData(remote);
    state.backendMode = "online";
  } catch (error) {
    if (String(error.message || "").includes("401")) {
      state.authToken = "";
      resetToken();
    }
    state.data = loadDataLocal();
    state.backendMode = "offline";
  }
}

function persistMutation(task) {
  saveQueue = saveQueue
    .then(async () => {
      if (state.backendMode === "online") {
        try {
          await task();
        } catch {
          state.backendMode = "offline";
        }
      }
      saveDataLocal();
      updateShellState();
    })
    .catch(() => null);

  return saveQueue;
}

function showToast(message) {
  if (!toastRoot) return;
  const text = String(message || "").trim();
  if (!text) return;
  toastRoot.textContent = text;
  toastRoot.classList.remove("hidden");
  toastRoot.classList.add("show");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => {
    toastRoot.classList.remove("show");
    toastRoot.classList.add("hidden");
  }, 2200);
}

function closeOverlay() {
  if (!overlayRoot) return;
  overlayRoot.innerHTML = "";
  overlayRoot.classList.add("hidden");
}

function showOverlay(content) {
  if (!overlayRoot) return;
  overlayRoot.innerHTML = content;
  overlayRoot.classList.remove("hidden");

  const mask = overlayRoot.querySelector(".overlay-mask");
  if (mask) {
    mask.addEventListener("click", (event) => {
      if (event.target === mask) closeOverlay();
    });
  }

  overlayRoot.querySelectorAll("[data-overlay-close]").forEach((button) => {
    button.addEventListener("click", () => closeOverlay());
  });
}

function showConfirmOverlay({ title, description, confirmText = "确定", tone = "primary", onConfirm }) {
  showOverlay(`
    <div class="overlay-mask">
      <section class="overlay-card">
        <p class="overlay-kicker">操作确认</p>
        <h3>${escapeHtml(title)}</h3>
        <p class="overlay-copy">${escapeHtml(description || "")}</p>
        <div class="overlay-actions">
          <button class="ghost-btn" data-overlay-close>取消</button>
          <button class="${tone === "danger" ? "danger-btn inline-danger" : "primary-btn"}" id="overlayConfirmBtn">${escapeHtml(confirmText)}</button>
        </div>
      </section>
    </div>
  `);

  document.getElementById("overlayConfirmBtn")?.addEventListener("click", () => {
    closeOverlay();
    onConfirm?.();
  });
}

function openReportComposer(item) {
  showOverlay(`
    <div class="overlay-mask">
      <section class="overlay-card overlay-form-card">
        <p class="overlay-kicker">商品举报</p>
        <h3>举报 ${escapeHtml(item.title)}</h3>
        <p class="overlay-copy">请说明举报原因，管理员会依据描述审核处理。</p>
        <form id="reportComposerForm" class="overlay-form">
          <label>
            举报原因
            <textarea name="reason" rows="4" maxlength="120" placeholder="例如：疑似违规商品、虚假描述、恶意占位">疑似违规商品</textarea>
          </label>
          <p class="overlay-help">建议写清问题点，审核会更快。</p>
          <div class="overlay-actions">
            <button type="button" class="ghost-btn" data-overlay-close>取消</button>
            <button type="submit" class="primary-btn">提交举报</button>
          </div>
        </form>
      </section>
    </div>
  `);

  document.getElementById("reportComposerForm")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const reason = String(formData.get("reason") || "").trim();
    if (reason.length < 2) {
      showToast("请至少填写 2 个字的举报原因。");
      return;
    }

    const report = {
      id: `rp${Date.now()}`,
      goodsId: item.id,
      goodsTitle: item.title,
      reporterId: state.currentUser.id,
      reporterName: state.currentUser.name,
      sellerId: item.sellerId,
      reason,
      status: "pending",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    state.data.reports.unshift(report);
    state.data.messages.unshift({
      id: `m${Date.now()}`,
      from: "系统通知",
      toId: state.currentUser.id,
      item: item.title,
      text: "你的举报已提交，管理员会尽快审核。",
      time: "刚刚",
    });

    void persistMutation(() =>
      apiRequest("/api/reports", {
        method: "POST",
        body: JSON.stringify({ report }),
      }),
    );

    closeOverlay();
    showToast("举报已提交。");
  });
}

function submitIntentRating(intent, score) {
  const createdRating = {
    id: `r${Date.now()}`,
    fromId: state.currentUser.id,
    toId: intent.sellerId,
    score,
    itemId: intent.itemId,
    time: new Date().toISOString(),
  };
  state.data.ratings.unshift(createdRating);
  intent.rated = true;
  addCredit(intent.sellerId, 3);
  void persistMutation(() =>
    apiRequest("/api/ratings", {
      method: "POST",
      body: JSON.stringify({ rating: createdRating, creditDelta: 3 }),
    }),
  );
  showToast("评分已提交。");
}

function openRatingComposer(intent) {
  showOverlay(`
    <div class="overlay-mask">
      <section class="overlay-card overlay-form-card">
        <p class="overlay-kicker">交易评价</p>
        <h3>给卖家 ${escapeHtml(intent.sellerName)} 打分</h3>
        <p class="overlay-copy">评分会影响卖家的信用展示，请基于本次交易体验选择。</p>
        <form id="ratingComposerForm" class="overlay-form">
          <div class="rating-grid">
            ${[5, 4, 3, 2, 1]
              .map(
                (score) => `
                  <label class="rating-option">
                    <input type="radio" name="score" value="${score}" ${score === 5 ? "checked" : ""} />
                    <span>${score} 分</span>
                  </label>
                `,
              )
              .join("")}
          </div>
          <p class="overlay-help">5 分表示非常满意，1 分表示体验较差。</p>
          <div class="overlay-actions">
            <button type="button" class="ghost-btn" data-overlay-close>取消</button>
            <button type="submit" class="primary-btn">提交评分</button>
          </div>
        </form>
      </section>
    </div>
  `);

  document.getElementById("ratingComposerForm")?.addEventListener("submit", (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const score = Number(formData.get("score") || 0);
    if (!score || score < 1 || score > 5) {
      showToast("评分需在 1 到 5 分之间。");
      return;
    }
    closeOverlay();
    submitIntentRating(intent, score);
    renderMessages();
  });
}

function setActiveTab() {
  tabs.forEach((t) => t.classList.toggle("active", t.dataset.tab === state.tab));
}

function openDetailScreen(itemId, sourceTab = state.tab) {
  if (!itemId) return;
  state.detailItemId = itemId;
  state.previousTab = sourceTab === "detail" ? state.previousTab || "home" : sourceTab;
  state.tab = "detail";
  setActiveTab();
  render();
}

function closeDetailScreen() {
  state.tab = state.previousTab || "home";
  state.detailItemId = "";
  setActiveTab();
  render();
}

function getCredit(userId) {
  return Number(state.data.credits[userId] || 80);
}

function addCredit(userId, delta) {
  state.data.credits[userId] = getCredit(userId) + delta;
}

function getFavSet() {
  if (!state.currentUser) return new Set();
  const list = state.data.favorites[state.currentUser.id] || [];
  return new Set(list);
}

function toggleFavorite(goodsId) {
  if (!state.currentUser) return;
  const userId = state.currentUser.id;
  const list = new Set(state.data.favorites[userId] || []);
  if (list.has(goodsId)) list.delete(goodsId);
  else list.add(goodsId);
  const favorites = Array.from(list);
  state.data.favorites[userId] = favorites;
  void persistMutation(() =>
    apiRequest(`/api/users/${encodeURIComponent(userId)}/favorites`, {
      method: "PUT",
      body: JSON.stringify({ favorites }),
    }),
  );
  render();
}

function statusLabel(status) {
  if (status === "sale") return "在售";
  if (status === "sold") return "已售";
  if (status === "blocked") return "已下架";
  return "未知";
}

function renderIntentTimeline(status) {
  const steps = [
    { key: "created", label: "已发起" },
    { key: "accepted", label: "已确认" },
    { key: "completed", label: "已完成" },
  ];
  const level = status === "pending" ? 1 : status === "accepted" ? 2 : status === "completed" ? 3 : 1;
  const rejected = status === "rejected";

  return `
    <div class="intent-timeline ${rejected ? "rejected" : ""}">
      ${steps
        .map(
          (step, index) => `
          <span class="timeline-node ${index + 1 <= level ? "done" : ""}">${step.label}</span>
        `,
        )
        .join("")}
      ${rejected ? '<span class="timeline-rejected">已拒绝</span>' : ""}
    </div>
  `;
}

function intentStatusText(status) {
  if (status === "pending") return "待确认";
  if (status === "accepted") return "待交易";
  if (status === "completed") return "已完成";
  if (status === "rejected") return "已拒绝";
  return "处理中";
}

function getDisplayNameByUserId(userId) {
  if (!userId) return "";
  return state.data.users[userId]?.name || "";
}

function getMessageSenderName(msg) {
  if (msg.fromId === "system") return "系统通知";
  return getDisplayNameByUserId(msg.fromId) || msg.from || "匿名用户";
}

function isMessageSentByCurrentUser(msg) {
  if (!state.currentUser) return false;
  if (msg.fromId) return msg.fromId === state.currentUser.id;
  return msg.from === state.currentUser.name;
}

function isMessageRelatedToCurrentUser(msg) {
  if (!state.currentUser) return false;
  if (msg.toId === state.currentUser.id) return true;
  if (isMessageSentByCurrentUser(msg)) return true;
  if ((msg.fromId === "system" || msg.from === "系统通知") && (!msg.toId || msg.toId === state.currentUser.id)) return true;
  return false;
}

function getMessageThreadKey(msg) {
  if (!state.currentUser) return "thread:none";
  if (msg.intentId) return `intent:${msg.intentId}`;
  if (msg.fromId === "system" || msg.from === "系统通知") {
    return `system:${msg.item || msg.text || "系统消息"}`;
  }

  const myId = state.currentUser.id;
  const peerId = msg.fromId === myId ? msg.toId : msg.fromId;
  if (peerId) {
    if (msg.itemId) return `peer-item:${peerId}:${msg.itemId}`;
    if (msg.item) return `peer-topic:${peerId}:${msg.item}`;
    return `peer:${peerId}`;
  }

  if (msg.toId === myId && msg.from) return `legacy-name:${msg.from}:${msg.item || "消息"}`;
  if (isMessageSentByCurrentUser(msg) && msg.item) return `legacy-self:${msg.item}`;
  return `legacy:${msg.from || "匿名"}:${msg.toId || "none"}:${msg.item || "消息"}`;
}

function getMessageTimestamp(msg) {
  const direct = Date.parse(msg.createdAt || msg.updatedAt || msg.time || "");
  if (Number.isFinite(direct)) return direct;
  const fallback = Number(String(msg.id || "").replace(/\D/g, ""));
  return Number.isFinite(fallback) ? fallback : 0;
}

function isIntentParticipant(intent, userId) {
  return intent.buyerId === userId || intent.sellerId === userId;
}

function getIntentThreadMessages(intent) {
  return state.data.messages
    .filter((msg) => {
      if (msg.intentId && msg.intentId === intent.id) return true;
      if (msg.itemId && msg.itemId !== intent.itemId) return false;
      if (!msg.fromId || !msg.toId) return false;
      return (
        isIntentParticipant(intent, msg.fromId) &&
        isIntentParticipant(intent, msg.toId)
      );
    })
    .sort((a, b) => getMessageTimestamp(a) - getMessageTimestamp(b));
}

function buildIntentMessage(intent, text, toUserId) {
  const nowIso = new Date().toISOString();
  return {
    id: `m${Date.now()}${Math.floor(Math.random() * 1000)}`,
    intentId: intent.id,
    itemId: intent.itemId,
    item: intent.itemTitle,
    from: state.currentUser.name,
    fromId: state.currentUser.id,
    toId: toUserId,
    text: String(text || "").trim(),
    createdAt: nowIso,
    time: formatDate(nowIso),
  };
}

function sendIntentMessage(intentId, rawText) {
  if (!ensureActiveUser("发送消息")) return;
  const intent = state.data.intents.find((it) => it.id === intentId);
  if (!intent) return;
  const text = String(rawText || "").trim();
  if (!text) return;

  const toUserId = state.currentUser.id === intent.buyerId ? intent.sellerId : intent.buyerId;
  const message = buildIntentMessage(intent, text, toUserId);
  state.data.messages.unshift(message);
  intent.updatedAt = message.createdAt;

  void persistMutation(async () => {
    await apiRequest("/api/messages", {
      method: "POST",
      body: JSON.stringify({ message }),
    });
    await apiRequest(`/api/intents/${encodeURIComponent(intent.id)}`, {
      method: "PATCH",
      body: JSON.stringify({
        updatedAt: intent.updatedAt,
      }),
    });
  });

  renderMessages();
}

function getAdminFilteredGoods() {
  const keyword = state.adminGoodsKeyword.trim().toLowerCase();
  return state.data.goods.filter((item) => {
    const hitStatus = state.adminGoodsStatus === "all" || item.status === state.adminGoodsStatus;
    const hitKeyword =
      !keyword ||
      item.title.toLowerCase().includes(keyword) ||
      item.desc.toLowerCase().includes(keyword) ||
      item.sellerName.toLowerCase().includes(keyword);
    return hitStatus && hitKeyword;
  });
}

function render() {
  updateShellState();
  if (!state.currentUser) {
    renderAuth();
    return;
  }

  if (state.tab === "admin" && !isAdmin()) {
    state.tab = "home";
    setActiveTab();
  }
  if (!isAdmin() && isBlockedCurrentUser() && (state.tab === "publish" || state.tab === "messages")) {
    state.tab = "home";
    setActiveTab();
  }

  if (state.tab === "home") renderHome();
  if (state.tab === "publish") renderPublish();
  if (state.tab === "messages") renderMessages();
  if (state.tab === "profile") renderProfile();
  if (state.tab === "admin") renderAdmin();
  if (state.tab === "detail") renderDetailScreen();
}

function updateShellState() {
  const loggedIn = Boolean(state.currentUser);
  shell.classList.toggle("auth-mode", !loggedIn);
  quickSellBtn.classList.toggle("hidden", !loggedIn || isBlockedCurrentUser());
  const unreadCount = loggedIn ? getUnreadCount() : 0;
  const msgTabBtn = document.querySelector('.tab[data-tab="messages"]');
  if (msgTabBtn) {
    msgTabBtn.textContent = unreadCount > 0 ? `交易 (${unreadCount})` : "交易";
  }

  const adminMode = loggedIn && isAdmin();
  adminTabBtn.classList.toggle("hidden", !adminMode);
  tabbar.classList.toggle("admin-mode", adminMode);
  shell.classList.toggle("preview-mode", state.backendMode === "preview");

  const modeText =
    state.backendMode === "online"
      ? "在线模式"
      : state.backendMode === "offline"
        ? "离线模式"
        : state.backendMode === "preview"
          ? "预览模式"
        : "加载中";

  eyebrow.textContent = loggedIn
    ? `学号 ${state.currentUser.studentId} 已认证 · ${state.currentUser.status === "blocked" ? "已封禁 · " : ""}${modeText}`
    : `校园认证二手交易 · ${modeText}`;

  appTitle.textContent = loggedIn ? `${state.currentUser.name} 的校园淘` : "MY校园淘二手";
}

function renderAuth() {
  const tpl = document.getElementById("authTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const form = document.getElementById("authForm");
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(form);
    const name = String(formData.get("name") || "").trim();
    const studentId = String(formData.get("studentId") || "").trim();
    const email = String(formData.get("email") || "").trim();

    if (!/^[0-9]{8}$/.test(studentId)) {
      showToast("学号需为 8 位数字。");
      return;
    }

    if (state.backendMode === "online") {
      try {
        const auth = await apiRequest("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({ name, studentId, email }),
        });
        if (!auth || !auth.user) throw new Error("Invalid auth response");
        state.authToken = String(auth.token || "");
        state.currentUser = normalizeUser(auth.user || null);
        saveToken();
        await bootstrapData();
      } catch {
        state.currentUser = normalizeUser({ id: `u${studentId}`, name, studentId, email });
        state.authToken = "";
        resetToken();
        state.backendMode = "offline";
      }
    } else if (state.backendMode === "preview") {
      state.currentUser = normalizeUser({ id: `u${studentId}`, name, studentId, email });
    } else {
      state.currentUser = normalizeUser({ id: `u${studentId}`, name, studentId, email });
    }

    if (!state.data.credits[state.currentUser.id]) state.data.credits[state.currentUser.id] = 80;
    if (!state.data.favorites[state.currentUser.id]) state.data.favorites[state.currentUser.id] = [];
    state.data.users[state.currentUser.id] = {
      ...(state.data.users[state.currentUser.id] || {}),
      ...state.currentUser,
    };

    saveUser();
    saveDataLocal();
    state.tab = "home";
    setActiveTab();
    render();
  });
}

function renderHome() {
  const tpl = document.getElementById("homeTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const favList = state.data.favorites[state.currentUser.id] || [];
  document.getElementById("homeGreet").textContent = `你好，${state.currentUser.name}`;
  document.getElementById("favCountChip").textContent = `收藏 ${favList.length}`;

  const searchInput = document.getElementById("searchInput");
  const categoryFilter = document.getElementById("categoryFilter");
  const sortBtns = document.querySelectorAll(".sort-btn");
  const quickCards = document.querySelectorAll(".quick-card");

  searchInput.value = state.search;
  categoryFilter.value = state.category;
  sortBtns.forEach((btn) => btn.classList.toggle("active", btn.dataset.sort === state.sort));

  searchInput.addEventListener("input", (e) => {
    state.search = e.target.value.trim();
    renderGoodsList();
  });

  categoryFilter.addEventListener("change", (e) => {
    state.category = e.target.value;
    renderGoodsList();
  });

  sortBtns.forEach((btn) => {
    btn.addEventListener("click", () => {
      state.sort = btn.dataset.sort;
      renderHome();
    });
  });

  quickCards.forEach((btn) => {
    btn.addEventListener("click", () => {
      state.category = btn.dataset.quickCategory || "all";
      renderHome();
    });
  });

  renderGoodsList();
}

function getFilteredGoods() {
  const keyword = state.search.toLowerCase();
  const filtered = state.data.goods.filter((item) => {
    if (!isAdmin() && item.status === "blocked") return false;
    const hitKeyword =
      item.title.toLowerCase().includes(keyword) || item.desc.toLowerCase().includes(keyword);
    const hitCategory = state.category === "all" || state.category === item.category;
    return hitKeyword && hitCategory;
  });

  return filtered.sort((a, b) => {
    if (state.sort === "priceAsc") return a.price - b.price;
    if (state.sort === "priceDesc") return b.price - a.price;
    if (state.sort === "popular") return b.views - a.views;
    return new Date(b.createdAt) - new Date(a.createdAt);
  });
}

function renderGoodsList(targetListId = "goodsList") {
  const listEl = document.getElementById(targetListId);
  if (!listEl) return;

  const goods = getFilteredGoods();
  const favs = getFavSet();
  const isHomeFeed = targetListId === "goodsList";
  const countEl = document.getElementById("resultCount");
  if (countEl) countEl.textContent = `${goods.length} 件`;

  if (!goods.length) {
    listEl.innerHTML = '<div class="card">暂无匹配商品，试试换个关键词。</div>';
    return;
  }

  listEl.innerHTML = goods
    .map(
      (item) => `
      <article
        class="goods-item goods-feed-item ${isHomeFeed ? "goods-tile clickable" : ""}"
        ${isHomeFeed ? `data-open-detail="${item.id}" role="button" tabindex="0" aria-label="查看 ${escapeHtml(item.title)} 详情"` : ""}
      >
        <div class="goods-cover">
          <img class="goods-thumb" src="${escapeHtml(item.imageUrl || "./icon.svg")}" alt="商品图" />
          ${isHomeFeed ? '<span class="card-enter-hint" aria-hidden="true">↗</span>' : ""}
          <span class="deal-badge">校内验真</span>
          <span class="status ${item.status}">${statusLabel(item.status)}</span>
        </div>
        <div class="goods-top">
          <header>
            <div>
              <h3>${escapeHtml(item.title)}</h3>
              <p class="meta">${escapeHtml(item.category)} · ${escapeHtml(item.tradeType)} · 卖家 ${escapeHtml(item.sellerName)}</p>
              <p class="goods-stats">发布 ${formatRelativeTime(item.createdAt)} · 浏览 ${Number(item.views || 0)} · 信用 ${getCredit(item.sellerId)}</p>
            </div>
            <span class="price"><small>¥</small>${item.price}</span>
          </header>
          <p class="goods-desc">${escapeHtml(item.desc || "这件闲置暂未填写描述")}</p>
        </div>
        <div class="item-actions">
          ${isHomeFeed ? "" : `<button class="action-btn" data-action="detail" data-id="${item.id}">查看详情</button>`}
          ${
            isHomeFeed
              ? ""
              : `<button class="action-btn" data-action="contact" data-id="${item.id}" ${
                  item.status !== "sale" || item.sellerId === state.currentUser.id || isBlockedCurrentUser() ? "disabled" : ""
                }>发起交易</button>`
          }
          <button class="fav-btn ${favs.has(item.id) ? "active" : ""}" data-action="fav" data-id="${item.id}">${
        favs.has(item.id) ? (isHomeFeed ? "已藏" : "已收藏") : "收藏"
      }</button>
          ${
            item.sellerId !== state.currentUser.id
              ? `<button class="action-btn" data-action="report" data-id="${item.id}" ${item.status === "blocked" || isBlockedCurrentUser() ? "disabled" : ""}>${isHomeFeed ? "反馈" : "举报"}</button>`
              : ""
          }
          ${
            item.sellerId === state.currentUser.id
              ? `<button class="action-btn" data-action="toggleSold" data-id="${item.id}" ${isBlockedCurrentUser() ? "disabled" : ""}>${
                  item.status === "sale" ? (isHomeFeed ? "已售" : "标记已售") : (isHomeFeed ? "上架" : "重新上架")
                }</button>`
              : ""
          }
        </div>
      </article>
    `,
    )
    .join("");

  listEl.querySelectorAll("button[data-action]").forEach((btn) => {
    btn.addEventListener("click", () => handleGoodsAction(btn.dataset.action, btn.dataset.id));
  });
  listEl.querySelectorAll("[data-open-detail]").forEach((cardEl) => {
    cardEl.addEventListener("click", (event) => {
      const target = event.target;
      if (target instanceof Element && target.closest("button")) return;
      handleGoodsAction("detail", cardEl.dataset.openDetail);
    });
    cardEl.addEventListener("keydown", (event) => {
      if (event.key !== "Enter" && event.key !== " ") return;
      event.preventDefault();
      handleGoodsAction("detail", cardEl.dataset.openDetail);
    });
  });
}

function createReport(item) {
  if (!ensureActiveUser("举报商品")) return;
  openReportComposer(item);
}

function handleGoodsAction(action, id) {
  const item = state.data.goods.find((g) => g.id === id);
  if (!item) return;

  if (action === "detail") {
    openDetailScreen(item.id);
    return;
  }

  if (action === "fav") {
    toggleFavorite(item.id);
    return;
  }

  if (action === "report") {
    createReport(item);
    return;
  }

  if (action === "contact") {
    if (!ensureActiveUser("发起交易")) return;
    const intentId = `i${Date.now()}`;
    const intent = {
      id: intentId,
      itemId: item.id,
      itemTitle: item.title,
      buyerId: state.currentUser.id,
      buyerName: state.currentUser.name,
      sellerId: item.sellerId,
      sellerName: item.sellerName,
      status: "pending",
      rated: false,
      updatedAt: new Date().toISOString(),
    };

    const msg = {
      id: `m${Date.now()}${Math.floor(Math.random() * 1000)}`,
      intentId,
      itemId: item.id,
      from: state.currentUser.name,
      fromId: state.currentUser.id,
      toId: item.sellerId,
      item: item.title,
      text: "我想买这个商品，可以约时间交易吗？",
      createdAt: new Date().toISOString(),
      time: "刚刚",
    };

    state.data.intents.unshift(intent);
    state.data.messages.unshift(msg);
    state.intentFocusId = intent.id;
    void persistMutation(() =>
      apiRequest("/api/intents", {
        method: "POST",
        body: JSON.stringify({ intent, message: msg }),
      }),
    );
    state.tab = "messages";
    setActiveTab();
    render();
    return;
  }

  if (action === "toggleSold") {
    if (!ensureActiveUser("更新商品状态")) return;
    item.status = item.status === "sale" ? "sold" : "sale";
    void persistMutation(() =>
      apiRequest(`/api/goods/${encodeURIComponent(item.id)}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status: item.status }),
      }),
    );
    render();
  }
}

function handleIntentAction(action, intentId) {
  if (!ensureActiveUser("处理交易")) return;
  const intent = state.data.intents.find((it) => it.id === intentId);
  if (!intent) return;

  if (action === "accept") intent.status = "accepted";
  if (action === "reject") intent.status = "rejected";
  if (action === "complete") {
    intent.status = "completed";
    const goods = state.data.goods.find((g) => g.id === intent.itemId);
    if (goods) goods.status = "sold";
  }

  if (action === "rate") {
    if (intent.rated) return;
    openRatingComposer(intent);
    return;
  }

  intent.updatedAt = new Date().toISOString();
  let notifyText = "";
  if (action === "accept") notifyText = "我已同意交易意向，我们可以继续沟通时间地点。";
  if (action === "reject") notifyText = "抱歉，我暂时无法成交这件商品。";
  if (action === "complete") notifyText = "我已确认交易完成，感谢配合。";
  let statusNotice = null;
  if (notifyText) {
    const toId = state.currentUser.id === intent.buyerId ? intent.sellerId : intent.buyerId;
    statusNotice = buildIntentMessage(intent, notifyText, toId);
    state.data.messages.unshift(statusNotice);
  }

  void persistMutation(async () => {
    await apiRequest(`/api/intents/${encodeURIComponent(intent.id)}`, {
      method: "PATCH",
      body: JSON.stringify({
        status: intent.status,
        rated: intent.rated,
        updatedAt: intent.updatedAt,
      }),
    });
    if (statusNotice) {
      await apiRequest("/api/messages", {
        method: "POST",
        body: JSON.stringify({ message: statusNotice }),
      });
    }
  });
  renderMessages();
}

function buildDetailMarkup(item) {
  item.views += 1;
  const favs = getFavSet();
  const isFav = favs.has(item.id);
  const relatedGoods = state.data.goods
    .filter(
      (g) =>
        g.id !== item.id &&
        g.category === item.category &&
        g.status === "sale" &&
        (isAdmin() || g.status !== "blocked"),
    )
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 3);

  void persistMutation(() =>
    apiRequest(`/api/goods/${encodeURIComponent(item.id)}/views`, {
      method: "PATCH",
      body: JSON.stringify({ delta: 1 }),
    }),
  );

  return `
    <section class="card detail-screen">
      <div class="detail-header">
        <button class="ghost-btn detail-back-btn" id="detailBackBtn">返回</button>
        <span class="chip">商品详情</span>
      </div>
      <div class="detail-hero">
        <img class="detail-hero-image" src="${escapeHtml(item.imageUrl || "./icon.svg")}" alt="${escapeHtml(item.title)}" />
        <span class="status ${item.status} detail-status">${statusLabel(item.status)}</span>
      </div>
      <div class="detail-body">
      <h3>${escapeHtml(item.title)}</h3>
      <p class="detail-subline">${escapeHtml(item.category)} · ${escapeHtml(item.tradeType)} · ${formatRelativeTime(item.createdAt)}</p>
      <p>价格：¥${item.price}</p>
      <p>分类：${escapeHtml(item.category)} | 交易方式：${escapeHtml(item.tradeType)}</p>
      <p>描述：${escapeHtml(item.desc)}</p>
      <p>发布时间：${formatDate(item.createdAt)}</p>
      <p>信用参考：卖家信用分 ${getCredit(item.sellerId)}，浏览 ${item.views}</p>
      ${
        relatedGoods.length
          ? `<section class="related-section">
              <h4>同类推荐</h4>
              <div class="related-list">
                ${relatedGoods
                  .map(
                    (g) => `
                    <button class="related-item" data-related-id="${g.id}">
                      <strong>${escapeHtml(g.title)}</strong>
                      <span>¥${g.price} · ${formatRelativeTime(g.createdAt)}</span>
                    </button>
                  `,
                  )
                  .join("")}
              </div>
            </section>`
          : ""
      }
      <div class="detail-actions">
        <button class="primary-btn" id="detailContactBtn" ${
          item.status !== "sale" || item.sellerId === state.currentUser?.id || isBlockedCurrentUser() ? "disabled" : ""
        }>发起交易</button>
        <button class="fav-btn ${isFav ? "active" : ""}" id="detailFavBtn">${isFav ? "取消收藏" : "收藏商品"}</button>
        <button class="action-btn" id="detailReportBtn" ${item.sellerId === state.currentUser?.id || isBlockedCurrentUser() ? "disabled" : ""}>举报商品</button>
      </div>
      </div>
    </section>
  `;
}

function bindDetailActions(item) {
  document.getElementById("detailBackBtn").onclick = () => closeDetailScreen();
  document.getElementById("detailContactBtn").onclick = () => {
    handleGoodsAction("contact", item.id);
  };
  document.getElementById("detailFavBtn").onclick = () => {
    handleGoodsAction("fav", item.id);
    renderDetailScreen();
  };
  document.getElementById("detailReportBtn").onclick = () => {
    createReport(item);
  };
  appEl.querySelectorAll("button[data-related-id]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const nextItem = state.data.goods.find((g) => g.id === btn.dataset.relatedId);
      if (!nextItem) return;
      openDetailScreen(nextItem.id, state.previousTab || "home");
    });
  });
}

function renderDetailScreen() {
  const item = state.data.goods.find((g) => g.id === state.detailItemId);
  if (!item) {
    closeDetailScreen();
    return;
  }
  appEl.innerHTML = buildDetailMarkup(item);
  bindDetailActions(item);
}

function renderPublish() {
  if (!ensureActiveUser("发布商品")) {
    state.tab = "home";
    setActiveTab();
    renderHome();
    return;
  }
  const tpl = document.getElementById("publishTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const form = document.getElementById("publishForm");
  const titleInput = form.elements.namedItem("title");
  const priceInput = form.elements.namedItem("price");
  const descInput = form.elements.namedItem("desc");
  const imageUrlInput = form.elements.namedItem("imageUrl");
  const imageFileInput = form.elements.namedItem("imageFile");
  const submitBtn = document.getElementById("publishSubmitBtn");
  const formErrorEl = document.getElementById("publishFormError");
  const previewWrapEl = document.getElementById("publishImagePreview");
  const previewImgEl = document.getElementById("publishPreviewImg");
  const previewTextEl = document.getElementById("publishPreviewText");
  const priceHintEl = form.querySelector('[data-hint="price"]');
  const descHintEl = form.querySelector('[data-hint="desc"]');

  let localImageDataUrl = "";

  const setFieldHint = (el, text = "") => {
    if (!el) return;
    el.textContent = text;
    el.classList.toggle("error", Boolean(text));
  };

  const setFormError = (text = "") => {
    formErrorEl.textContent = text;
    formErrorEl.classList.toggle("hidden", !text);
  };

  const setPreview = (src, text) => {
    previewImgEl.src = src || "./icon.svg";
    previewTextEl.textContent = text || "未选择图片";
    previewWrapEl.classList.toggle("hidden", !src);
  };

  const validateFields = () => {
    const title = String(titleInput.value || "").trim();
    const price = Number(priceInput.value || 0);
    const desc = String(descInput.value || "").trim();
    const imageUrl = String(imageUrlInput.value || "").trim();

    let ok = true;
    setFieldHint(priceHintEl, "");
    setFieldHint(descHintEl, "");
    setFormError("");

    if (title.length < 2) {
      ok = false;
      setFormError("标题至少 2 个字。");
    }

    if (!Number.isFinite(price) || price < 1) {
      ok = false;
      setFieldHint(priceHintEl, "价格至少为 1 元");
    } else if (price > 99999) {
      ok = false;
      setFieldHint(priceHintEl, "价格不能超过 99999 元");
    }

    if (desc && desc.length < 5) {
      ok = false;
      setFieldHint(descHintEl, "描述建议至少 5 个字，便于买家判断");
    }

    if (!localImageDataUrl && imageUrl) {
      try {
        new URL(imageUrl);
      } catch {
        ok = false;
        setFormError("图片链接格式不正确，请填写完整 URL。");
      }
    }

    return ok;
  };

  imageUrlInput.addEventListener("input", () => {
    if (localImageDataUrl) return;
    const url = String(imageUrlInput.value || "").trim();
    if (!url) {
      setPreview("", "");
      return;
    }
    setPreview(url, "来自图片链接预览");
  });

  imageFileInput.addEventListener("change", () => {
    const file = imageFileInput.files?.[0];
    if (!file) {
      localImageDataUrl = "";
      const url = String(imageUrlInput.value || "").trim();
      setPreview(url, url ? "来自图片链接预览" : "");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      imageFileInput.value = "";
      localImageDataUrl = "";
      setFormError("图片大小请控制在 2MB 以内。");
      setPreview("", "");
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      localImageDataUrl = String(reader.result || "");
      setPreview(localImageDataUrl, `本地图片：${file.name}`);
      setFormError("");
    };
    reader.onerror = () => {
      imageFileInput.value = "";
      localImageDataUrl = "";
      setFormError("读取本地图片失败，请重试。");
      setPreview("", "");
    };
    reader.readAsDataURL(file);
  });

  priceInput.addEventListener("input", () => void validateFields());
  descInput.addEventListener("input", () => void validateFields());

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    if (!validateFields()) return;
    submitBtn.disabled = true;
    const formData = new FormData(form);
    const urlImage = String(formData.get("imageUrl") || "").trim();
    const finalImage = localImageDataUrl || urlImage;

    const newItem = {
      id: `g${Date.now()}`,
      title: String(formData.get("title") || "").trim(),
      category: String(formData.get("category") || "教材"),
      price: Number(formData.get("price") || 0),
      imageUrl: finalImage,
      desc: String(formData.get("desc") || "").trim(),
      sellerId: state.currentUser.id,
      sellerName: state.currentUser.name,
      tradeType: String(formData.get("tradeType") || "当面交易"),
      status: "sale",
      createdAt: new Date().toISOString(),
      views: 0,
    };

    state.data.goods.unshift(newItem);
    void persistMutation(() =>
      apiRequest("/api/goods", {
        method: "POST",
        body: JSON.stringify(newItem),
      }),
    );
    form.reset();
    localImageDataUrl = "";
    setPreview("", "");
    submitBtn.disabled = false;
    showToast("发布成功，已在首页展示。");
    state.tab = "home";
    setActiveTab();
    render();
  });
}

function renderMessages() {
  const tpl = document.getElementById("messagesTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const intentListEl = document.getElementById("intentList");
  const summaryEl = document.getElementById("intentStatusSummary");
  const filterBtns = document.querySelectorAll("button[data-intent-filter]");
  const messageSearchInput = document.getElementById("messageSearchInput");
  const myIntents = state.data.intents
    .filter(
      (it) => it.buyerId === state.currentUser.id || it.sellerId === state.currentUser.id,
    )
    .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));

  const statusCount = {
    all: myIntents.length,
    pending: myIntents.filter((it) => it.status === "pending").length,
    accepted: myIntents.filter((it) => it.status === "accepted").length,
    completed: myIntents.filter((it) => it.status === "completed").length,
    rejected: myIntents.filter((it) => it.status === "rejected").length,
  };
  summaryEl.innerHTML = `
    <span class="chip">总计 ${statusCount.all}</span>
    <span class="chip">待确认 ${statusCount.pending}</span>
    <span class="chip">待交易 ${statusCount.accepted}</span>
    <span class="chip">已完成 ${statusCount.completed}</span>
  `;

  filterBtns.forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.intentFilter === state.intentFilter);
    btn.addEventListener("click", () => {
      state.intentFilter = btn.dataset.intentFilter || "all";
      renderMessages();
    });
  });

  const intents = myIntents.filter((it) =>
    state.intentFilter === "all" ? true : it.status === state.intentFilter,
  );

  if (!myIntents.length) {
    state.intentFocusId = "";
    intentListEl.innerHTML = "<p class=\"muted\">暂无交易意向，去首页找找好物吧。</p>";
  } else if (!intents.length) {
    state.intentFocusId = "";
    intentListEl.innerHTML = "<p class=\"muted\">当前筛选下暂无交易记录。</p>";
  } else {
    const focusedIdInList = intents.some((it) => it.id === state.intentFocusId);
    if (!focusedIdInList) state.intentFocusId = intents[0].id;
    const activeIntent = intents.find((it) => it.id === state.intentFocusId) || intents[0];
    const activeMessages = getIntentThreadMessages(activeIntent);
    const isSeller = activeIntent.sellerId === state.currentUser.id;

    const actions = [];
    if (isSeller && activeIntent.status === "pending") {
      actions.push(`<button class=\"action-btn\" data-ia=\"accept\" data-id=\"${activeIntent.id}\">同意</button>`);
      actions.push(`<button class=\"action-btn\" data-ia=\"reject\" data-id=\"${activeIntent.id}\">拒绝</button>`);
    }
    if (!isSeller && activeIntent.status === "accepted") {
      actions.push(`<button class=\"action-btn\" data-ia=\"complete\" data-id=\"${activeIntent.id}\">确认完成</button>`);
    }
    if (!isSeller && activeIntent.status === "completed" && !activeIntent.rated) {
      actions.push(`<button class=\"action-btn\" data-ia=\"rate\" data-id=\"${activeIntent.id}\">评价卖家</button>`);
    }

    intentListEl.innerHTML = `
      <article class="msg-item intent-board">
        <div class="intent-convo-list">
          ${intents
            .map((it) => {
              const who = it.sellerId === state.currentUser.id ? `买家 ${it.buyerName}` : `卖家 ${it.sellerName}`;
              return `
                <button class="intent-convo ${it.id === activeIntent.id ? "active" : ""}" data-ifocus="${it.id}">
                  <strong>${escapeHtml(it.itemTitle || "未命名商品")}</strong>
                  <p>${escapeHtml(who)} · ${intentStatusText(it.status)}</p>
                  <span>${formatDate(it.updatedAt)}</span>
                </button>
              `;
            })
            .join("")}
        </div>
        <div class="intent-chat-panel">
          <header class="intent-chat-head">
            <div>
              <strong>${escapeHtml(activeIntent.itemTitle || "未命名商品")}</strong>
              <p>${isSeller ? `买家：${escapeHtml(activeIntent.buyerName)}` : `卖家：${escapeHtml(activeIntent.sellerName)}`}</p>
            </div>
            <span class="chip">${intentStatusText(activeIntent.status)}</span>
          </header>
          ${renderIntentTimeline(activeIntent.status)}
          <div class="chat-thread">
            ${
              activeMessages.length
                ? activeMessages
                    .slice(-16)
                    .map((msg) => {
                      const mine = msg.fromId === state.currentUser.id;
                      return `
                        <div class="chat-bubble ${mine ? "mine" : "theirs"}">
                          <p class="chat-meta">${escapeHtml(getMessageSenderName(msg))} · ${formatDate(msg.createdAt || msg.time || activeIntent.updatedAt)}</p>
                          <p>${escapeHtml(msg.text || "")}</p>
                        </div>
                      `;
                    })
                    .join("")
                : '<p class="muted">还没有聊天记录，发一条消息开始沟通吧。</p>'
            }
          </div>
          <form class="chat-form" data-chat-form="${activeIntent.id}">
            <input
              type="text"
              maxlength="200"
              name="chatText"
              placeholder="${isSeller ? "给买家发送消息..." : "给卖家发送消息..."}"
            />
            <button type="submit" class="action-btn">发送</button>
          </form>
          <div class="item-actions">${actions.join("")}</div>
        </div>
      </article>
    `;

    intentListEl.querySelectorAll("button[data-ifocus]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.intentFocusId = btn.dataset.ifocus || "";
        renderMessages();
      });
    });
    intentListEl.querySelectorAll("button[data-ia]").forEach((btn) => {
      btn.addEventListener("click", () => handleIntentAction(btn.dataset.ia, btn.dataset.id));
    });
    intentListEl.querySelectorAll("form[data-chat-form]").forEach((formEl) => {
      formEl.addEventListener("submit", (event) => {
        event.preventDefault();
        const intentId = formEl.dataset.chatForm;
        const input = formEl.querySelector('input[name="chatText"]');
        if (!input) return;
        sendIntentMessage(intentId, input.value);
      });
    });
  }

  const listEl = document.getElementById("messagesList");
  const unreadChipEl = document.getElementById("messageUnreadChip");
  const markAllReadBtn = document.getElementById("markAllReadBtn");
  const messages = state.data.messages
    .filter(
      (msg) => isMessageRelatedToCurrentUser(msg),
    )
    .sort((a, b) => getMessageTimestamp(b) - getMessageTimestamp(a));
  const readSet = getReadSetForCurrentUser();
  const unreadMessages = messages.filter((msg) => msg.toId === state.currentUser.id && !readSet.has(msg.id));
  unreadChipEl.textContent = `未读 ${unreadMessages.length}`;
  markAllReadBtn.disabled = unreadMessages.length === 0;
  markAllReadBtn.addEventListener("click", () => {
    markMessagesRead(unreadMessages.map((msg) => msg.id));
    renderMessages();
  });

  const keyword = state.messageSearch.trim().toLowerCase();
  messageSearchInput.value = state.messageSearch;
  messageSearchInput.addEventListener("input", () => {
    state.messageSearch = messageSearchInput.value.trim();
    renderMessages();
  });
  const matchedMessages = messages.filter((msg) => {
    if (!keyword) return true;
    const haystack =
      `${msg.from || ""} ${getMessageSenderName(msg) || ""} ${msg.item || ""} ${msg.text || ""}`.toLowerCase();
    return haystack.includes(keyword);
  });

  if (!messages.length) {
    state.messageFocusKey = "";
    listEl.innerHTML = "<p class=\"muted\">暂无消息</p>";
    return;
  }
  if (!matchedMessages.length) {
    state.messageFocusKey = "";
    listEl.innerHTML = "<p class=\"muted\">没有匹配的消息，换个关键词试试。</p>";
    return;
  }

  const matchedThreadKeys = new Set(matchedMessages.map((msg) => getMessageThreadKey(msg)));
  const sourceMessages = keyword
    ? messages.filter((msg) => matchedThreadKeys.has(getMessageThreadKey(msg)))
    : messages;

  const threadsMap = new Map();
  sourceMessages.forEach((msg) => {
    const key = getMessageThreadKey(msg);
    if (!threadsMap.has(key)) {
      threadsMap.set(key, {
        key,
        latestAt: msg.createdAt || msg.time || new Date().toISOString(),
        messages: [],
      });
    }
    const thread = threadsMap.get(key);
    thread.messages.push(msg);
    const msgAt = getMessageTimestamp(msg);
    if (msgAt > getMessageTimestamp({ createdAt: thread.latestAt })) {
      thread.latestAt = msg.createdAt || msg.time || thread.latestAt;
    }
  });

  const threads = Array.from(threadsMap.values())
    .map((thread) => {
      const sorted = thread.messages.sort((a, b) => getMessageTimestamp(a) - getMessageTimestamp(b));
      const last = sorted[sorted.length - 1];
      const intentIdFromKey = thread.key.startsWith("intent:") ? thread.key.slice("intent:".length) : "";
      const intent = intentIdFromKey ? state.data.intents.find((it) => it.id === intentIdFromKey) : null;
      const peerId = last.fromId === state.currentUser.id ? last.toId : last.fromId;
      const peerName =
        last.from === "系统通知" || last.fromId === "system"
          ? "系统通知"
          : getDisplayNameByUserId(peerId) || getMessageSenderName(last) || "站内消息";
      const itemTitle =
        intent?.itemTitle ||
        [...sorted].reverse().map((msg) => String(msg.item || "").trim()).find(Boolean) ||
        "";
      const displayTitle = itemTitle ? `${peerName} · ${itemTitle}` : peerName;
      const unread = sorted.filter((msg) => msg.toId === state.currentUser.id && !readSet.has(msg.id)).length;
      return {
        ...thread,
        messages: sorted,
        last,
        peerName,
        displayTitle,
        unread,
        title: itemTitle || "站内消息",
      };
    })
    .sort((a, b) => getMessageTimestamp({ createdAt: b.latestAt }) - getMessageTimestamp({ createdAt: a.latestAt }));

  if (!threads.some((it) => it.key === state.messageFocusKey)) {
    state.messageFocusKey = threads[0].key;
  }
  const activeThread = threads.find((it) => it.key === state.messageFocusKey) || threads[0];

  listEl.innerHTML = `
    <article class="msg-item message-board">
      <div class="message-convo-list">
        ${threads
          .map(
            (thread) => `
            <button class="message-convo ${thread.key === activeThread.key ? "active" : ""}" data-mfocus="${escapeHtml(thread.key)}">
              <strong>${escapeHtml(thread.displayTitle)}</strong>
              <p>${escapeHtml(thread.last.text || "")}</p>
              <span>${formatDate(thread.last.createdAt || thread.last.time || new Date().toISOString())} ${
                thread.unread ? `· 未读 ${thread.unread}` : ""
              }</span>
            </button>
          `,
          )
          .join("")}
      </div>
      <div class="message-chat-panel">
        <header class="message-chat-head">
          <div>
            <strong>${escapeHtml(activeThread.displayTitle)}</strong>
            <p>${escapeHtml(activeThread.title)}</p>
          </div>
          <span class="chip">${activeThread.unread ? `未读 ${activeThread.unread}` : "已读"}</span>
        </header>
        <div class="chat-thread">
          ${activeThread.messages
            .slice(-20)
            .map((msg) => {
              const mine = msg.fromId === state.currentUser.id || msg.from === state.currentUser.name;
              const unread = msg.toId === state.currentUser.id && !readSet.has(msg.id);
              return `
                <div class="chat-bubble ${mine ? "mine" : "theirs"}">
                  <p class="chat-meta">${escapeHtml(getMessageSenderName(msg))} · ${formatDate(msg.createdAt || msg.time || new Date().toISOString())}</p>
                  <p>${escapeHtml(msg.text || "")}</p>
                  ${unread ? `<div class="item-actions"><button class="action-btn" data-mark-read="${msg.id}">标为已读</button></div>` : ""}
                </div>
              `;
            })
            .join("")}
        </div>
      </div>
    </article>
  `;

  listEl.querySelectorAll("button[data-mfocus]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.messageFocusKey = btn.dataset.mfocus || "";
      renderMessages();
    });
  });

  listEl.querySelectorAll("button[data-mark-read]").forEach((btn) => {
    btn.addEventListener("click", () => {
      markMessagesRead([btn.dataset.markRead]);
      renderMessages();
    });
  });
}

function renderProfile() {
  const tpl = document.getElementById("profileTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const roleText = isAdmin() ? "管理员" : "学生";
  const statusText = isBlockedCurrentUser() ? "已封禁" : "正常";
  document.getElementById("profileName").textContent = `${state.currentUser.name}（${roleText}）`;
  document.getElementById("studentIdText").textContent = `学号认证：${state.currentUser.studentId}（${statusText}）`;
  document.getElementById("creditScore").textContent = `${getCredit(state.currentUser.id)}`;

  const mine = state.data.goods.filter((g) => g.sellerId === state.currentUser.id);
  const soldCount = mine.filter((g) => g.status === "sold").length;
  document.getElementById("soldCountChip").textContent = `累计成交 ${soldCount}`;

  const userRatings = state.data.ratings.filter((r) => r.toId === state.currentUser.id);
  const avg = userRatings.length
    ? (userRatings.reduce((sum, r) => sum + Number(r.score), 0) / userRatings.length).toFixed(1)
    : "5.0";
  document.getElementById("ratingChip").textContent = `平均评分 ${avg}`;

  const listEl = document.getElementById("myGoodsList");
  if (!mine.length) {
    listEl.innerHTML = "<p class=\"muted\">还没有发布商品，去发布一个吧。</p>";
  } else {
    listEl.innerHTML = mine
      .map(
        (item) => `
        <article class="goods-item">
          <header>
            <h3>${escapeHtml(item.title)}</h3>
            <span class="price">¥${item.price}</span>
          </header>
          <p class="meta">${escapeHtml(item.category)} · ${escapeHtml(item.tradeType)}</p>
          <span class="status ${item.status}">${statusLabel(item.status)}</span>
          <div class="item-actions">
            <button class="action-btn" data-action="detail" data-id="${item.id}">详情</button>
            <button class="action-btn" data-action="toggleSold" data-id="${item.id}" ${item.status === "blocked" || isBlockedCurrentUser() ? "disabled" : ""}>${
          item.status === "sale" ? "标记已售" : "重新上架"
        }</button>
          </div>
        </article>
      `,
      )
      .join("");

    listEl.querySelectorAll("button[data-action]").forEach((btn) => {
      btn.addEventListener("click", () => handleGoodsAction(btn.dataset.action, btn.dataset.id));
    });
  }

  const favoriteListEl = document.getElementById("favoriteGoodsList");
  const favoriteIds = new Set(state.data.favorites[state.currentUser.id] || []);
  const favoriteGoods = state.data.goods.filter((item) => favoriteIds.has(item.id));
  if (!favoriteGoods.length) {
    favoriteListEl.innerHTML = "<p class=\"muted\">你还没有收藏商品。</p>";
  } else {
    favoriteListEl.innerHTML = favoriteGoods
      .map(
        (item) => `
        <article class="goods-item">
          <header>
            <h3>${escapeHtml(item.title)}</h3>
            <span class="price">¥${item.price}</span>
          </header>
          <p class="meta">${escapeHtml(item.category)} · 卖家 ${escapeHtml(item.sellerName)}</p>
          <span class="status ${item.status}">${statusLabel(item.status)}</span>
          <div class="item-actions">
            <button class="action-btn" data-action="detail" data-id="${item.id}">详情</button>
            <button class="fav-btn active" data-action="fav" data-id="${item.id}">取消收藏</button>
            <button class="action-btn" data-action="contact" data-id="${item.id}" ${
          item.status !== "sale" || item.sellerId === state.currentUser.id || isBlockedCurrentUser() ? "disabled" : ""
        }>发起交易</button>
          </div>
        </article>
      `,
      )
      .join("");
    favoriteListEl.querySelectorAll("button[data-action]").forEach((btn) => {
      btn.addEventListener("click", () => handleGoodsAction(btn.dataset.action, btn.dataset.id));
    });
  }

  document.getElementById("logoutBtn").addEventListener("click", () => {
    showConfirmOverlay({
      title: "退出当前账号？",
      description: "退出后会回到登录入口，本地浏览和演示数据仍会保留。",
      confirmText: "退出",
      tone: "danger",
      onConfirm: () => {
        state.currentUser = null;
        state.authToken = "";
        resetUser();
        resetToken();
        state.tab = "home";
        render();
      },
    });
  });
}

function handleReportAction(action, reportId) {
  const report = state.data.reports.find((r) => r.id === reportId);
  if (!report) return;

  if (action === "block") {
    report.status = "resolved";
    report.updatedAt = new Date().toISOString();
    const goods = state.data.goods.find((g) => g.id === report.goodsId);
    if (goods) goods.status = "blocked";
    state.data.auditLogs.unshift({
      id: `al${Date.now()}`,
      actorId: state.currentUser.id,
      actorName: state.currentUser.name,
      action: "report_review",
      targetType: "report",
      targetId: report.id,
      detail: "status=resolved, action=block_goods",
      createdAt: new Date().toISOString(),
    });

    void persistMutation(() =>
      apiRequest(`/api/reports/${encodeURIComponent(report.id)}`, {
        method: "PATCH",
        body: JSON.stringify({
          status: "resolved",
          reviewedBy: state.currentUser.id,
          reviewNote: "违规下架",
          action: "block_goods",
        }),
      }),
    );
  }

  if (action === "reject") {
    report.status = "dismissed";
    report.updatedAt = new Date().toISOString();
    state.data.auditLogs.unshift({
      id: `al${Date.now()}`,
      actorId: state.currentUser.id,
      actorName: state.currentUser.name,
      action: "report_review",
      targetType: "report",
      targetId: report.id,
      detail: "status=dismissed, action=ignore",
      createdAt: new Date().toISOString(),
    });

    void persistMutation(() =>
      apiRequest(`/api/reports/${encodeURIComponent(report.id)}`, {
        method: "PATCH",
        body: JSON.stringify({
          status: "dismissed",
          reviewedBy: state.currentUser.id,
          reviewNote: "证据不足",
          action: "ignore",
        }),
      }),
    );
  }

  renderAdmin();
}

function handleUserAction(action, userId) {
  const target = state.data.users[userId];
  if (!target || target.role === "admin") return;

  const nextStatus = action === "blockUser" ? "blocked" : "active";
  target.status = nextStatus;
  state.data.auditLogs.unshift({
    id: `al${Date.now()}`,
    actorId: state.currentUser.id,
    actorName: state.currentUser.name,
    action: nextStatus === "blocked" ? "user_block" : "user_unblock",
    targetType: "user",
    targetId: userId,
    detail: `set status=${nextStatus}`,
    createdAt: new Date().toISOString(),
  });
  if (state.currentUser && state.currentUser.id === userId) {
    state.currentUser.status = nextStatus;
    saveUser();
  }

  void persistMutation(() =>
    apiRequest(`/api/users/${encodeURIComponent(userId)}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status: nextStatus }),
    }),
  );
  renderAdmin();
}

function handleAdminGoodsSelection(goodsId, checked) {
  if (checked) state.adminSelectedGoods.add(goodsId);
  else state.adminSelectedGoods.delete(goodsId);
}

function handleAdminBatchGoods(action) {
  const ids = Array.from(state.adminSelectedGoods);
  if (!ids.length) {
    showToast("请先选择商品。");
    return;
  }

  const nextStatus = action === "block" ? "blocked" : "sale";
  for (const id of ids) {
    const goods = state.data.goods.find((item) => item.id === id);
    if (goods) goods.status = nextStatus;
  }

  state.adminSelectedGoods.clear();
  state.data.auditLogs.unshift({
    id: `al${Date.now()}`,
    actorId: state.currentUser.id,
    actorName: state.currentUser.name,
    action: "goods_status_batch_update",
    targetType: "goods_batch",
    targetId: ids.join(","),
    detail: `count=${ids.length}, status=${nextStatus}`,
    createdAt: new Date().toISOString(),
  });
  void persistMutation(() =>
    apiRequest("/api/goods/status-batch", {
      method: "PATCH",
      body: JSON.stringify({ ids, status: nextStatus }),
    }),
  );
  renderAdmin();
}

function renderAdmin() {
  const tpl = document.getElementById("adminTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const pendingCount = state.data.reports.filter((r) => r.status === "pending").length;
  const blockedCount = state.data.goods.filter((g) => g.status === "blocked").length;
  document.getElementById("pendingReportChip").textContent = `待审核举报 ${pendingCount}`;
  document.getElementById("blockedGoodsChip").textContent = `已下架商品 ${blockedCount}`;

  const reportListEl = document.getElementById("reportList");
  const reports = [...state.data.reports].sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));

  if (!reports.length) {
    reportListEl.innerHTML = "<p class=\"muted\">暂无举报记录。</p>";
  } else {
    reportListEl.innerHTML = reports
      .map((rp) => {
        const statusText =
          rp.status === "pending" ? "待审核" : rp.status === "resolved" ? "已处理" : "已驳回";

        const actions =
          rp.status === "pending"
            ? `<button class=\"action-btn\" data-ra=\"block\" data-id=\"${rp.id}\">下架商品</button>
               <button class=\"action-btn\" data-ra=\"reject\" data-id=\"${rp.id}\">驳回举报</button>`
            : "";

        return `
          <article class="msg-item">
            <strong>${escapeHtml(rp.goodsTitle)}</strong>
            <p>举报人：${escapeHtml(rp.reporterName)} · 状态：${statusText}</p>
            <p>原因：${escapeHtml(rp.reason)}</p>
            <p>${formatDate(rp.updatedAt)}</p>
            <div class="item-actions">${actions}</div>
          </article>
        `;
      })
      .join("");

    reportListEl.querySelectorAll("button[data-ra]").forEach((btn) => {
      btn.addEventListener("click", () => handleReportAction(btn.dataset.ra, btn.dataset.id));
    });
  }

  const userListEl = document.getElementById("userList");
  const users = Object.values(state.data.users).sort((a, b) => {
    if (a.role === "admin" && b.role !== "admin") return -1;
    if (a.role !== "admin" && b.role === "admin") return 1;
    return String(a.studentId).localeCompare(String(b.studentId));
  });

  if (!users.length) {
    userListEl.innerHTML = "<p class=\"muted\">暂无用户数据。</p>";
  } else {
    userListEl.innerHTML = users
      .map((user) => {
        const roleText = user.role === "admin" ? "管理员" : "学生";
        const statusText = user.status === "blocked" ? "已封禁" : "正常";
        const actions =
          user.role === "admin"
            ? ""
            : user.status === "blocked"
              ? `<button class=\"action-btn\" data-ua=\"unblockUser\" data-id=\"${user.id}\">解封</button>`
              : `<button class=\"action-btn\" data-ua=\"blockUser\" data-id=\"${user.id}\">封禁</button>`;
        return `
          <article class="msg-item">
            <strong>${escapeHtml(user.name || user.studentId)}</strong>
            <p>学号：${escapeHtml(user.studentId)} · ${roleText}</p>
            <p>状态：${statusText}</p>
            <div class="item-actions">${actions}</div>
          </article>
        `;
      })
      .join("");

    userListEl.querySelectorAll("button[data-ua]").forEach((btn) => {
      btn.addEventListener("click", () => handleUserAction(btn.dataset.ua, btn.dataset.id));
    });
  }

  const goodsKeywordEl = document.getElementById("adminGoodsKeyword");
  const goodsStatusEl = document.getElementById("adminGoodsStatusFilter");
  const goodsListEl = document.getElementById("adminGoodsList");
  const selectAllBtn = document.getElementById("adminSelectAllBtn");
  const batchBlockBtn = document.getElementById("adminBatchBlockBtn");
  const batchRestoreBtn = document.getElementById("adminBatchRestoreBtn");

  goodsKeywordEl.value = state.adminGoodsKeyword;
  goodsStatusEl.value = state.adminGoodsStatus;

  const renderAdminGoodsList = () => {
    const goods = getAdminFilteredGoods();
    const visibleIds = new Set(goods.map((g) => g.id));
    state.adminSelectedGoods = new Set(
      Array.from(state.adminSelectedGoods).filter((id) => visibleIds.has(id)),
    );

    if (!goods.length) {
      goodsListEl.innerHTML = "<p class=\"muted\">当前筛选条件下暂无商品。</p>";
      return;
    }

    goodsListEl.innerHTML = goods
      .map((item) => {
        const checked = state.adminSelectedGoods.has(item.id) ? "checked" : "";
        return `
          <article class="msg-item">
            <strong>${escapeHtml(item.title)}</strong>
            <p>卖家：${escapeHtml(item.sellerName)} · 状态：${statusLabel(item.status)} · ¥${item.price}</p>
            <p>${formatDate(item.createdAt)}</p>
            <div class="item-actions">
              <label><input type="checkbox" data-gid="${item.id}" ${checked} /> 选择</label>
              <button class="action-btn" data-ga="block" data-id="${item.id}">下架</button>
              <button class="action-btn" data-ga="restore" data-id="${item.id}">恢复</button>
            </div>
          </article>
        `;
      })
      .join("");

    goodsListEl.querySelectorAll("input[data-gid]").forEach((input) => {
      input.addEventListener("change", () => {
        handleAdminGoodsSelection(input.dataset.gid, input.checked);
      });
    });

    goodsListEl.querySelectorAll("button[data-ga]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const goods = state.data.goods.find((g) => g.id === btn.dataset.id);
        if (!goods) return;
        const nextStatus = btn.dataset.ga === "block" ? "blocked" : "sale";
        goods.status = nextStatus;
        state.data.auditLogs.unshift({
          id: `al${Date.now()}`,
          actorId: state.currentUser.id,
          actorName: state.currentUser.name,
          action: "goods_status_update",
          targetType: "goods",
          targetId: goods.id,
          detail: `set status=${nextStatus}`,
          createdAt: new Date().toISOString(),
        });
        void persistMutation(() =>
          apiRequest(`/api/goods/${encodeURIComponent(goods.id)}/status`, {
            method: "PATCH",
            body: JSON.stringify({ status: nextStatus }),
          }),
        );
        renderAdmin();
      });
    });
  };

  goodsKeywordEl.addEventListener("input", () => {
    state.adminGoodsKeyword = goodsKeywordEl.value;
    renderAdminGoodsList();
  });
  goodsStatusEl.addEventListener("change", () => {
    state.adminGoodsStatus = goodsStatusEl.value;
    renderAdminGoodsList();
  });
  selectAllBtn.addEventListener("click", () => {
    const goods = getAdminFilteredGoods();
    if (!goods.length) return;
    for (const item of goods) state.adminSelectedGoods.add(item.id);
    renderAdminGoodsList();
  });
  batchBlockBtn.addEventListener("click", () => handleAdminBatchGoods("block"));
  batchRestoreBtn.addEventListener("click", () => handleAdminBatchGoods("restore"));

  renderAdminGoodsList();

  const auditLogListEl = document.getElementById("auditLogList");
  const logs = [...state.data.auditLogs]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 20);
  if (!logs.length) {
    auditLogListEl.innerHTML = "<p class=\"muted\">暂无操作日志。</p>";
  } else {
    auditLogListEl.innerHTML = logs
      .map(
        (log) => `
        <article class="msg-item">
          <strong>${escapeHtml(log.action)}</strong>
          <p>操作人：${escapeHtml(log.actorName || log.actorId || "-")}</p>
          <p>对象：${escapeHtml(log.targetType || "-")} · ${escapeHtml(log.targetId || "-")}</p>
          <p>${escapeHtml(log.detail || "")}</p>
          <p>${formatDate(log.createdAt)}</p>
        </article>
      `,
      )
      .join("");
  }
}

function formatDate(iso) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return String(iso || "");
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${d} ${hh}:${mm}`;
}

function formatRelativeTime(iso) {
  const diff = Date.now() - new Date(iso).getTime();
  if (!Number.isFinite(diff) || diff < 0) return "刚刚";
  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`;
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`;
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`;
  return formatDate(iso).slice(0, 10);
}

function escapeHtml(text) {
  return String(text)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

async function init() {
  await bootstrapData();
  if (state.currentUser && state.data.users[state.currentUser.id]) {
    state.currentUser = normalizeUser({
      ...state.currentUser,
      ...state.data.users[state.currentUser.id],
    });
    saveUser();
  }
  render();
}

void init();
