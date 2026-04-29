import fs from "node:fs/promises";
import path from "node:path";

const rootDir = process.cwd();
const outDir = path.join(rootDir, "mobile-web");

const staticFiles = [
  "index.html",
  "app.js",
  "styles.css",
  "manifest.webmanifest",
  "sw.js",
  "icon.svg",
  "app-config.js",
];

async function ensureCleanDir(dir) {
  await fs.rm(dir, { recursive: true, force: true });
  await fs.mkdir(dir, { recursive: true });
}

async function copyStaticFiles() {
  for (const file of staticFiles) {
    await fs.copyFile(path.join(rootDir, file), path.join(outDir, file));
  }
}

async function main() {
  await ensureCleanDir(outDir);
  await copyStaticFiles();
  console.log(`Prepared mobile web assets in: ${outDir}`);
}

main().catch((error) => {
  console.error("Failed to prepare mobile web assets:", error);
  process.exitCode = 1;
});
