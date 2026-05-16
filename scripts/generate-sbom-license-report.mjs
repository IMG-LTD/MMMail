#!/usr/bin/env node
import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import { dirname, join, relative, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = resolve(SCRIPT_DIR, '..');
const OUTPUT_DIR = process.env.MMMAIL_SUPPLY_CHAIN_REPORT_DIR || join(tmpdir(), 'mmmail-supply-chain');
const GENERATED_AT = new Date().toISOString();
const CYCLONEDX_VERSION = '1.5';
const SPDX_VERSION = 'SPDX-2.3';
const UNKNOWN_LICENSE = 'NOASSERTION';

const paths = {
  frontendPackage: join(ROOT_DIR, 'frontend-admin/package.json'),
  frontendNodeModules: join(ROOT_DIR, 'frontend-admin/node_modules'),
  backendRoot: join(ROOT_DIR, 'backend')
};

const frontendComponents = await collectFrontendComponents();
const backendComponents = await collectBackendComponents();
const components = dedupeComponents([...frontendComponents, ...backendComponents]);

await mkdir(OUTPUT_DIR, { recursive: true });
await writeJson('mmmail-sbom.cdx.json', buildCycloneDxBom(components));
await writeJson('dependency-license-report.json', buildLicenseReport(components));
await writeJson('dependency-license-report.spdx.json', buildSpdxReport(components));

console.log(`[supply-chain] wrote ${components.length} components to ${relative(ROOT_DIR, OUTPUT_DIR)}`);

async function collectFrontendComponents() {
  const packageJson = await readJson(paths.frontendPackage);
  const rootComponent = packageComponent({
    name: packageJson.name,
    version: packageJson.version,
    license: packageJson.license,
    scope: 'frontend-root',
    purlType: 'npm'
  });
  const dependencyComponents = await collectPackageDependencies(packageJson);
  return [rootComponent, ...dependencyComponents];
}

async function collectPackageDependencies(packageJson) {
  const dependencies = {
    ...(packageJson.dependencies || {}),
    ...(packageJson.devDependencies || {})
  };
  const entries = Object.entries(dependencies).sort(([left], [right]) => left.localeCompare(right));
  const components = [];

  for (const [name, specifier] of entries) {
    const manifest = await readNodePackageManifest(name);
    components.push(
      packageComponent({
        name,
        version: manifest?.version || normalizeVersionSpecifier(specifier),
        license: manifest?.license || UNKNOWN_LICENSE,
        scope: 'frontend',
        purlType: 'npm'
      })
    );
  }

  return components;
}

async function collectBackendComponents() {
  const pomPaths = await findFiles(paths.backendRoot, 'pom.xml');
  const propertyMap = await collectMavenProperties(pomPaths);
  const components = [];

  for (const pomPath of pomPaths) {
    const xml = await readFile(pomPath, 'utf8');
    components.push(...parseMavenDependencies(xml, propertyMap, pomPath));
  }

  return components;
}

async function collectMavenProperties(pomPaths) {
  const propertyMap = new Map();

  for (const pomPath of pomPaths) {
    const xml = await readFile(pomPath, 'utf8');
    for (const [name, value] of parseXmlProperties(xml)) {
      propertyMap.set(name, value);
    }
  }

  return propertyMap;
}

function parseMavenDependencies(xml, propertyMap, pomPath) {
  const dependencies = [];
  const blocks = xml.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g);

  for (const match of blocks) {
    const block = match[1];
    const scope = xmlText(block, 'scope') || 'compile';
    if (scope === 'test') continue;

    const groupId = xmlText(block, 'groupId');
    const artifactId = xmlText(block, 'artifactId');
    if (!groupId || !artifactId) continue;

    dependencies.push(
      packageComponent({
        name: `${groupId}:${artifactId}`,
        version: resolveMavenVersion(xmlText(block, 'version'), propertyMap),
        license: UNKNOWN_LICENSE,
        scope: `backend:${scope}`,
        purlType: 'maven',
        source: relative(ROOT_DIR, pomPath)
      })
    );
  }

  return dependencies;
}

function packageComponent(options) {
  return {
    type: 'library',
    name: options.name,
    version: options.version || UNKNOWN_LICENSE,
    scope: options.scope,
    purl: buildPurl(options.purlType, options.name, options.version),
    licenses: [normalizeLicense(options.license)],
    source: options.source || ''
  };
}

function buildPurl(type, name, version) {
  const encodedName = encodeURIComponent(name).replaceAll('%2F', '/').replaceAll('%3A', '/');
  const versionSuffix = version && version !== UNKNOWN_LICENSE ? `@${encodeURIComponent(version)}` : '';
  return `pkg:${type}/${encodedName}${versionSuffix}`;
}

