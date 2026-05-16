import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

interface ScopedRequestOptions {
  scopeHeaders?: Record<string, string>;
  token: string;
}

export interface CollaborationProject {
  id: string;
  name: string;
  status: string;
  taskCount: number;
  updatedAt: string;
}

export interface CollaborationTask {
  id: string;
  projectId: string;
  title: string;
  status: string;
  assigneeEmail: string | null;
  dueAt: string | null;
}

export interface CollaborationActivity {
  id: string;
  title: string;
  product: string;
  occurredAt: string;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data;
}

export async function listCollaborationProjects(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CollaborationProject[]>>(
    "/api/v2/collaboration/projects",
    options,
  );
  return unwrapResponse(response);
}

export function createCollaborationProject(
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.post<ApiResponse<CollaborationProject>>("/api/v2/collaboration/projects", {
    ...options,
    body,
  });
}

export async function readCollaborationProject(projectId: string, options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CollaborationProject>>(
    `/api/v2/collaboration/projects/${projectId}`,
    options,
  );
  return unwrapResponse(response);
}

export async function listCollaborationTasks(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CollaborationTask[]>>(
    "/api/v2/collaboration/tasks",
    options,
  );
  return unwrapResponse(response);
}

export function createCollaborationTask(
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.post<ApiResponse<CollaborationTask>>("/api/v2/collaboration/tasks", {
    ...options,
    body,
  });
}

export function patchCollaborationTask(
  taskId: string,
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.patch<ApiResponse<CollaborationTask>>(`/api/v2/collaboration/tasks/${taskId}`, {
    ...options,
    body,
  });
}

export function commentCollaborationTask(
  taskId: string,
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.post<ApiResponse<CollaborationTask>>(
    `/api/v2/collaboration/tasks/${taskId}/comments`,
    { ...options, body },
  );
}

export async function listCollaborationActivity(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CollaborationActivity[]>>(
    "/api/v2/collaboration/activity",
    options,
  );
  return unwrapResponse(response);
}
