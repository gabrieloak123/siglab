export interface ResearchGroup {
  group_id: string;
  name: string;
  research_fields?: string[];
}

export interface GroupMember {
  id: number;
  email: string;
  name: string;
  interest_areas?: string[];
  isLeader: boolean;
}

export interface User {
  id?: number;
  email: string;
  name: string;
  password?: string;
  interest_areas?: string[];
}

export type AuthTab = "login" | "register";
