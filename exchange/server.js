const http = require("http");
const fs = require("fs");
const fsp = fs.promises;
const path = require("path");
const { URL } = require("url");
const crypto = require("crypto");

const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || "127.0.0.1";
const ROOT = __dirname;
const DB_PATH = path.join(ROOT, "db.json");
const ADMIN_STUDENT_IDS = new Set(["99990000"]);
const TOKEN_SECRET = process.env.TOKEN_SECRET || "dev-change-me";
const TOKEN_TTL_SEC = 7 * 24 * 60 * 60;

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
};

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

let dbWriteQueue = Promise.resolve();

function normalizeDb(data) {
  const db = data || {};
  db.goods = Array.isArray(db.goods) ? db.goods : [];
  db.messages = Array.isArray(db.messages) ? db.messages : [];
  db.intents = Array.isArray(db.intents) ? db.intents : [];
  db.reports = Array.isArray(db.reports) ? db.reports : [];
  db.auditLogs = Array.isArray(db.auditLogs) ? db.auditLogs : [];
  db.ratings = Array.isArray(db.ratings) ? db.ratings : [];
  db.credits = db.credits || {};
  db.favorites = db.favorites || {};
  db.users = db.users || {};
  return db;
}

async function ensureDb() {
  try {
    await fsp.access(DB_PATH);
    const existing = await readDb();
    await writeDb(normalizeDb(existing));
  } catch {
    await writeDb(normalizeDb(seedData));
  }
}

async function readDb() {
  const raw = await fsp.readFile(DB_PATH, "utf-8");
  return normalizeDb(JSON.parse(raw));
}

function writeDb(nextData) {
  dbWriteQueue = dbWriteQueue.then(() =>
    fsp.writeFile(DB_PATH, JSON.stringify(normalizeDb(nextData), null, 2), "utf-8"),
  );
  return dbWriteQueue;
}

function applyCors(req, res) {
  const origin = String(req.headers.origin || "").trim();
  if (!origin) return;
  res.setHeader("Access-Control-Allow-Origin", origin);
  res.setHeader("Vary", "Origin");
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
  res.setHeader("Access-Control-Max-Age", "86400");
}

function sendJson(res, status, payload) {
  res.writeHead(status, { "Content-Type": MIME[".json"] });
  res.end(JSON.stringify(payload));
}

function notFound(res) {
  sendJson(res, 404, { error: "Not Found" });
}

function badRequest(res, message) {
  sendJson(res, 400, { error: message });
}

function unauthorized(res, message = "Unauthorized") {
  sendJson(res, 401, { error: message });
}

function forbidden(res, message = "Forbidden") {
  sendJson(res, 403, { error: message });
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk;
      if (raw.length > 1e6) {
        reject(new Error("Payload too large"));
        req.destroy();
      }
    });
    req.on("end", () => {
      if (!raw) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error("Invalid JSON"));
      }
    });
    req.on("error", reject);
  });
}

function safePathname(pathname) {
  const decoded = decodeURIComponent(pathname);
  const filePath = path.join(ROOT, decoded === "/" ? "/index.html" : decoded);
  if (!filePath.startsWith(ROOT)) return null;
  return filePath;
}

async function serveStatic(req, res, pathname) {
  const filePath = safePathname(pathname);
  if (!filePath) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  try {
    const stat = await fsp.stat(filePath);
    if (stat.isDirectory()) {
      res.writeHead(302, { Location: path.posix.join(pathname, "index.html") });
      res.end();
      return;
    }
    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, { "Content-Type": MIME[ext] || "application/octet-stream" });
    fs.createReadStream(filePath).pipe(res);
  } catch {
    try {
      const fallback = await fsp.readFile(path.join(ROOT, "index.html"));
      res.writeHead(200, { "Content-Type": MIME[".html"] });
      res.end(fallback);
    } catch {
      res.writeHead(404);
      res.end("Not Found");
    }
  }
}

function getIdFromPath(pathname, prefix) {
  const rest = pathname.slice(prefix.length);
  if (!rest.startsWith("/")) return null;
  const id = rest.slice(1);
  return id || null;
}

