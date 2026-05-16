import { request } from '../request';

export function listCollaborationProjects(params: Record<string, number | undefined> = {}) {
  return request<Api.Collaboration.Project[]>({ url: '/api/v2/collaboration/projects', params });
}

export function createCollaborationProject(data: { name: string; product: string; status: string }) {
  return request<Api.Collaboration.Project>({
    url: '/api/v2/collaboration/projects',
    method: 'post',
    data
  });
}

export function listCollaborationTasks(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Collaboration.Task[]>({ url: '/api/v2/collaboration/tasks', params });
}

export function readCollaborationBoard(projectId: string) {
  return request<Api.Collaboration.Board>({ url: `/api/v2/collaboration/projects/${projectId}/board` });
}

export function moveCollaborationTask(taskId: string, data: Api.Collaboration.TaskMovePayload) {
  return request<Api.Collaboration.TaskMoveResult>({
    url: `/api/v2/collaboration/tasks/${taskId}/move`,
    method: 'patch',
    data
  });
}
