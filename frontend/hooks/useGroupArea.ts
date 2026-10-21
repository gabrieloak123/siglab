"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError, groupsApi, messageOf } from "@/lib/api";
import { readStoredUser } from "@/lib/storage";
import type { GroupMember, ResearchGroup, User } from "@/types";

export interface GroupArea {
  user: User | null;
  group: ResearchGroup | null;
  members: GroupMember[];
  isLeader: boolean;
  loading: boolean;
  error: string;
  reload: () => Promise<void>;
}

export const useGroupArea = (groupId: string): GroupArea => {
  const [user, setUser] = useState<User | null>(null);
  const [group, setGroup] = useState<ResearchGroup | null>(null);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    setError("");
    setLoading(true);

    try {
      const stored = readStoredUser();
      setUser(stored);

      // Grupo e membros são endpoints públicos: carregamos ambos também para
      // visitantes anônimos, que podem consultar os posts do grupo.
      const [current, groupMembers] = await Promise.all([
        groupsApi.get(groupId),
        groupsApi.members(groupId),
      ]);

      if (!current) {
        setError("Grupo de pesquisa não encontrado.");
        return;
      }

      setGroup(current);
      setMembers(groupMembers);
    } catch (caught) {
      setError(
        caught instanceof ApiError && caught.status === 404
          ? "Grupo de pesquisa não encontrado."
          : messageOf(caught, {
              api: "Não foi possível carregar os dados do grupo de pesquisa.",
              network: "Falha de conexão com o servidor.",
            })
      );
    } finally {
      setLoading(false);
    }
  }, [groupId]);

  useEffect(() => {
    void (async () => {
      await reload();
    })();
  }, [reload]);

  return {
    user,
    group,
    members,
    isLeader: members.some(
      (member) => member.id === user?.id && member.isLeader
    ),
    loading,
    error,
    reload,
  };
};
