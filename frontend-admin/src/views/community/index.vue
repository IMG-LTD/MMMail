<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  NButton,
  NCard,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NList,
  NListItem,
  NSelect,
  NSpace,
  NTag,
  NThing
} from 'naive-ui';
import { EmptyState } from '@/components/feedback';
import {
  createCommunityComment,
  createCommunityPost,
  createCommunityReport,
  listCommunityComments,
  listCommunityPosts,
  listCommunityTopics,
  toggleCommunityPostBookmark,
  toggleCommunityPostLike
} from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Community' });

const topics = ref<Api.Community.Topic[]>([]);
const posts = ref<Api.Community.Post[]>([]);
const comments = ref<Api.Community.Comment[]>([]);
const selectedPost = ref<Api.Community.Post | null>(null);
const postForm = reactive({ bodyMd: '', tagsText: '', title: '', topicId: 'tp_general' });
const commentBody = ref('');
const reportDetail = ref('');

const topicOptions = computed(() => topics.value.map(topic => ({ label: topic.title, value: topic.id })));

async function loadCommunity() {
  await Promise.all([loadTopics(), loadPosts()]);
}

async function loadTopics() {
  const { data, error } = await listCommunityTopics();
  if (!error) {
    topics.value = data;
    postForm.topicId = data[0]?.id || 'tp_general';
  }
}

async function loadPosts() {
  const { data, error } = await listCommunityPosts({ sort: 'latest', size: 20 });
  if (!error) {
    posts.value = data.items;
    selectedPost.value ||= data.items[0] || null;
    if (selectedPost.value) {
      await loadComments(selectedPost.value.id);
    }
  }
}

async function selectPost(post: Api.Community.Post) {
  selectedPost.value = post;
  await loadComments(post.id);
}

async function submitPost() {
  const { data, error } = await createCommunityPost({
    bodyMd: postForm.bodyMd,
    tags: parseTags(postForm.tagsText),
    title: postForm.title,
    topicId: postForm.topicId
  });
  if (!error) {
    selectedPost.value = data;
    resetPostForm();
    await loadPosts();
  }
}

async function submitComment(parentCommentId?: string) {
  if (!selectedPost.value || !commentBody.value.trim()) {
    return;
  }
  const { error } = await createCommunityComment(selectedPost.value.id, {
    bodyMd: commentBody.value,
    parentCommentId
  });
  if (!error) {
    commentBody.value = '';
    await loadComments(selectedPost.value.id);
    await loadPosts();
  }
}

async function likePost() {
  if (!selectedPost.value) {
    return;
  }
  const { data, error } = await toggleCommunityPostLike(selectedPost.value.id);
  if (!error) {
    selectedPost.value = { ...selectedPost.value, likeCount: data.likeCount };
  }
}

async function bookmarkPost() {
  if (!selectedPost.value) {
    return;
  }
  await toggleCommunityPostBookmark(selectedPost.value.id);
}

async function reportPost() {
  if (!selectedPost.value) {
    return;
  }
  const { error } = await createCommunityReport({
    detail: reportDetail.value,
    reason: 'spam',
    targetId: selectedPost.value.id,
    targetType: 'post'
  });
  if (!error) {
    reportDetail.value = '';
  }
}

async function loadComments(postId: string) {
  const { data, error } = await listCommunityComments(postId);
  if (!error) {
    comments.value = data;
  }
}

function parseTags(value: string) {
  return value
    .split(',')
    .map(tag => tag.trim())
    .filter(Boolean);
}

function resetPostForm() {
  postForm.title = '';
  postForm.bodyMd = '';
  postForm.tagsText = '';
}

onMounted(loadCommunity);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:10">
      <NCard class="card-wrapper" :title="$t('page.community.newPost')">
        <NForm :model="postForm" label-placement="top">
          <NFormItem path="topicId" :label="$t('page.community.topic')">
            <NSelect v-model:value="postForm.topicId" :options="topicOptions" />
          </NFormItem>
          <NFormItem path="title" :label="$t('page.community.postTitle')">
            <NInput v-model:value="postForm.title" />
          </NFormItem>
          <NFormItem path="tagsText" :label="$t('page.community.tags')">
            <NInput v-model:value="postForm.tagsText" />
          </NFormItem>
          <NFormItem path="bodyMd" :label="$t('page.community.body')">
            <NInput v-model:value="postForm.bodyMd" type="textarea" :autosize="{ minRows: 6 }" />
          </NFormItem>
          <NSpace justify="end">
            <NButton type="primary" @click="submitPost">{{ $t('page.community.publish') }}</NButton>
          </NSpace>
        </NForm>
      </NCard>

      <NCard class="card-wrapper mt-16px" :title="$t('page.community.posts')">
        <NList v-if="posts.length" clickable hoverable>
          <NListItem v-for="post in posts" :key="post.id" @click="selectPost(post)">
            <NThing :title="post.title" :description="post.updatedAt">
              <NSpace size="small">
                <NTag v-for="tag in post.tags" :key="tag" size="small">{{ tag }}</NTag>
              </NSpace>
            </NThing>
          </NListItem>
        </NList>
        <EmptyState v-else :description="$t('page.community.empty')" />
      </NCard>
    </NGi>

    <NGi span="24 m:14">
      <NCard class="card-wrapper" :title="selectedPost?.title || $t('page.community.title')">
        <template v-if="selectedPost">
          <NSpace class="mb-12px" size="small">
            <NTag>{{ selectedPost.status }}</NTag>
            <NTag>{{ selectedPost.likeCount }} {{ $t('page.community.like') }}</NTag>
            <NTag>{{ selectedPost.commentCount }} {{ $t('page.community.comment') }}</NTag>
          </NSpace>
          <div class="min-h-180px whitespace-pre-wrap text-14px leading-6">{{ selectedPost.bodyHtml }}</div>
          <NSpace class="mt-16px">
            <NButton @click="likePost">{{ $t('page.community.like') }}</NButton>
            <NButton @click="bookmarkPost">{{ $t('page.community.bookmark') }}</NButton>
          </NSpace>

          <NForm class="mt-16px" label-placement="top">
            <NFormItem :label="$t('page.community.comment')">
              <NInput v-model:value="commentBody" type="textarea" :autosize="{ minRows: 3 }" />
            </NFormItem>
            <NSpace justify="end">
              <NButton type="primary" @click="submitComment()">{{ $t('page.community.send') }}</NButton>
            </NSpace>
          </NForm>

          <NList class="mt-16px">
            <NListItem v-for="comment in comments" :key="comment.id">
              <NThing :description="comment.createdAt">
                <template #header>{{ comment.bodyMd }}</template>
                <div v-for="reply in comment.replies" :key="reply.id" class="mt-8px pl-12px text-13px opacity-80">
                  {{ reply.bodyMd }}
                </div>
              </NThing>
            </NListItem>
          </NList>

          <NForm class="mt-16px" label-placement="top">
            <NFormItem :label="$t('page.community.report')">
              <NInput v-model:value="reportDetail" />
            </NFormItem>
            <NSpace justify="end">
              <NButton tertiary type="warning" @click="reportPost">{{ $t('page.community.report') }}</NButton>
            </NSpace>
          </NForm>
        </template>
        <EmptyState v-else :description="$t('page.community.empty')" />
      </NCard>
    </NGi>
  </NGrid>
</template>
