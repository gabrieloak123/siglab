import { isSameEmail } from "@/lib/utils";
import type { GroupMember, ResearchGroup, User } from "@/types";

const DEFAULT_API_URL = "http://localhost:8080";

export const getApiUrl = () => process.env.NEXT_PUBLIC_API_URL || DEFAULT_API_URL;

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

const jsonBody = (payload: unknown): RequestInit => ({
  headers: { "Content-Type": "application/json" },
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
