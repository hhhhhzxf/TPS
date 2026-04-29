import assert from "node:assert/strict";
import { spawn } from "node:child_process";

const PORT = process.env.TEST_PORT || "3101";
const HOST = "127.0.0.1";
const BASE = `http://${HOST}:${PORT}`;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForHealth(timeoutMs = 8000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const res = await fetch(`${BASE}/api/health`);
      if (res.ok) return;
    } catch {
      // ignore
    }
    await sleep(200);
  }
  throw new Error("Server did not become healthy in time");
}

async function api(path, { method = "GET", token = "", body } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  let data = null;
  const text = await res.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  return { status: res.status, data };
}

function randomId(prefix) {
  return `${prefix}${Date.now()}${Math.floor(Math.random() * 1000)}`;
}

function randomStudentId(prefix = "23") {
  const rand = Math.floor(Math.random() * 1000000)
    .toString()
    .padStart(6, "0");
  return `${prefix}${rand}`;
}

const server = spawn("node", ["server.js"], {
  cwd: process.cwd(),
  env: {
    ...process.env,
    PORT,
    HOST,
    TOKEN_SECRET: "smoke-test-secret",
  },
  stdio: ["ignore", "pipe", "pipe"],
});

server.stdout.on("data", (chunk) => process.stdout.write(`[server] ${chunk}`));
server.stderr.on("data", (chunk) => process.stderr.write(`[server-err] ${chunk}`));