function normalizeLicense(value) {
  if (!value) {
    return { expression: UNKNOWN_LICENSE };
  }
  if (typeof value === 'string') {
    return { expression: value };
  }
  if (Array.isArray(value)) {
    return { expression: value.map(item => item.type || item.name || UNKNOWN_LICENSE).join(' OR ') };
  }
  return { expression: value.type || value.name || UNKNOWN_LICENSE };
}

function dedupeComponents(items) {
  const components = new Map();

  for (const item of items) {
    const key = `${item.purl}|${item.scope}`;
    if (!components.has(key)) {
      components.set(key, item);
    }
  }

  return [...components.values()].sort((left, right) => left.name.localeCompare(right.name));
}

function buildCycloneDxBom(items) {
  return {
    bomFormat: 'CycloneDX',
    specVersion: CYCLONEDX_VERSION,
    serialNumber: `urn:uuid:${randomUUID()}`,
    version: 1,
    metadata: {
      timestamp: GENERATED_AT,
      component: {
        type: 'application',
        name: 'MMMail',
        version: 'v2.2-prep'
      }
    },
    components: items.map(toCycloneDxComponent)
  };
}

function toCycloneDxComponent(item) {
  return {
    type: item.type,
    name: item.name,
    version: item.version,
    purl: item.purl,
    licenses: [{ expression: item.licenses[0].expression }],
    properties: [
      { name: 'mmmail:scope', value: item.scope },
      { name: 'mmmail:source', value: item.source }
    ].filter(property => property.value)
  };
}

function buildLicenseReport(items) {
  const components = items.map(item => ({
    name: item.name,
    version: item.version,
    scope: item.scope,
    purl: item.purl,
    license: item.licenses[0].expression,
    source: item.source
  }));

  return {
    generatedAt: GENERATED_AT,
    format: 'MMMail dependency-license-report/v1',
    componentCount: components.length,
    unknownLicenseCount: components.filter(item => item.license === UNKNOWN_LICENSE).length,
    components
  };
}

function buildSpdxReport(items) {
  return {
    spdxVersion: SPDX_VERSION,
    dataLicense: 'CC0-1.0',
    SPDXID: 'SPDXRef-DOCUMENT',
    name: 'MMMail dependency license report',
    documentNamespace: `https://mmmail.local/spdx/${Date.now()}`,
    creationInfo: {
      created: GENERATED_AT,
      creators: ['Tool: MMMail generate-sbom-license-report.mjs']
    },
    packages: items.map(toSpdxPackage)
  };
}

function toSpdxPackage(item, index) {
  return {
    SPDXID: `SPDXRef-Package-${index + 1}`,
    name: item.name,
    versionInfo: item.version,
    downloadLocation: 'NOASSERTION',
    filesAnalyzed: false,
    licenseConcluded: item.licenses[0].expression,
    licenseDeclared: item.licenses[0].expression,
    supplier: 'NOASSERTION',
    externalRefs: [{ referenceCategory: 'PACKAGE-MANAGER', referenceType: 'purl', referenceLocator: item.purl }]
  };
}

async function readNodePackageManifest(name) {
  const manifestPath = join(paths.frontendNodeModules, ...name.split('/'), 'package.json');
  if (!existsSync(manifestPath)) {
    return null;
  }
  return readJson(manifestPath);
}

async function findFiles(baseDir, targetName) {
  const files = [];
  const entries = await readdir(baseDir, { withFileTypes: true });

  for (const entry of entries) {
    const path = join(baseDir, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await findFiles(path, targetName)));
    } else if (entry.name === targetName) {
      files.push(path);
    }
  }

  return files.sort();
}

function parseXmlProperties(xml) {
  const properties = [];
  const match = /<properties>([\s\S]*?)<\/properties>/.exec(xml);
  if (!match) return properties;

  for (const property of match[1].matchAll(/<([A-Za-z0-9_.-]+)>([^<]+)<\/\1>/g)) {
    properties.push([property[1], property[2].trim()]);
  }

  return properties;
}

function xmlText(xml, tag) {
  const match = new RegExp(`<${tag}>([\\s\\S]*?)<\\/${tag}>`).exec(xml);
  return match?.[1]?.trim() || '';
}

function resolveMavenVersion(version, propertyMap) {
  const propertyMatch = /^\$\{([^}]+)}$/.exec(version || '');
  if (!propertyMatch) return version || UNKNOWN_LICENSE;
  return propertyMap.get(propertyMatch[1]) || version;
}

function normalizeVersionSpecifier(specifier) {
  return String(specifier || UNKNOWN_LICENSE).replace(/^[~^]/, '') || UNKNOWN_LICENSE;
}

async function readJson(path) {
  return JSON.parse(await readFile(path, 'utf8'));
}

async function writeJson(fileName, data) {
  await writeFile(join(OUTPUT_DIR, fileName), `${JSON.stringify(data, null, 2)}\n`, 'utf8');
}
