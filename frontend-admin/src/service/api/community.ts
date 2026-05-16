import { request } from '../request';

type CommunityPostPayload = {
  bodyMd: string;
  tags: string[];
  title: string;
  topicId?: string;
};

export function listCommunityTopics() {
  return request<Api.Community.Topic[]>({ url: '/api/v1/community/topics' });
}

export function updateCommunityTopic(topicId: string, data: { description?: string; slug?: string; title?: string }) {
  return request<Api.Community.Topic>({
    url: `/api/v1/community/topics/${topicId}`,
    method: 'patch',
    data
  });
}

export function deleteCommunityTopic(topicId: string) {
  return request<Api.Community.TopicDelete>({
    url: `/api/v1/community/topics/${topicId}`,
    method: 'delete'
  });
}

export function listCommunityTags(params: { limit?: number } = {}) {
  return request<string[]>({ url: '/api/v1/community/tags', params });
}

export function listCommunityPosts(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Community.PostPage>({ url: '/api/v1/community/posts', params });
}

export function createCommunityPost(data: CommunityPostPayload) {
  return request<Api.Community.Post>({
    url: '/api/v1/community/posts',
    method: 'post',
    data
  });
}

export function updateCommunityPost(postId: string, data: CommunityPostPayload) {
  return request<Api.Community.Post>({
    url: `/api/v1/community/posts/${postId}`,
    method: 'patch',
    data
  });
}

export function deleteCommunityPost(postId: string) {
  return request<Api.Community.Post>({
    url: `/api/v1/community/posts/${postId}`,
    method: 'delete'
  });
}

export function readCommunityPost(postId: string) {
  return request<Api.Community.Post>({ url: `/api/v1/community/posts/${postId}` });
}

export function listCommunityComments(postId: string) {
  return request<Api.Community.Comment[]>({ url: `/api/v1/community/posts/${postId}/comments` });
}

export function createCommunityComment(postId: string, data: { bodyMd: string; parentCommentId?: string }) {
  return request<Api.Community.Comment>({
    url: `/api/v1/community/posts/${postId}/comments`,
    method: 'post',
    data
  });
}

export function toggleCommunityPostLike(postId: string) {
  return request<Api.Community.Reaction>({
    url: `/api/v1/community/posts/${postId}/like`,
    method: 'post'
  });
}

export function toggleCommunityPostBookmark(postId: string) {
  return request<Api.Community.Bookmark>({
    url: `/api/v1/community/posts/${postId}/bookmark`,
    method: 'post'
  });
}

export function recordCommunityPostView(postId: string) {
  return request<Api.Community.ViewCount>({
    url: `/api/v1/community/posts/${postId}/view`,
    method: 'post'
  });
}

export function listCommunityBookmarks(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Community.PostPage>({ url: '/api/v1/community/me/bookmarks', params });
}

export function createCommunityReport(data: { detail?: string; reason: string; targetId: string; targetType: string }) {
  return request<Api.Community.Report>({
    url: '/api/v1/community/reports',
    method: 'post',
    data
  });
}
