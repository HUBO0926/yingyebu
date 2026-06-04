import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = new URL('../', import.meta.url);
const rootPath = fileURLToPath(root);
const requiredPageExts = ['js', 'json', 'wxml', 'wxss'];
const textExts = new Set(['.js', '.json', '.wxml', '.wxss', '.md', '.vue', '.ts']);
const badPatterns = [
  /\uFFFD/,
  /[ÃÂ�]/,
  /[\u4e00-\u9fa5][\uFFFD]/,
  /鏅|浜|绯|鎶|鍙|瀹|璇|閿|鐭|妗|规|灞/
];
const mojibakeSnippets = [
  '\u7ed7',
  '\u5bf0\u546d',
  '\u95bf',
  '\u9352',
  '\u59e4\u4ecb',
  '\u947d',
  '\u72b1\u7d85'
];

function readJson(relativePath) {
  const file = new URL(relativePath, root);
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (error) {
    throw new Error(`${relativePath} is not valid JSON: ${error.message}`);
  }
}

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  return entries.flatMap((entry) => {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) return walk(full);
    return [full];
  });
}

const app = readJson('app.json');
readJson('project.config.json');

const errors = [];
for (const page of app.pages) {
  for (const ext of requiredPageExts) {
    const file = new URL(`${page}.${ext}`, root);
    if (!fs.existsSync(file)) errors.push(`Missing ${page}.${ext}`);
  }
}

for (const file of walk(rootPath)) {
  const ext = path.extname(file);
  if (!textExts.has(ext)) continue;
  const rel = path.relative(rootPath, file).replaceAll('\\', '/');
  const content = fs.readFileSync(file, 'utf8');
  if (badPatterns.some((pattern) => pattern.test(content))) {
    errors.push(`Possible mojibake text in ${rel}`);
  }
  if (mojibakeSnippets.some((snippet) => content.includes(snippet))) {
    errors.push(`Possible mojibake snippet in ${rel}`);
  }
  if ((ext === '.wxml' || ext === '.wxss') && /\\u[0-9a-fA-F]{4}/.test(content)) {
    errors.push(`Raw unicode escape in template/style file ${rel}`);
  }
  if (ext === '.json') {
    try {
      JSON.parse(content);
    } catch (error) {
      errors.push(`Invalid JSON in ${rel}: ${error.message}`);
    }
  }
}

if (errors.length) {
  console.error(errors.join('\n'));
  process.exit(1);
}

console.log(`Checked ${app.pages.length} pages, JSON files, and text encoding.`);
