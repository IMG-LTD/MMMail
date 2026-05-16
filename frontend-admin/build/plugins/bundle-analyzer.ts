import { gzipSync } from 'node:zlib';
import type { PluginOption } from 'vite';

type BundleFile = {
  fileName: string;
  gzipBytes: number;
  rawBytes: number;
  type: 'asset' | 'chunk';
};

export function setupBundleAnalyzer(): PluginOption {
  return {
    apply: 'build',
    generateBundle(_, bundle) {
      const files = Object.entries(bundle)
        .map(([fileName, output]) => toBundleFile(fileName, output.type, output))
        .sort((left, right) => left.fileName.localeCompare(right.fileName));

      this.emitFile({
        type: 'asset',
        fileName: 'bundle-analyzer.json',
        source: JSON.stringify({ files }, null, 2)
      });
    },
    name: 'vite-plugin-bundle-analyzer'
  };
}

function toBundleFile(fileName: string, type: 'asset' | 'chunk', output: unknown): BundleFile {
  const source = type === 'asset' ? assetSource(output) : chunkSource(output);
  const buffer = Buffer.isBuffer(source) ? source : Buffer.from(source);

  return {
    fileName,
    gzipBytes: gzipSync(buffer).length,
    rawBytes: buffer.length,
    type
  };
}

function assetSource(output: unknown): string | Uint8Array {
  return (output as { source: string | Uint8Array }).source;
}

function chunkSource(output: unknown): string {
  return (output as { code: string }).code;
}
