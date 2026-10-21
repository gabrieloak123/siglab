import { isSameEmail } from "@/lib/utils";
import type { ConferenceSuggestion, GooglePreview, GroupMember, KanbanBoard, KanbanTask, Post, ResearchGroup, User } from "@/types";

const DEFAULT_API_URL = "/backend-api";
export const getApiUrl = () => DEFAULT_API_URL;
export class ApiError extends Error {
  readonly serverMessage?: string;
  constructor(readonly status: number, serverMessage?: string) {
    super(serverMessage ?? "Erro inesperado na comunicação com o servidor.");
    this.name = "ApiError";
    this.serverMessage = serverMessage;
  }
}
export const messageOf = (error: unknown, messages: { api: string; network: string }) =>
  error instanceof ApiError ? (error.serverMessage ?? messages.api) : messages.network;
const readServerMessage = async (res: Response) => {
  const data = await res.json().catch(() => null);
  return data?.message as string | undefined;
};
const jsonBody = (payload: unknown, extraHeaders: Record<string, string> = {}): RequestInit => ({
  headers: { "Content-Type": "application/json", ...extraHeaders }, body: JSON.stringify(payload),
});
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${getApiUrl()}${path}`, { credentials: "include", ...init });
  if (!res.ok) throw new ApiError(res.status, await readServerMessage(res));
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
export const usersApi = {
  list: () => request<User[]>("/api/users"),
  findByEmail: async (email: string) => (await usersApi.list()).find(user => isSameEmail(user.email, email)) ?? null,
  create: (payload: Omit<User, "id">) => request<User>("/api/users", { method: "POST", ...jsonBody(payload) }),
  update: (id: number, payload: Partial<User>) => request<User>(`/api/users/${id}`, { method: "PUT", ...jsonBody(payload) }),
  remove: (id: number, password: string) => request<void>(`/api/users/${id}?password=${encodeURIComponent(password)}`, { method: "DELETE" }),
};
export const authApi = {
  me: () => request<User>("/api/auth/me"),
  login: (email: string, password: string) => request<User>("/api/auth/login", { method: "POST", ...jsonBody({ email, password }) }),
  register: (payload: { name: string; email: string; password: string; interestAreas: string[] }) => request<User>("/api/auth/register", { method: "POST", ...jsonBody(payload) }),
  google: (credential: string, interestAreas: string[]) => request<User>("/api/auth/google", { method: "POST", ...jsonBody({ credential, interestAreas }) }),
  googlePreview: (credential: string) => request<GooglePreview>("/api/auth/google/preview", { method: "POST", ...jsonBody({ credential }) }),
  linkGoogle: (credential: string) => request<User>("/api/auth/google/link", { method: "POST", ...jsonBody({ credential }) }),
  orcidStatus: () => request<{ configured: boolean }>("/api/auth/orcid/status"),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
};
export const socialApi = {
  recentPosts: () => request<Post[]>("/api/social/recent-posts"),
  researchers: () => request<User[]>("/api/social/suggestions/researchers"),
  groups: () => request<ResearchGroup[]>("/api/social/suggestions/groups"),
  conferences: () => request<ConferenceSuggestion[]>("/api/social/suggestions/conferences"),
  conferenceCountries: () => request<string[]>("/api/social/conferences/countries"),
  searchConferences: (params: { start: string; end: string; field: string; country?: string }) => {
    const query = new URLSearchParams({ start: params.start, end: params.end, field: params.field });
    if (params.country) query.set("country", params.country);
    return request<ConferenceSuggestion[]>(`/api/social/conferences/search?${query.toString()}`);
  },
  feed: (followedOnly = false) => request<Post[]>(`/api/social/feed?followedOnly=${followedOnly}`),
  following: () => request<{ researchers: number[]; researcherFollows: ResearcherFollow[]; groups: string[] }>("/api/social/following"),
  followResearcher: (id: number, interestAreas?: string[]) => request<void>(`/api/social/researchers/${id}/follow`, { method: "POST", ...jsonBody({ interestAreas: interestAreas ?? [] }) }),
  unfollowResearcher: (id: number) => request<void>(`/api/social/researchers/${id}/follow`, { method: "DELETE" }),
  followGroup: (id: string) => request<void>(`/api/social/groups/${encodeURIComponent(id)}/follow`, { method: "POST" }),
  unfollowGroup: (id: string) => request<void>(`/api/social/groups/${encodeURIComponent(id)}/follow`, { method: "DELETE" }),
  postLike: (id: number) => request<{ liked: boolean; count: number }>(`/api/social/posts/${id}/like`),
  likePost: (id: number) => request<{ liked: boolean; count: number }>(`/api/social/posts/${id}/like`, { method: "POST" }),
  unlikePost: (id: number) => request<{ liked: boolean; count: number }>(`/api/social/posts/${id}/like`, { method: "DELETE" }),
};
export interface ResearcherFollow { researcherId: number; interestAreas: string[] }
export interface DiscoveryPost { id: number; title: string; description: string | null; groupName: string; keywords: string[]; tags: string[]; indexed: boolean }
export const discoveryApi = {
  posts: (page: number) => request<{ posts: DiscoveryPost[]; hasMore: boolean }>(`/api/discovery/posts?page=${page}`),
  save: (postIds: number[]) => request<void>("/api/discovery/selection", { method: "POST", ...jsonBody({ postIds }) }),
};
export type InviteTokenResponse = { inviteToken: string };
const groupPath = (id: string) => `/api/research-groups/${encodeURIComponent(id)}`;
export const groupsApi = {
  list: () => request<ResearchGroup[]>("/api/research-groups"),
  get: (id: string) => request<ResearchGroup>(groupPath(id)),
  create: (payload: ResearchGroup) => request<ResearchGroup>("/api/research-groups", { method: "POST", ...jsonBody(payload) }),
  update: (id: string, payload: Omit<ResearchGroup, "group_id">) => request<ResearchGroup>(groupPath(id), { method: "PUT", ...jsonBody(payload) }),
  remove: (id: string, password: string, email: string) => request<void>(`${groupPath(id)}?leaderPassword=${encodeURIComponent(password)}`, { method: "DELETE", headers: { "X-Requester-Email": email } }),
  members: (id: string) => request<GroupMember[]>(`${groupPath(id)}/users`),
  createInvite: (id: string, email: string, days=7) => request<InviteTokenResponse>(`${groupPath(id)}/invites?expirationDays=${days}`, { method: "POST", headers: { "X-Requester-Email": email } }),
  join: (token: string, email: string) => request<GroupMember>("/api/research-groups/join", { method: "POST", ...jsonBody({ inviteToken: token, userEmail: email }) }),
  setLeader: (id: string, email: string, leader: boolean) => request<GroupMember>(`${groupPath(id)}/members/${encodeURIComponent(email)}/leader?isLeader=${leader}`, { method: "PUT" }),
};
export interface CreateTaskPayload { id_atribuido_a: number | null; id_coluna: number; titulo: string; descricao: string | null; tags: string[] }
export interface UpdateTaskPayload { id_atribuido_a: number | null; titulo: string; descricao: string | null; tags: string[] }
export const kanbanApi = {
  board: (id: string) => request<KanbanBoard>(`${groupPath(id)}/board`),
  createTask: (id: string, payload: CreateTaskPayload) => request<KanbanTask>(`${groupPath(id)}/tasks`, { method: "POST", ...jsonBody(payload) }),
  updateTask: (id: string, task: number, payload: UpdateTaskPayload) => request<KanbanTask>(`${groupPath(id)}/tasks/${task}`, { method: "PUT", ...jsonBody(payload) }),
  moveTask: (id: string, task: number, column: number) => request<KanbanTask>(`${groupPath(id)}/tasks/${task}/column`, { method: "PATCH", ...jsonBody({ id_coluna_destino: column }) }),
  removeTask: (id: string, task: number) => request<void>(`${groupPath(id)}/tasks/${task}`, { method: "DELETE" }),
};
export interface PostPayload { title: string; description: string | null; content: string; tags: string[]; filePath: string | null; postType: "ARTIGO" | "OUTROS"; keywords: string[] }
const requesterHeaders = (email: string) => ({ "X-Requester-Email": email });
const optionalRequesterHeaders = (email?: string | null) =>
  email ? requesterHeaders(email) : undefined;
export const postsApi = {
  list: (id: string, email?: string | null) => request<Post[]>(`${groupPath(id)}/posts`, { headers: optionalRequesterHeaders(email) }),
  get: (id: string, post: number, email?: string | null) => request<Post>(`${groupPath(id)}/posts/${post}`, { headers: optionalRequesterHeaders(email) }),
  create: (id: string, email: string, payload: PostPayload) => request<Post>(`${groupPath(id)}/posts`, { method: "POST", ...jsonBody(payload, requesterHeaders(email)) }),
  update: (id: string, post: number, email: string, payload: PostPayload) => request<Post>(`${groupPath(id)}/posts/${post}`, { method: "PUT", ...jsonBody(payload, requesterHeaders(email)) }),
  remove: (id: string, post: number, email: string) => request<void>(`${groupPath(id)}/posts/${post}`, { method: "DELETE", headers: requesterHeaders(email) }),
};
