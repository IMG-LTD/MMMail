import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 community service exposes posts, comments, reactions, bookmarks, reports, and topics', async () => {
  const [service, apiIndex, types] = await Promise.all([
    read('src/service/api/community.ts'),
    read('src/service/api/index.ts'),
    read('src/typings/api/expanded.d.ts')
  ]);

  for (const fn of [
    'listCommunityTopics',
    'updateCommunityTopic',
    'deleteCommunityTopic',
    'listCommunityPosts',
    'createCommunityPost',
    'updateCommunityPost',
    'deleteCommunityPost',
    'listCommunityComments',
    'createCommunityComment',
    'toggleCommunityPostLike',
    'toggleCommunityPostBookmark',
    'recordCommunityPostView',
    'listCommunityBookmarks',
    'createCommunityReport'
  ]) {
    assert.match(service, new RegExp(fn));
  }
  assert.match(service, /\/api\/v1\/community\/posts/);
  assert.match(service, /\/api\/v1\/community\/reports/);
  assert.match(service, /\/api\/v1\/community\/me\/bookmarks/);
  assert.match(apiIndex, /export \* from '\.\/community'/);
  assert.match(types, /namespace Community/);
  assert.match(types, /interface Post/);
  assert.match(types, /interface Comment/);
  assert.match(types, /interface Report/);
});

test('v2.1.2 community page replaces unavailable placeholder with real workflow bindings', async () => {
  const [page, zhCN, enUS] = await Promise.all([
    read('src/views/community/index.vue'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const token of [
    'listCommunityTopics',
    'listCommunityPosts',
    'createCommunityPost',
    'listCommunityComments',
    'createCommunityComment',
    'toggleCommunityPostLike',
    'toggleCommunityPostBookmark',
    'createCommunityReport'
  ]) {
    assert.match(page, new RegExp(token));
  }
  assert.doesNotMatch(page, /NResult/);
  assert.doesNotMatch(page, /communityUnavailable/);
  for (const source of [zhCN, enUS]) {
    for (const key of ['posts', 'newPost', 'comment', 'report', 'bookmark']) {
      assert.match(source, new RegExp(`${key}:`));
    }
  }
});
