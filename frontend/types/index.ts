export interface ResearchGroup { group_id: string; name: string; research_fields?: string[] }
export interface GroupMember { id: number; email: string; name: string; interest_areas?: string[]; isLeader: boolean }
export interface User { id?: number; email: string; name: string; password?: string; interest_areas?: string[]; authorId?: string; orcid?: string; verificationLevel?: "NAO_VERIFICADO" | "INSTITUCIONAL" | "ORCID"; institutionalDomain?: string }
export interface ScholarCandidate { authorId: string; name: string; affiliation: string; verifiedDomain: string; interests: string[] }
export interface GooglePreview { existingUser: boolean; name: string; email: string; institutionalDomain?: string; candidates: ScholarCandidate[] }
export interface KanbanTask { id: number; id_grupo: string; id_criado_por: number; id_atribuido_a?: number | null; titulo: string; descricao?: string | null; tags?: string[] | null; id_coluna: number }
export interface KanbanColumn { id: number; nome: string; ordem: number; id_grupo_pesquisa: string; tarefas: KanbanTask[] }
export interface KanbanBoard { id: string; id_grupo_pesquisa: string; colunas: KanbanColumn[] }
export interface Post { id: number; groupId: string; publisherUserId?: number | null; title: string; description?: string | null; content?: string | null; tags?: string[] | null; filePath?: string | null; createdAt?: string | null; postType?: "ARTIGO" | "OUTROS"; keywords?: string[] }
export type AuthTab = "login" | "register";