function isBlockedUser(db, userId) {
  const user = db.users[userId];
  return user && user.status === "blocked";
}

function base64UrlEncode(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function base64UrlDecode(input) {
  const normalized = input.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized + "=".repeat((4 - (normalized.length % 4 || 4)) % 4);
  return Buffer.from(padded, "base64").toString("utf-8");
}

function sign(data) {
  return base64UrlEncode(crypto.createHmac("sha256", TOKEN_SECRET).update(data).digest());
}

function issueToken(user) {
  const header = {
    alg: "HS256",
    typ: "JWT",
  };
  const payload = {
    sub: user.id,
    role: user.role,
    exp: Math.floor(Date.now() / 1000) + TOKEN_TTL_SEC,
  };
  const headerPart = base64UrlEncode(JSON.stringify(header));
  const payloadPart = base64UrlEncode(JSON.stringify(payload));
  const signingInput = `${headerPart}.${payloadPart}`;
  const sigPart = sign(signingInput);
  return `${headerPart}.${payloadPart}.${sigPart}`;
}

function verifyToken(token) {
  if (!token || typeof token !== "string") return null;
  const [headerPart, payloadPart, sigPart] = token.split(".");
  if (!headerPart || !payloadPart || !sigPart) return null;
  const expected = sign(`${headerPart}.${payloadPart}`);
  const sigBuf = Buffer.from(sigPart);
  const expBuf = Buffer.from(expected);
  if (sigBuf.length !== expBuf.length || !crypto.timingSafeEqual(sigBuf, expBuf)) return null;

  try {
    const header = JSON.parse(base64UrlDecode(headerPart));
    if (header.typ !== "JWT" || header.alg !== "HS256") return null;
    const payload = JSON.parse(base64UrlDecode(payloadPart));
    if (!payload.sub || !payload.exp || payload.exp < Math.floor(Date.now() / 1000)) return null;
    return payload;
  } catch {
    return null;
  }
}

function readBearerToken(req) {
  const auth = req.headers.authorization || "";
  if (!auth.startsWith("Bearer ")) return null;
  return auth.slice(7);
}

function requireAuth(req, res, db) {
  const token = readBearerToken(req);
  const payload = verifyToken(token);
  if (!payload) {
    unauthorized(res);
    return null;
  }
  const user = db.users[payload.sub];
  if (!user) {
    unauthorized(res, "User not found");
    return null;
  }
  return user;
}

function requireAdmin(res, authUser) {
  if (authUser.role === "admin") return true;
  forbidden(res);
  return false;
}

function requireActiveUser(res, authUser, message) {
  if (authUser.status !== "blocked") return true;
  forbidden(res, message || "Blocked user cannot perform this action");
  return false;
}

function requireSelfOrAdmin(res, authUser, targetUserId, message) {
  if (authUser.id === targetUserId || authUser.role === "admin") return true;
  forbidden(res, message || "No permission");
  return false;
}

function appendAuditLog(db, actor, action, targetType, targetId, detail) {
  db.auditLogs.unshift({
    id: `al${Date.now()}${Math.floor(Math.random() * 1000)}`,
    actorId: actor.id,
    actorName: actor.name,
    action,
    targetType,
    targetId,
    detail: detail || "",
    createdAt: new Date().toISOString(),
  });
  if (db.auditLogs.length > 500) db.auditLogs = db.auditLogs.slice(0, 500);
}

async function handleApi(req, res, pathname, searchParams) {
  if (req.method === "GET" && pathname === "/api/health") {
    sendJson(res, 200, { ok: true, now: new Date().toISOString() });
    return;
  }

  if (req.method === "POST" && pathname === "/api/auth/login") {
    const body = await parseBody(req);
    const name = String(body.name || "").trim();
    const studentId = String(body.studentId || "").trim();
    const email = String(body.email || "").trim();

    if (!name || !/^[0-9]{8}$/.test(studentId) || !email.includes("@")) {
      badRequest(res, "Invalid login payload");
      return;
    }

    const userId = `u${studentId}`;
    const role = ADMIN_STUDENT_IDS.has(studentId) ? "admin" : "user";

    const db = await readDb();
    if (!db.credits[userId]) db.credits[userId] = role === "admin" ? 100 : 80;
    if (!db.favorites[userId]) db.favorites[userId] = [];
    const prevUser = db.users[userId] || {};
    db.users[userId] = {
      id: userId,
      name,
      studentId,
      email,
      role: prevUser.role || role,
      status: prevUser.status || "active",
    };
    await writeDb(db);

    sendJson(res, 200, {
      token: issueToken(db.users[userId]),
      user: db.users[userId],
    });
    return;
  }

  const db = await readDb();
  const authUser = requireAuth(req, res, db);
  if (!authUser) return;

  if (req.method === "GET" && pathname === "/api/state") {
    sendJson(res, 200, db);
    return;
  }

  if (req.method === "GET" && pathname === "/api/users") {
    if (!requireAdmin(res, authUser)) return;
    sendJson(res, 200, Object.values(db.users));
    return;
  }

  if (req.method === "GET" && pathname === "/api/audit-logs") {
    if (!requireAdmin(res, authUser)) return;
    sendJson(res, 200, db.auditLogs);
    return;
  }

  if (req.method === "PATCH" && pathname.startsWith("/api/users/") && pathname.endsWith("/status")) {
    if (!requireAdmin(res, authUser)) return;

    const userId = pathname.slice("/api/users/".length, -"/status".length);
    const body = await parseBody(req);
    const status = String(body.status || "");
    if (!["active", "blocked"].includes(status)) {
      badRequest(res, "Invalid user status");
      return;
    }

    if (!db.users[userId]) {
      badRequest(res, "User not found");
      return;
    }
    if (db.users[userId].role === "admin") {
      badRequest(res, "Cannot block admin");
      return;
    }

    db.users[userId].status = status;
    appendAuditLog(
      db,
      authUser,
      status === "blocked" ? "user_block" : "user_unblock",
      "user",
      userId,
      `set status=${status}`,
    );
    await writeDb(db);
    sendJson(res, 200, db.users[userId]);
    return;
  }

  if (req.method === "POST" && pathname === "/api/goods") {
    const body = await parseBody(req);
    if (!body || !body.id || !body.title || !body.sellerId) {
      badRequest(res, "Invalid goods payload");
      return;
    }
    if (!requireActiveUser(res, authUser, "Blocked user cannot publish goods")) return;
    if (!requireSelfOrAdmin(res, authUser, body.sellerId, "Cannot publish for another user")) return;

    db.goods.unshift(body);
    await writeDb(db);
    sendJson(res, 201, body);
    return;
  }

  if (req.method === "PATCH" && pathname.startsWith("/api/goods/") && pathname.endsWith("/status")) {
    const goodsId = pathname.slice("/api/goods/".length, -"/status".length);
    const body = await parseBody(req);
    const status = String(body.status || "");
    if (!["sale", "sold", "blocked"].includes(status)) {
      badRequest(res, "Invalid status");
      return;
    }

    const goods = db.goods.find((item) => item.id === goodsId);
    if (!goods) {
      notFound(res);
      return;
    }

    if (!requireSelfOrAdmin(res, authUser, goods.sellerId, "No permission to update this goods")) return;

    goods.status = status;
    if (authUser.role === "admin") {
      appendAuditLog(db, authUser, "goods_status_update", "goods", goods.id, `set status=${status}`);
    }
    await writeDb(db);
    sendJson(res, 200, goods);
    return;
  }

  if (req.method === "PATCH" && pathname === "/api/goods/status-batch") {
    if (!requireAdmin(res, authUser)) return;

    const body = await parseBody(req);
    const ids = Array.isArray(body.ids) ? body.ids : [];
    const status = String(body.status || "");
    if (!ids.length || !["sale", "sold", "blocked"].includes(status)) {
      badRequest(res, "Invalid batch payload");
      return;
    }

    const changed = [];
    for (const id of ids) {
      const goods = db.goods.find((item) => item.id === id);
      if (goods) {
        goods.status = status;
        changed.push(goods.id);
      }
    }
    appendAuditLog(
      db,
      authUser,
      "goods_status_batch_update",
      "goods_batch",
      changed.join(","),
      `count=${changed.length}, status=${status}`,
    );
    await writeDb(db);
    sendJson(res, 200, { changedCount: changed.length, ids: changed, status });
    return;
  }

  if (req.method === "PATCH" && pathname.startsWith("/api/goods/") && pathname.endsWith("/views")) {
    const goodsId = pathname.slice("/api/goods/".length, -"/views".length);
    const body = await parseBody(req);
    const delta = Number(body.delta || 1);

    const goods = db.goods.find((item) => item.id === goodsId);
    if (!goods) {
      notFound(res);
      return;
    }

    goods.views = Number(goods.views || 0) + (Number.isNaN(delta) ? 1 : delta);
    await writeDb(db);
    sendJson(res, 200, goods);
    return;
  }

  if (req.method === "POST" && pathname === "/api/intents") {
    const body = await parseBody(req);
    if (!body || !body.intent || !body.intent.id) {
      badRequest(res, "Invalid intents payload");
      return;
    }
    if (!requireActiveUser(res, authUser, "Blocked user cannot create intents")) return;
    if (!requireSelfOrAdmin(res, authUser, body.intent.buyerId, "Cannot create intent for another user")) return;

    db.intents.unshift(body.intent);
    if (body.message && body.message.id) db.messages.unshift(body.message);
    await writeDb(db);
    sendJson(res, 201, { intent: body.intent, message: body.message || null });
    return;
  }

  if (req.method === "PATCH" && pathname.startsWith("/api/intents/")) {
    const intentId = getIdFromPath(pathname, "/api/intents");
    const body = await parseBody(req);
    const intent = db.intents.find((it) => it.id === intentId);
    if (!intent) {
      notFound(res);
      return;
    }

    const isParticipant = intent.buyerId === authUser.id || intent.sellerId === authUser.id;
    if (!isParticipant && authUser.role !== "admin") {
      forbidden(res, "No permission for this intent");
      return;
    }

    if (body.status) intent.status = String(body.status);
    if (typeof body.rated === "boolean") intent.rated = body.rated;
    intent.updatedAt = body.updatedAt || new Date().toISOString();

    if (intent.status === "completed") {
      const goods = db.goods.find((item) => item.id === intent.itemId);
      if (goods && goods.status !== "blocked") goods.status = "sold";
    }

    await writeDb(db);
    sendJson(res, 200, intent);
    return;
  }

  if (req.method === "POST" && pathname === "/api/messages") {
    const body = await parseBody(req);
    const message = body && body.message;
    if (!message || !message.id || !message.toId || !String(message.text || "").trim()) {
      badRequest(res, "Invalid message payload");
      return;
    }
    if (!requireActiveUser(res, authUser, "Blocked user cannot send messages")) return;
    message.fromId = message.fromId || authUser.id;
    if (!requireSelfOrAdmin(res, authUser, message.fromId, "Cannot send message as another user")) return;

    if (message.intentId) {
      const intent = db.intents.find((it) => it.id === message.intentId);
      if (!intent) {
        badRequest(res, "Intent not found for message");
        return;
      }
      const isParticipant = intent.buyerId === authUser.id || intent.sellerId === authUser.id;
      const toIsParticipant = intent.buyerId === message.toId || intent.sellerId === message.toId;
      if (!isParticipant || !toIsParticipant) {
        forbidden(res, "No permission for this conversation");
        return;
      }
      message.itemId = message.itemId || intent.itemId;
      message.item = message.item || intent.itemTitle;
    }

    message.from = message.from || authUser.name;
    message.fromId = message.fromId || authUser.id;
    message.createdAt = message.createdAt || new Date().toISOString();
    message.time = message.time || message.createdAt;

    db.messages.unshift(message);
    await writeDb(db);
    sendJson(res, 201, message);
    return;
  }

  if (req.method === "POST" && pathname === "/api/ratings") {
    const body = await parseBody(req);
    const rating = body.rating;
    if (!rating || !rating.id || !rating.toId || !rating.fromId) {
      badRequest(res, "Invalid rating payload");
      return;
    }
    if (!requireSelfOrAdmin(res, authUser, rating.fromId, "Cannot submit rating for another user")) return;

    db.ratings.unshift(rating);
    db.credits[rating.toId] = Number(db.credits[rating.toId] || 80) + Number(body.creditDelta || 3);
    await writeDb(db);
    sendJson(res, 201, {
      rating,
      newCredit: db.credits[rating.toId],
    });
    return;
  }

  if (req.method === "POST" && pathname === "/api/reports") {
    const body = await parseBody(req);
    const report = body.report;
    if (!report || !report.id || !report.goodsId || !report.reporterId) {
      badRequest(res, "Invalid report payload");
      return;
    }
    if (!requireActiveUser(res, authUser, "Blocked user cannot submit reports")) return;
    if (!requireSelfOrAdmin(res, authUser, report.reporterId, "Cannot submit report for another user")) return;

    db.reports.unshift(report);
    await writeDb(db);
    sendJson(res, 201, report);
    return;
  }

  if (req.method === "GET" && pathname === "/api/reports") {
    const status = searchParams.get("status");
    let reports = status ? db.reports.filter((rp) => rp.status === status) : db.reports;
    if (authUser.role !== "admin") {
      reports = reports.filter((rp) => rp.reporterId === authUser.id);
    }
    sendJson(res, 200, reports);
    return;
  }

  if (req.method === "PATCH" && pathname.startsWith("/api/reports/")) {
    if (!requireAdmin(res, authUser)) return;

    const reportId = getIdFromPath(pathname, "/api/reports");
    const body = await parseBody(req);
    const report = db.reports.find((rp) => rp.id === reportId);
    if (!report) {
      notFound(res);
      return;
    }

    report.status = String(body.status || report.status || "pending");
    report.reviewedBy = body.reviewedBy || authUser.id;
    report.reviewNote = body.reviewNote || report.reviewNote || "";
    report.updatedAt = new Date().toISOString();

    if (body.action === "block_goods") {
      const goods = db.goods.find((item) => item.id === report.goodsId);
      if (goods) goods.status = "blocked";
    }

    appendAuditLog(
      db,
      authUser,
      "report_review",
      "report",
      report.id,
      `status=${report.status}, action=${String(body.action || "none")}`,
    );

    await writeDb(db);
    sendJson(res, 200, report);
    return;
  }

  if (req.method === "PUT" && pathname.startsWith("/api/users/") && pathname.endsWith("/favorites")) {
    const userId = pathname.slice("/api/users/".length, -"/favorites".length);
    if (!requireSelfOrAdmin(res, authUser, userId)) return;

    const body = await parseBody(req);
    if (!Array.isArray(body.favorites)) {
      badRequest(res, "favorites must be an array");
      return;
    }

    db.favorites[userId] = body.favorites;
    await writeDb(db);
    sendJson(res, 200, { userId, favorites: db.favorites[userId] });
    return;
  }

  if (req.method === "PUT" && pathname === "/api/state") {
    if (!requireAdmin(res, authUser)) return;

    const body = await parseBody(req);
    if (!body || typeof body !== "object") {
      badRequest(res, "State payload is required");
      return;
    }
    const nextState = normalizeDb(body);
    appendAuditLog(nextState, authUser, "state_overwrite", "state", "all", "legacy PUT /api/state");
    await writeDb(nextState);
    sendJson(res, 200, { ok: true });
    return;
  }

  notFound(res);
}

async function start() {
  await ensureDb();

  const server = http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, `http://${req.headers.host}`);
      applyCors(req, res);
      if (url.pathname.startsWith("/api/")) {
        if (req.method === "OPTIONS") {
          res.writeHead(204);
          res.end();
          return;
        }
        await handleApi(req, res, url.pathname, url.searchParams);
        return;
      }
      await serveStatic(req, res, url.pathname);
    } catch (error) {
      sendJson(res, 500, { error: "Server Error", detail: String(error.message || error) });
    }
  });

  server.listen(PORT, HOST, () => {
    console.log(`MY校园淘二手 server running at http://${HOST}:${PORT}`);
  });
}

start();
