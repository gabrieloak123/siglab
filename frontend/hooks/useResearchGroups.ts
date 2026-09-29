"use client";

import { useCallback, useEffect, useState } from "react";

import { groupsApi, messageOf } from "@/lib/api";
import { isSameEmail } from "@/lib/utils";
import type { GroupMember, ResearchGroup, User } from "@/types";
import type { Feedback } from "./useFeedback";

export interface UseResearchGroups {
  allGroups: ResearchGroup[];
  memberGroups: ResearchGroup[];
  leaderGroups: ResearchGroup[];
  generatedTokens: Record<string, string>;
  refresh: () => Promise<boolean>;
  createGroup: (payload: {
    group_id: string;
    name: string;
    research_fields: string[];
  }) => Promise<boolean>;
  joinGroup: (inviteToken: string) => Promise<boolean>;
  generateInvite: (groupId: string) => Promise<boolean>;
}

export const useResearchGroups = (
  user: User | null,
  feedback: Feedback
): UseResearchGroups => {
  const [allGroups, setAllGroups] = useState<ResearchGroup[]>([]);
  const [memberGroups, setMemberGroups] = useState<ResearchGroup[]>([]);
  const [leaderGroups, setLeaderGroups] = useState<ResearchGroup[]>([]);
  const [generatedTokens, setGeneratedTokens] = useState<Record<string, string>>(
    {}
  );

  const loadGroups = useCallback(async () => {
    const groups = await groupsApi.list();
    setAllGroups(groups);
    return groups;
  }, []);

  const loadMemberships = useCallback(
    async (userEmail: string, groups?: ResearchGroup[]) => {
      const list = groups ?? (await groupsApi.list());

      const entries = await Promise.all(
        list.map(async (group) => {
          try {
            const members: GroupMember[] = await groupsApi.members(
              group.group_id
            );
            return members.find((member) =>
              isSameEmail(member.email, userEmail)
            );
          } catch (error) {
            console.error(
              "Erro ao checar membros do grupo:",
              group.group_id,
              error
            );
            return undefined;
          }
        })
      );

      setMemberGroups(
        list.filter((_, index) => entries[index] !== undefined)
      );
      setLeaderGroups(
        list.filter((_, index) => entries[index]?.isLeader === true)
      );
    },
    []
  );

  const refresh = useCallback(async () => {
    if (!user) {
      setMemberGroups([]);
      setLeaderGroups([]);
      setGeneratedTokens({});
    }

    try {
      const groups = await loadGroups();
      if (user) {
        await loadMemberships(user.email, groups);
      }
      return true;
    } catch (error) {
      console.error("Erro ao atualizar a lista de grupos:", error);
      return false;
    }
  }, [user, loadGroups, loadMemberships]);

  const { fail } = feedback;

  useEffect(() => {
    void (async () => {
      const loaded = await refresh();
      if (!loaded) {
        fail(
          "Não foi possível carregar os grupos. Verifique se a API está em execução."
        );
      }
    })();
  }, [refresh, fail]);

  const createGroup = async (payload: {
    group_id: string;
    name: string;
    research_fields: string[];
  }) => {
    if (!user) {
      feedback.fail(
        "Você precisa estar logado para criar um grupo de pesquisa."
      );
      return false;
    }

    feedback.begin();
    try {
      const groupId = payload.group_id.trim();
      const name = payload.name.trim();

      await groupsApi.create({ ...payload, group_id: groupId, name });

      try {
        await groupsApi.setLeader(groupId, user.email, true);
      } catch {
        feedback.fail(
          "Grupo criado, mas ocorreu uma falha ao registrar o líder."
        );
        return false;
      }

      feedback.succeed(
        `Grupo de pesquisa '${name}' criado com sucesso! Você foi definido como líder/orientador.`
      );
      await refresh();
      return true;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Erro ao criar grupo de pesquisa. Verifique se o Código ID já existe.",
          network: "Falha na comunicação ao tentar criar o grupo.",
        })
      );
      return false;
    } finally {
      feedback.setLoading(false);
    }
  };

  const joinGroup = async (inviteToken: string) => {
    if (!user) {
      feedback.fail("Você precisa estar logado para entrar em um grupo.");
      return false;
    }

    feedback.begin();
    try {
      await groupsApi.join(inviteToken.trim(), user.email);
      feedback.succeed("Ingresso realizado com sucesso no grupo de pesquisa!");
      await refresh();
      return true;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Hash de convite inválida, expirada ou já utilizada.",
          network: "Erro ao tentar entrar no grupo.",
        })
      );
      return false;
    } finally {
      feedback.setLoading(false);
    }
  };

  const generateInvite = async (groupId: string) => {
    if (!user) return false;

    feedback.clear();
    try {
      const invite = await groupsApi.createInvite(groupId, user.email);
      setGeneratedTokens((prev) => ({ ...prev, [groupId]: invite.inviteToken }));
      feedback.succeed(
        `Hash gerada para o grupo ${groupId}: ${invite.inviteToken}`
      );
      return true;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Apenas líderes do grupo têm permissão para gerar hashes de convite.",
          network: "Erro ao gerar hash de convite.",
        })
      );
      return false;
    }
  };

  return {
    allGroups,
    memberGroups,
    leaderGroups,
    generatedTokens,
    refresh,
    createGroup,
    joinGroup,
    generateInvite,
  };
};
