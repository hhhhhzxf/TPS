import fs from "node:fs/promises";
import path from "node:path";

const rootDir = process.cwd();
const dbPath = path.join(rootDir, "db.json");

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

await fs.writeFile(dbPath, `${JSON.stringify(seedData, null, 2)}\n`, "utf-8");
console.log(`Reset db.json to seed data: ${dbPath}`);