try {
  await waitForHealth();

  const noAuthState = await api("/api/state");
  assert.equal(noAuthState.status, 401, "state endpoint must require auth");

  const buyerStudentId = randomStudentId("23");
  const sellerStudentId = randomStudentId("24");
  const buyerUserId = `u${buyerStudentId}`;
  const sellerUserId = `u${sellerStudentId}`;

  const buyerLogin = await api("/api/auth/login", {
    method: "POST",
    body: {
      name: "买家测试",
      studentId: buyerStudentId,
      email: "buyer@campus.edu.cn",
    },
  });
  assert.equal(buyerLogin.status, 200, "buyer login should succeed");
  const buyerToken = buyerLogin.data?.token;
  assert.ok(buyerToken, "buyer should receive token");

  const sellerLogin = await api("/api/auth/login", {
    method: "POST",
    body: {
      name: "卖家测试",
      studentId: sellerStudentId,
      email: "seller@campus.edu.cn",
    },
  });
  assert.equal(sellerLogin.status, 200, "seller login should succeed");
  const sellerToken = sellerLogin.data?.token;
  assert.ok(sellerToken, "seller should receive token");

  const adminLogin = await api("/api/auth/login", {
    method: "POST",
    body: {
      name: "王老师",
      studentId: "99990000",
      email: "admin@campus.edu.cn",
    },
  });
  assert.equal(adminLogin.status, 200, "admin login should succeed");
  const adminToken = adminLogin.data?.token;
  assert.ok(adminToken, "admin should receive token");

  const normalUsersRead = await api("/api/users", { token: buyerToken });
  assert.equal(normalUsersRead.status, 403, "normal user cannot read user list");

  const goodsId = randomId("g");
  const createGoods = await api("/api/goods", {
    method: "POST",
    token: sellerToken,
    body: {
      id: goodsId,
      title: "链路测试商品",
      category: "生活",
      price: 15,
      desc: "full flow smoke test",
      imageUrl: "",
      sellerId: sellerUserId,
      sellerName: "卖家测试",
      tradeType: "当面交易",
      status: "sale",
      createdAt: new Date().toISOString(),
      views: 0,
    },
  });
  assert.equal(createGoods.status, 201, "seller can publish goods");

  const spoofGoods = await api("/api/goods", {
    method: "POST",
    token: buyerToken,
    body: {
      id: randomId("g"),
      title: "冒名商品",
      category: "生活",
      price: 10,
      desc: "should fail",
      imageUrl: "",
      sellerId: sellerUserId,
      sellerName: "卖家测试",
      tradeType: "当面交易",
      status: "sale",
      createdAt: new Date().toISOString(),
      views: 0,
    },
  });
  assert.equal(spoofGoods.status, 403, "buyer cannot publish for others");

  const viewRes = await api(`/api/goods/${goodsId}/views`, {
    method: "PATCH",
    token: buyerToken,
    body: { delta: 2 },
  });
  assert.equal(viewRes.status, 200, "view endpoint should work");
  assert.ok(Number(viewRes.data?.views) >= 2, "views should increase");

  const intentId = randomId("i");
  const createIntent = await api("/api/intents", {
    method: "POST",
    token: buyerToken,
    body: {
      intent: {
        id: intentId,
        itemId: goodsId,
        itemTitle: "链路测试商品",
        buyerId: buyerUserId,
        buyerName: "买家测试",
        sellerId: sellerUserId,
        sellerName: "卖家测试",
        status: "pending",
        rated: false,
        updatedAt: new Date().toISOString(),
      },
      message: {
        id: randomId("m"),
        from: "买家测试",
        toId: sellerUserId,
        item: "链路测试商品",
        text: "有意向购买",
        time: "刚刚",
      },
    },
  });
  assert.equal(createIntent.status, 201, "buyer can create intent");

  const acceptIntent = await api(`/api/intents/${intentId}`, {
    method: "PATCH",
    token: sellerToken,
    body: { status: "accepted", updatedAt: new Date().toISOString() },
  });
  assert.equal(acceptIntent.status, 200, "seller can accept intent");

  const completeIntent = await api(`/api/intents/${intentId}`, {
    method: "PATCH",
    token: buyerToken,
    body: { status: "completed", updatedAt: new Date().toISOString() },
  });
  assert.equal(completeIntent.status, 200, "buyer can complete intent");

  const ratingId = randomId("r");
  const ratingRes = await api("/api/ratings", {
    method: "POST",
    token: buyerToken,
    body: {
      rating: {
        id: ratingId,
        fromId: buyerUserId,
        toId: sellerUserId,
        score: 5,
        itemId: goodsId,
        time: new Date().toISOString(),
      },
      creditDelta: 3,
    },
  });
  assert.equal(ratingRes.status, 201, "buyer can submit rating");

  const reportId = randomId("rp");
  const reportRes = await api("/api/reports", {
    method: "POST",
    token: buyerToken,
    body: {
      report: {
        id: reportId,
        goodsId,
        goodsTitle: "链路测试商品",
        reporterId: buyerUserId,
        reporterName: "买家测试",
        sellerId: sellerUserId,
        reason: "测试举报",
        status: "pending",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    },
  });
  assert.equal(reportRes.status, 201, "buyer can submit report");

  const userReports = await api("/api/reports?status=pending", { token: buyerToken });
  assert.equal(userReports.status, 200, "buyer can query own pending reports");
  assert.ok(Array.isArray(userReports.data), "reports response must be array");
  assert.ok(userReports.data.some((x) => x.id === reportId), "pending report should be visible to buyer");

  const nonAdminReview = await api(`/api/reports/${reportId}`, {
    method: "PATCH",
    token: buyerToken,
    body: { status: "dismissed", action: "ignore" },
  });
  assert.equal(nonAdminReview.status, 403, "non-admin cannot review report");

  const adminReview = await api(`/api/reports/${reportId}`, {
    method: "PATCH",
    token: adminToken,
    body: {
      status: "resolved",
      action: "block_goods",
      reviewNote: "smoke test",
    },
  });
  assert.equal(adminReview.status, 200, "admin can review report");

  const batchGoods1 = randomId("g");
  const batchGoods2 = randomId("g");
  for (const gid of [batchGoods1, batchGoods2]) {
    const create = await api("/api/goods", {
      method: "POST",
      token: sellerToken,
      body: {
        id: gid,
        title: `批量商品-${gid}`,
        category: "生活",
        price: 18,
        desc: "batch test",
        imageUrl: "",
        sellerId: sellerUserId,
        sellerName: "卖家测试",
        tradeType: "当面交易",
        status: "sale",
        createdAt: new Date().toISOString(),
        views: 0,
      },
    });
    assert.equal(create.status, 201, "prepare batch goods should succeed");
  }

  const nonAdminBatch = await api("/api/goods/status-batch", {
    method: "PATCH",
    token: sellerToken,
    body: { ids: [batchGoods1, batchGoods2], status: "blocked" },
  });
  assert.equal(nonAdminBatch.status, 403, "non-admin cannot batch update goods");

  const adminBatch = await api("/api/goods/status-batch", {
    method: "PATCH",
    token: adminToken,
    body: { ids: [batchGoods1, batchGoods2], status: "blocked" },
  });
  assert.equal(adminBatch.status, 200, "admin can batch update goods");
  assert.equal(adminBatch.data?.changedCount, 2, "batch update should change 2 goods");

  const blockUser = await api(`/api/users/${encodeURIComponent(buyerUserId)}/status`, {
    method: "PATCH",
    token: adminToken,
    body: { status: "blocked" },
  });
  assert.equal(blockUser.status, 200, "admin can block user");

  const blockedCreateGoods = await api("/api/goods", {
    method: "POST",
    token: buyerToken,
    body: {
      id: randomId("g"),
      title: "封禁后发布",
      category: "生活",
      price: 10,
      desc: "should fail",
      imageUrl: "",
      sellerId: buyerUserId,
      sellerName: "买家测试",
      tradeType: "当面交易",
      status: "sale",
      createdAt: new Date().toISOString(),
      views: 0,
    },
  });
  assert.equal(blockedCreateGoods.status, 403, "blocked user cannot publish goods");

  const blockedCreateIntent = await api("/api/intents", {
    method: "POST",
    token: buyerToken,
    body: {
      intent: {
        id: randomId("i"),
        itemId: batchGoods1,
        itemTitle: "blocked intent",
        buyerId: buyerUserId,
        buyerName: "买家测试",
        sellerId: sellerUserId,
        sellerName: "卖家测试",
        status: "pending",
        rated: false,
        updatedAt: new Date().toISOString(),
      },
    },
  });
  assert.equal(blockedCreateIntent.status, 403, "blocked user cannot create intent");

  const blockedReport = await api("/api/reports", {
    method: "POST",
    token: buyerToken,
    body: {
      report: {
        id: randomId("rp"),
        goodsId: batchGoods1,
        goodsTitle: "blocked report",
        reporterId: buyerUserId,
        reporterName: "买家测试",
        sellerId: sellerUserId,
        reason: "blocked",
        status: "pending",
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
    },
  });
  assert.equal(blockedReport.status, 403, "blocked user cannot report");

  const adminUsersRead = await api("/api/users", { token: adminToken });
  assert.equal(adminUsersRead.status, 200, "admin can read users");

  const userAuditDenied = await api("/api/audit-logs", { token: sellerToken });
  assert.equal(userAuditDenied.status, 403, "non-admin cannot read audit logs");

  const logs = await api("/api/audit-logs", { token: adminToken });
  assert.equal(logs.status, 200, "admin can read audit logs");
  assert.ok(Array.isArray(logs.data), "audit logs response should be array");
  assert.ok(logs.data.some((x) => x.action === "user_block"), "audit logs should include user_block action");
  assert.ok(
    logs.data.some((x) => x.action === "goods_status_batch_update"),
    "audit logs should include goods_status_batch_update action",
  );
  assert.ok(logs.data.some((x) => x.action === "report_review"), "audit logs should include report_review action");

  console.log("\nSmoke test passed.");
} catch (error) {
  console.error("\nSmoke test failed:", error.message);
  process.exitCode = 1;
} finally {
  if (!server.killed) {
    server.kill("SIGTERM");
    await sleep(300);
    if (!server.killed) server.kill("SIGKILL");
  }
}
