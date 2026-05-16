import { readFileSync, writeFileSync } from 'node:fs';

const generatedTypeFile = new URL('../src/typings/elegant-router.d.ts', import.meta.url);

const source = readFileSync(generatedTypeFile, 'utf8');
const normalized = source.replace(/[ \t]+$/gm, '');

if (normalized !== source) {
  writeFileSync(generatedTypeFile, normalized);
}
