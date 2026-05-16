import { readdir, readFile } from 'node:fs/promises';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = new URL('../', import.meta.url);
const rootPath = fileURLToPath(rootDir);
const sourcePath = fileURLToPath(new URL('src/', rootDir));
const bannedSourcePatterns = [
  { label: 'legacy MMMail CSS variables', pattern: /--(?:mm|v211)-/ },
  { label: 'direct axios import', pattern: /import\s+(?:['"]axios['"]|[^'"]+\s+from\s+['"]axios['"])/ },
  { label: 'Element Plus usage', pattern: /element-plus/i },
  { label: 'Ant Design Vue usage', pattern: /ant-design-vue/i },
  { label: 'Tailwind usage', pattern: /@tailwind|tailwindcss|@tailwindcss/i },
  { label: 'Moment usage', pattern: /from\s+['"]moment['"]|require\(['"]moment['"]\)/i },
  { label: 'SortableJS usage', pattern: /sortablejs/i },
  { label: 'Chart.js usage', pattern: /chart\.js/i },
  { label: 'D3 usage', pattern: /from\s+['"]d3(?:-[^'"]+)?['"]|require\(['"]d3(?:-[^'"]+)?['"]\)/i },
  { label: 'Mapbox usage', pattern: /mapbox|mapbox-gl|@mapbox/i }
];
const bannedDependencyPatterns = [
  { label: 'Element Plus', pattern: /^element-plus$/i },
  { label: 'Ant Design Vue', pattern: /^ant-design-vue$/i },
  { label: 'Vant', pattern: /^vant$/i },
  { label: 'Tailwind', pattern: /^(tailwindcss|@tailwindcss\/.+)$/i },
  { label: 'Moment', pattern: /^moment$/i },
  { label: 'SortableJS', pattern: /^sortablejs$/i },
  { label: 'Chart.js', pattern: /^chart\.js$/i },
  { label: 'D3', pattern: /^(d3|d3-.+)$/i },
  { label: 'Mapbox', pattern: /^(mapbox|mapbox-gl|@mapbox\/.+)$/i }
];
const sourceExtensions = new Set(['.ts', '.vue', '.css', '.scss']);

async function walk(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const child = join(dir, entry.name);

    if (entry.isDirectory()) {
      files.push(...(await walk(child)));
    }

    if (entry.isFile()) {
      files.push(child);
    }
  }

  return files;
}

async function readJson(relativePath) {
  const content = await readFile(new URL(relativePath, rootDir), 'utf8');

  return JSON.parse(content);
}

function collectPackageViolations(pkg) {
  const allDeps = { ...pkg.dependencies, ...pkg.devDependencies };

  return Object.keys(allDeps).flatMap(dep => {
    const rule = bannedDependencyPatterns.find(({ pattern }) => pattern.test(dep));

    return rule ? [`Forbidden dependency (${rule.label}): ${dep}`] : [];
  });
}

async function collectSourceViolations() {
  const files = await walk(sourcePath);
  const checkedFiles = files.filter(file => sourceExtensions.has(extname(file)));
  const violations = [];

  for (const file of checkedFiles) {
    const source = await readFile(file, 'utf8');
    const relativePath = file.replace(rootPath, '');

    if (extname(file) === '.scss' && !relativePath.startsWith('src/styles/scss/')) {
      violations.push(`Unexpected SCSS file: ${relativePath}`);
    }

    for (const { label, pattern } of bannedSourcePatterns) {
      if (pattern.test(source)) {
        violations.push(`${label}: ${relativePath}`);
      }
    }
  }

  return violations;
}

const pkg = await readJson('package.json');
const violations = [...collectPackageViolations(pkg), ...(await collectSourceViolations())];

if (violations.length) {
  console.error(violations.join('\n'));
  process.exitCode = 1;
} else {
  console.log('Style discipline checks passed.');
}
