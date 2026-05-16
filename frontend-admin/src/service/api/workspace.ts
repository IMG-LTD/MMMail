import { request } from '../request';

export function readWorkspaceSummary() {
  return request<Api.Workspace.Summary>({ url: '/api/v2/workspace/summary' });
}

export function listWorkspaceActivity() {
  return request<Api.Workspace.ActivityItem[]>({ url: '/api/v2/workspace/activity' });
}

export function listWorkspaceTasks() {
  return request<Api.Workspace.Task[]>({ url: '/api/v2/workspace/tasks' });
}

export function patchWorkspaceTask(taskId: string, data: Api.Workspace.PatchTaskPayload) {
  return request<Api.Workspace.Task>({
    url: `/api/v2/workspace/tasks/${taskId}`,
    method: 'patch',
    data
  });
}
