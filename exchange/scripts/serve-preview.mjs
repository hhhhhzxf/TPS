import http from "node:http";
import fs from "node:fs/promises";
import path from "node:path";

const rootDir = process.cwd();
const port = Number(process.env.PORT || 9876);
const host = process.env.HOST || "127.0.0.1";
const apiBaseUrl = String(process.env.API_BASE_URL || "").trim();
const previewMode = String(process.env.PREVIEW_MODE || "").trim() === "true";
const serverLabel = previewMode ? "preview" : "web";

const mime = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".webmanifest": "application/manifest+json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
};

function buildPreviewConfig() {
  return `window.__APP_CONFIG__ = Object.assign({ apiBaseUrl: ${JSON.stringify(apiBaseUrl)}, previewMode: ${previewMode} }, window.__APP_CONFIG__ || {});\n`;
}

function safeFilePath(urlPath) {
  const decoded = decodeURIComponent(urlPath);
  const fullPath = path.join(rootDir, decoded === "/" ? "index.html" : decoded.replace(/^\/+/, ""));
  if (!fullPath.startsWith(rootDir)) return null;
  return fullPath;
}

async function serveFile(res, filePath) {
  try {
    const stat = await fs.stat(filePath);
    if (stat.isDirectory()) {
      await serveFile(res, path.join(filePath, "index.html"));
      return;
    }
    const ext = path.extname(filePath).toLowerCase();
    const content = await fs.readFile(filePath);
    res.writeHead(200, {
      "Content-Type": mime[ext] || "application/octet-stream",
      "Cache-Control": "no-store, no-cache, must-revalidate",
      Pragma: "no-cache",
      Expires: "0",
    });
    res.end(content);
  } catch {
    const html = await fs.readFile(path.join(rootDir, "index.html"));
    res.writeHead(200, {
      "Content-Type": mime[".html"],
      "Cache-Control": "no-store, no-cache, must-revalidate",
      Pragma: "no-cache",
      Expires: "0",
    });
    res.end(html);
  }
}

const server = http.createServer(async (req, res) => {
  try {
    const requestUrl = new URL(req.url, `http://${req.headers.host}`);
    if (requestUrl.pathname === "/app-config.js") {
      res.writeHead(200, {
        "Content-Type": mime[".js"],
        "Cache-Control": "no-store, no-cache, must-revalidate",
        Pragma: "no-cache",
        Expires: "0",
      });
      res.end(buildPreviewConfig());
      return;
    }

    const filePath = safeFilePath(requestUrl.pathname);
    if (!filePath) {
      res.writeHead(403);
      res.end("Forbidden");
      return;
    }

    await serveFile(res, filePath);
  } catch (error) {
    res.writeHead(500, { "Content-Type": "text/plain; charset=utf-8" });
    res.end(`Preview server error: ${String(error.message || error)}`);
  }
});

server.listen(port, host, () => {
  console.log(`Campus Exchange ${serverLabel} running at http://${host}:${port}`);
});
