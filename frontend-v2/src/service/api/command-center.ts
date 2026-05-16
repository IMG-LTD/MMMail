import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

interface ScopedRequestOptions {
  scopeHeaders?: Record<string, string>;
  token: string;
}

export interface CommandCenterCommand {
  id: string;
  name: string;
  description: string;
  product: string;
  enabled: boolean;
  parameterCount: number;
}

export interface CommandCenterRun {
  id: string;
  commandId: string;
  status: string;
  startedAt: string;
  finishedAt: string | null;
  logTail: string[];
}

export interface CommandCenterWorkflow {
  id: string;
  name: string;
  status: string;
  stepCount: number;
  updatedAt: string;
}

export interface CommandCenterAuditEntry {
  id: string;
  action: string;
  actorEmail: string;
  status: string;
  createdAt: string;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data;
}

export async function listCommandCenterCommands(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CommandCenterCommand[]>>(
    "/api/v2/command-center/commands",
    options,
  );
  return unwrapResponse(response);
}

export async function readCommandCenterCommand(commandId: string, options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CommandCenterCommand>>(
    `/api/v2/command-center/commands/${commandId}`,
    options,
  );
  return unwrapResponse(response);
}

export async function createCommandCenterRun(
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  const response = await httpClient.post<ApiResponse<CommandCenterRun>>(
    "/api/v2/command-center/runs",
    { ...options, body },
  );
  return unwrapResponse(response);
}

export async function readCommandCenterRun(runId: string, options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CommandCenterRun>>(
    `/api/v2/command-center/runs/${runId}`,
    options,
  );
  return unwrapResponse(response);
}

export function cancelCommandCenterRun(runId: string, options: ScopedRequestOptions) {
  return httpClient.post<ApiResponse<CommandCenterRun>>(
    `/api/v2/command-center/runs/${runId}/cancel`,
    options,
  );
}

export function retryCommandCenterRun(runId: string, options: ScopedRequestOptions) {
  return httpClient.post<ApiResponse<CommandCenterRun>>(
    `/api/v2/command-center/runs/${runId}/retry`,
    options,
  );
}

export async function listCommandCenterWorkflows(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CommandCenterWorkflow[]>>(
    "/api/v2/command-center/workflows",
    options,
  );
  return unwrapResponse(response);
}

export function createCommandCenterWorkflow(
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.post<ApiResponse<CommandCenterWorkflow>>("/api/v2/command-center/workflows", {
    ...options,
    body,
  });
}

export async function listCommandCenterAudit(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<CommandCenterAuditEntry[]>>(
    "/api/v2/command-center/audit",
    options,
  );
  return unwrapResponse(response);
}
