import { describe, expect, it } from 'vitest';
import * as Y from 'yjs';

describe('docs collab CRDT merge', () => {
  it('merges concurrent edits from two clients without losing characters', () => {
    const docA = new Y.Doc();
    const docB = new Y.Doc();
    const textA = docA.getText('content');
    const textB = docB.getText('content');

    textA.insert(0, 'Hello');
    Y.applyUpdate(docB, Y.encodeStateAsUpdate(docA));

    textA.insert(textA.length, ', world');
    textB.insert(textB.length, '!! 你好');

    const updateFromA = Y.encodeStateAsUpdate(docA, Y.encodeStateVector(docB));
    const updateFromB = Y.encodeStateAsUpdate(docB, Y.encodeStateVector(docA));

    Y.applyUpdate(docB, updateFromA);
    Y.applyUpdate(docA, updateFromB);

    expect(textA.toString()).toBe(textB.toString());
    expect(textA.toString()).toContain('Hello');
    expect(textA.toString()).toContain(', world');
    expect(textA.toString()).toContain('!! 你好');
  });

  it('encodes and decodes snapshot bytes round-trip', () => {
    const original = new Y.Doc();
    original.getText('content').insert(0, 'snapshot baseline');
    const snapshot = Y.encodeStateAsUpdate(original);

    const restored = new Y.Doc();
    Y.applyUpdate(restored, snapshot);

    expect(restored.getText('content').toString()).toBe('snapshot baseline');
  });
});
