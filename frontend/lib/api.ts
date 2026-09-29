import { isSameEmail } from "@/lib/utils";
import type {
  GroupMember,
  KanbanBoard,
  KanbanTask,
  Post,
  ResearchGroup,
  User,
} from "@/types";

const DEFAULT_API_URL = "/backend-api";

export const getApiUrl = () => DEFAULT_API_URL;

export class ApiError extends Error {
  readonly serverMessage?: string;

  constructor(
    readonly status: number,
    serverMessage?: string
  ) {
    super(serverMessage ?? "Erro inesperado na comunicação com o servidor.");
    this.name = "ApiError";
    this.serverMessage = serverMessage;
  }
}

export const messageOf = (
  error: unknown,
  messages: { api: string; network: string }
) =>
  error instanceof ApiError
    ? (error.serverMessage ?? messages.api)
    : messages.network;

const readServerMessage = async (res: Response) => {
  const data = await res.json().catch(() => null);
  return data?.message as string | undefined;
};

const jsonBody = (
  payload: unknown,
  extraHeaders: Record<string, string> = {}
): RequestInit => ({
  headers: { "Content-Type": "application/json", ...extraHeaders },
  body: JSON.stringify(payload),
});

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${getApiUrl()}${path}`, init);

  if (!res.ok) {
    throw new ApiError(res.status, await readServerMessage(res));
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return (await res.json()) as T;
}

export const usersApi = {
  list: () => request<User[]>("/api/users"),

  findByEmail: async (email: string) => {
    const users = await usersApi.list();
    return users.find((user) => isSameEmail(user.email, email)) ?? null;
  },

  create: (payload: Omit<User, "id">) =>
    request<User>("/api/users", { method: "POST", ...jsonBody(payload) }),

  update: (id: number, payload: Partial<User>) =>
    request<User>(`/api/users/${id}`, { method: "PUT", ...jsonBody(payload) }),

  remove: (id: number, password: string) =>
    request<void>(`/api/users/${id}?password=${encodeURIComponent(password)}`, {
      method: "DELETE",
    }),
};

export type InviteTokenResponse = { inviteToken: string };

export const groupsApi = {
  list: () => request<ResearchGroup[]>("/api/research-groups"),

  create: (payload: ResearchGroup) =>
    request<ResearchGroup>("/api/research-groups", {
      method: "POST",
      ...jsonBody(payload),
    }),

  update: (groupId: string, payload: Omit<ResearchGroup, "group_id">) =>
    request<ResearchGroup>(
      `/api/research-groups/${encodeURIComponent(groupId)}`,
      { method: "PUT", ...jsonBody(payload) }
    ),

  remove: (groupId: string, password: string, requesterEmail: string) =>
    request<void>(
      `/api/research-groups/${encodeURIComponent(groupId)}?leaderPassword=${encodeURIComponent(password)}`,
      { method: "DELETE", headers: { "X-Requester-Email": requesterEmail } }
    ),

  members: (groupId: string) =>
    request<GroupMember[]>(
      `/api/research-groups/${encodeURIComponent(groupId)}/users`
    ),

  createInvite: (groupId: string, requesterEmail: string, expirationDays = 7) =>
    request<InviteTokenResponse>(
      `/api/research-groups/${encodeURIComponent(groupId)}/invites?expirationDays=${expirationDays}`,
      { method: "POST", headers: { "X-Requester-Email": requesterEmail } }
    ),

  join: (inviteToken: string, userEmail: string) =>
    request<GroupMember>("/api/research-groups/join", {
      method: "POST",
      ...jsonBody({ inviteToken, userEmail }),
    }),

  setLeader: (groupId: string, userEmail: string, isLeader: boolean) =>
    request<GroupMember>(
      `/api/research-groups/${encodeURIComponent(groupId)}/members/${encodeURIComponent(userEmail)}/leader?isLeader=${isLeader}`,
      { method: "PUT" }
    ),
};

export interface CreateTaskPayload {
  id_criado_por: number;
  id_atribuido_a: number | null;
  id_coluna: number;
  titulo: string;
  descricao: string | null;
  tags: string[];
}

export interface UpdateTaskPayload {
  id_atribuido_a: number | null;
  titulo: string;
  descricao: string | null;
  tags: string[];
}

const boardPath = (groupId: string) =>
  `/api/research-groups/${encodeURIComponent(groupId)}`;

export const kanbanApi = {
  board: (groupId: string) =>
    request<KanbanBoard>(`${boardPath(groupId)}/board`),

  createTask: (groupId: string, payload: CreateTaskPayload) =>
    request<KanbanTask>(`${boardPath(groupId)}/tasks`, {
      method: "POST",
      ...jsonBody(payload),
    }),

  updateTask: (groupId: string, taskId: number, payload: UpdateTaskPayload) =>
    request<KanbanTask>(`${boardPath(groupId)}/tasks/${taskId}`, {
      method: "PUT",
      ...jsonBody(payload),
    }),

  moveTask: (groupId: string, taskId: number, columnId: number) =>
    request<KanbanTask>(`${boardPath(groupId)}/tasks/${taskId}/column`, {
      method: "PATCH",
      ...jsonBody({ id_coluna_destino: columnId }),
    }),

  removeTask: (groupId: string, taskId: number) =>
    request<void>(`${boardPath(groupId)}/tasks/${taskId}`, {
      method: "DELETE",
    }),
};

export interface PostPayload {
  title: string;
  description: string | null;
  content: string;
  tags: string[];
  filePath: string | null;
}

const requesterHeaders = (requesterEmail: string) => ({
  "X-Requester-Email": requesterEmail,
});

export const postsApi = {
  list: (groupId: string, requesterEmail: string) =>
    request<Post[]>(`${boardPath(groupId)}/posts`, {
      headers: requesterHeaders(requesterEmail),
    }),

  get: (groupId: string, postId: number, requesterEmail: string) =>
    request<Post>(`${boardPath(groupId)}/posts/${postId}`, {
      headers: requesterHeaders(requesterEmail),
    }),

  create: (groupId: string, requesterEmail: string, payload: PostPayload) =>
    request<Post>(`${boardPath(groupId)}/posts`, {
      method: "POST",
      ...jsonBody(payload, requesterHeaders(requesterEmail)),
    }),

  update: (
    groupId: string,
    postId: number,
    requesterEmail: string,
    payload: PostPayload
  ) =>
    request<Post>(`${boardPath(groupId)}/posts/${postId}`, {
      method: "PUT",
      ...jsonBody(payload, requesterHeaders(requesterEmail)),
    }),

  remove: (groupId: string, postId: number, requesterEmail: string) =>
    request<void>(`${boardPath(groupId)}/posts/${postId}`, {
      method: "DELETE",
      headers: requesterHeaders(requesterEmail),
    }),
};
