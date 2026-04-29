const STORAGE_KEY = "campus_exchange_data_v2";
const USER_KEY = "campus_exchange_user_v1";
const TOKEN_KEY = "campus_exchange_token_v1";
const ADMIN_STUDENT_IDS = new Set(["99990000"]);

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
  search: "",
  category: "all",
  sort: "latest",
  adminGoodsKeyword: "",
  adminGoodsStatus: "all",
  adminSelectedGoods: new Set(),
  data: normalizeData(structuredClone(seedData)),
  currentUser: loadUser(),
  authToken: loadToken(),
  backendMode: "loading",
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

let deferredInstallPrompt = null;
let saveQueue = Promise.resolve();

tabs.forEach((tabBtn) => {
  tabBtn.addEventListener("click", () => {
    if (
      state.currentUser &&
      isBlockedCurrentUser() &&
      !isAdmin() &&
      (tabBtn.dataset.tab === "publish" || tabBtn.dataset.tab === "messages")
    ) {
      alert("当前账号已被封禁，仅可浏览与查看个人信息。");
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
  alert(`当前账号已被封禁，无法${actionName}。`);
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

async function apiRequest(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  if (state.authToken) headers.Authorization = `Bearer ${state.authToken}`;

  const response = await fetch(path, {
    ...options,
    headers,
  });
  if (!response.ok) throw new Error(`API ${response.status}`);
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function bootstrapData() {
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

function setActiveTab() {
  tabs.forEach((t) => t.classList.toggle("active", t.dataset.tab === state.tab));
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
}

function updateShellState() {
  const loggedIn = Boolean(state.currentUser);
  shell.classList.toggle("auth-mode", !loggedIn);
  quickSellBtn.classList.toggle("hidden", !loggedIn || isBlockedCurrentUser());

  const adminMode = loggedIn && isAdmin();
  adminTabBtn.classList.toggle("hidden", !adminMode);
  tabbar.classList.toggle("admin-mode", adminMode);

  const modeText =
    state.backendMode === "online"
      ? "在线模式"
      : state.backendMode === "offline"
        ? "离线模式"
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
      alert("学号需为8位数字");
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
  const countEl = document.getElementById("resultCount");
  if (countEl) countEl.textContent = `${goods.length} 件`;

  if (!goods.length) {
    listEl.innerHTML = '<div class="card">暂无匹配商品，试试换个关键词。</div>';
    return;
  }

  listEl.innerHTML = goods
    .map(
      (item) => `
      <article class="goods-item">
        <div class="goods-top">
          <img class="goods-thumb" src="${escapeHtml(item.imageUrl || "./icon.svg")}" alt="商品图" />
          <header>
            <div>
              <h3>${escapeHtml(item.title)}</h3>
              <p class="meta">${escapeHtml(item.category)} · ${escapeHtml(item.tradeType)} · 卖家 ${escapeHtml(item.sellerName)}</p>
            </div>
            <span class="price">¥${item.price}</span>
          </header>
        </div>
        <span class="status ${item.status}">${statusLabel(item.status)}</span>
        <div class="item-actions">
          <button class="action-btn" data-action="detail" data-id="${item.id}">查看详情</button>
          <button class="action-btn" data-action="contact" data-id="${item.id}" ${
        item.status !== "sale" || item.sellerId === state.currentUser.id || isBlockedCurrentUser() ? "disabled" : ""
      }>发起交易</button>
          <button class="fav-btn ${favs.has(item.id) ? "active" : ""}" data-action="fav" data-id="${item.id}">${
        favs.has(item.id) ? "已收藏" : "收藏"
      }</button>
          ${
            item.sellerId !== state.currentUser.id
              ? `<button class="action-btn" data-action="report" data-id="${item.id}" ${item.status === "blocked" || isBlockedCurrentUser() ? "disabled" : ""}>举报</button>`
              : ""
          }
          ${
            item.sellerId === state.currentUser.id
              ? `<button class="action-btn" data-action="toggleSold" data-id="${item.id}" ${isBlockedCurrentUser() ? "disabled" : ""}>${
                  item.status === "sale" ? "标记已售" : "重新上架"
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
}

function createReport(item) {
  if (!ensureActiveUser("举报商品")) return;
  const reason = prompt("请输入举报原因", "疑似违规商品");
  if (!reason) return;

  const report = {
    id: `rp${Date.now()}`,
    goodsId: item.id,
    goodsTitle: item.title,
    reporterId: state.currentUser.id,
    reporterName: state.currentUser.name,
    sellerId: item.sellerId,
    reason: reason.trim(),
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
  alert("举报已提交");
}

function handleGoodsAction(action, id) {
  const item = state.data.goods.find((g) => g.id === id);
  if (!item) return;

  if (action === "detail") {
    showDetail(item);
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
    const intent = {
      id: `i${Date.now()}`,
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
      id: `m${Date.now()}`,
      from: state.currentUser.name,
      toId: item.sellerId,
      item: item.title,
      text: "我想买这个商品，可以约时间交易吗？",
      time: "刚刚",
    };

    state.data.intents.unshift(intent);
    state.data.messages.unshift(msg);
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
  let createdRating = null;

  if (action === "accept") intent.status = "accepted";
  if (action === "reject") intent.status = "rejected";
  if (action === "complete") {
    intent.status = "completed";
    const goods = state.data.goods.find((g) => g.id === intent.itemId);
    if (goods) goods.status = "sold";
  }

  if (action === "rate") {
    if (intent.rated) return;
    const score = Number(prompt("给卖家打分(1-5)", "5"));
    if (!score || score < 1 || score > 5) {
      alert("评分需在1-5之间");
      return;
    }
    createdRating = {
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
  }

  intent.updatedAt = new Date().toISOString();
  void persistMutation(() =>
    apiRequest(`/api/intents/${encodeURIComponent(intent.id)}`, {
      method: "PATCH",
      body: JSON.stringify({
        status: intent.status,
        rated: intent.rated,
        updatedAt: intent.updatedAt,
      }),
    }),
  );
  renderMessages();
}

function showDetail(item) {
  item.views += 1;
  void persistMutation(() =>
    apiRequest(`/api/goods/${encodeURIComponent(item.id)}/views`, {
      method: "PATCH",
      body: JSON.stringify({ delta: 1 }),
    }),
  );

  detailDialog.innerHTML = `
    <div class="detail-body">
      <h3>${escapeHtml(item.title)}</h3>
      <p>价格：¥${item.price}</p>
      <p>分类：${escapeHtml(item.category)} | 交易方式：${escapeHtml(item.tradeType)}</p>
      <p>描述：${escapeHtml(item.desc)}</p>
      <p>发布时间：${formatDate(item.createdAt)}</p>
      <p>信用参考：卖家信用分 ${getCredit(item.sellerId)}，浏览 ${item.views}</p>
      <div class="detail-actions">
        <button class="primary-btn" id="detailContactBtn" ${
          item.status !== "sale" || item.sellerId === state.currentUser?.id || isBlockedCurrentUser() ? "disabled" : ""
        }>发起交易</button>
        <button class="action-btn" id="detailReportBtn" ${item.sellerId === state.currentUser?.id || isBlockedCurrentUser() ? "disabled" : ""}>举报商品</button>
        <button class="ghost-btn" id="closeDialogBtn">关闭</button>
      </div>
    </div>
  `;
  detailDialog.showModal();
  document.getElementById("closeDialogBtn").onclick = () => detailDialog.close();
  document.getElementById("detailContactBtn").onclick = () => {
    handleGoodsAction("contact", item.id);
    detailDialog.close();
  };
  document.getElementById("detailReportBtn").onclick = () => {
    createReport(item);
    detailDialog.close();
  };
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
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const formData = new FormData(form);

    const newItem = {
      id: `g${Date.now()}`,
      title: String(formData.get("title") || "").trim(),
      category: String(formData.get("category") || "教材"),
      price: Number(formData.get("price") || 0),
      imageUrl: String(formData.get("imageUrl") || "").trim(),
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
    alert("发布成功，已在首页展示");
    state.tab = "home";
    setActiveTab();
    render();
  });
}

function renderMessages() {
  const tpl = document.getElementById("messagesTemplate");
  appEl.replaceChildren(tpl.content.cloneNode(true));

  const intentListEl = document.getElementById("intentList");
  const intents = state.data.intents.filter(
    (it) => it.buyerId === state.currentUser.id || it.sellerId === state.currentUser.id,
  );

  if (!intents.length) {
    intentListEl.innerHTML = "<p class=\"muted\">暂无交易意向，去首页找找好物吧。</p>";
  } else {
    intentListEl.innerHTML = intents
      .map((it) => {
        const isSeller = it.sellerId === state.currentUser.id;
        const statusText =
          it.status === "pending"
            ? "待确认"
            : it.status === "accepted"
              ? "待交易"
              : it.status === "completed"
                ? "已完成"
                : "已拒绝";

        const actions = [];
        if (isSeller && it.status === "pending") {
          actions.push(`<button class=\"action-btn\" data-ia=\"accept\" data-id=\"${it.id}\">同意</button>`);
          actions.push(`<button class=\"action-btn\" data-ia=\"reject\" data-id=\"${it.id}\">拒绝</button>`);
        }
        if (!isSeller && it.status === "accepted") {
          actions.push(`<button class=\"action-btn\" data-ia=\"complete\" data-id=\"${it.id}\">确认完成</button>`);
        }
        if (!isSeller && it.status === "completed" && !it.rated) {
          actions.push(`<button class=\"action-btn\" data-ia=\"rate\" data-id=\"${it.id}\">评价卖家</button>`);
        }

        return `
          <article class="msg-item">
            <strong>${escapeHtml(it.itemTitle)}</strong>
            <p>${isSeller ? `买家：${escapeHtml(it.buyerName)}` : `卖家：${escapeHtml(it.sellerName)}`}</p>
            <p>状态：${statusText}</p>
            <p>${formatDate(it.updatedAt)}</p>
            <div class="item-actions">${actions.join("")}</div>
          </article>
        `;
      })
      .join("");

    intentListEl.querySelectorAll("button[data-ia]").forEach((btn) => {
      btn.addEventListener("click", () => handleIntentAction(btn.dataset.ia, btn.dataset.id));
    });
  }

  const listEl = document.getElementById("messagesList");
  const messages = state.data.messages.filter(
    (msg) => msg.toId === state.currentUser.id || msg.from === "系统通知" || msg.from === state.currentUser.name,
  );

  if (!messages.length) {
    listEl.innerHTML = "<p class=\"muted\">暂无消息</p>";
    return;
  }

  listEl.innerHTML = messages
    .map(
      (msg) => `
      <article class="msg-item">
        <strong>${escapeHtml(msg.from)}</strong>
        <p>${escapeHtml(msg.item)}</p>
        <p>${escapeHtml(msg.text)}</p>
        <p>${escapeHtml(msg.time)}</p>
      </article>
    `,
    )
    .join("");
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

  document.getElementById("logoutBtn").addEventListener("click", () => {
    if (!confirm("确定退出当前账号吗？")) return;
    state.currentUser = null;
    state.authToken = "";
    resetUser();
    resetToken();
    state.tab = "home";
    render();
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
    alert("请先选择商品");
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
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${d} ${hh}:${mm}`;
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
