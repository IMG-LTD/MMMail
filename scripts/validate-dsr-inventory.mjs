#!/usr/bin/env node
import { readdirSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { join } from 'node:path';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const MIGRATION_DIR = join(ROOT, 'backend/mmmail-server/src/main/resources/db/migration');
const SCHEMA_FILE = join(ROOT, 'backend/mmmail-server/src/main/resources/schema.sql');
const INVENTORY_FILE = join(ROOT, 'docs/compliance/data-inventory.yaml');
const REQUIRED_FIELDS = ['owner:', 'retention:', 'subjectRef:', 'export:', 'delete:'];

function read(path) {
  return readFileSync(path, 'utf8');
}

function declaredTables() {
  const sqlFiles = readdirSync(MIGRATION_DIR)
    .filter(name => /^V\d+__.*\.sql$/.test(name))
    .map(name => join(MIGRATION_DIR, name));
  return new Set([SCHEMA_FILE, ...sqlFiles].flatMap(path => extractTables(read(path))));
}

function extractTables(sql) {
  const tables = [];
  const pattern = /\bcreate\s+table\s+(?:if\s+not\s+exists\s+)?`?([a-zA-Z0-9_]+)`?/gi;
  let match = pattern.exec(sql);
  while (match !== null) {
    tables.push(match[1]);
    match = pattern.exec(sql);
  }
  return tables;
}

function inventoryEntries() {
  const entries = new Map();
  const pattern = /^\s{2}([a-z0-9_]+):\s*\{([^}]+)\}\s*$/gm;
  const inventory = read(INVENTORY_FILE);
  let match = pattern.exec(inventory);
  while (match !== null) {
    entries.set(match[1], match[2]);
    match = pattern.exec(inventory);
  }
  return entries;
}

function validateFields(entries) {
  const invalid = [];
  for (const [table, metadata] of entries) {
    const missing = REQUIRED_FIELDS.filter(field => !metadata.includes(field));
    if (missing.length > 0) {
      invalid.push(`${table}: missing ${missing.join(', ')}`);
    }
  }
  return invalid;
}

function main() {
  const tables = declaredTables();
  const entries = inventoryEntries();
  const missing = [...tables].filter(table => !entries.has(table)).sort();
  const invalid = validateFields(entries);
  if (missing.length > 0 || invalid.length > 0) {
    console.error('DSR data inventory validation failed.');
    if (missing.length > 0) console.error(`Missing tables: ${missing.join(', ')}`);
    if (invalid.length > 0) console.error(`Invalid entries: ${invalid.join('; ')}`);
    process.exit(1);
  }
  console.log(`validated ${entries.size} data inventory entries`);
}

main();
