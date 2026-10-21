"use client";

import { useEffect, useState } from "react";

import { authApi, discoveryApi, messageOf, usersApi } from "@/lib/api";
import { clearDiscovery, readDiscovery, writeDiscovery } from "@/lib/discovery";
import { clearStoredUser, readStoredUser, writeStoredUser } from "@/lib/storage";
import type { User } from "@/types";
import type { Feedback } from "./useFeedback";

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
  interest_areas: string[];
}

export interface ProfilePayload {
  name: string;
  email: string;
  interest_areas: string[];
  password?: string;
  authorId: string;
}

export interface UseAuth {
  user: User | null;
  login: (email: string, password: string) => Promise<boolean>;
  googleLogin: (credential: string, interestAreas: string[]) => Promise<boolean>;
  register: (payload: RegisterPayload) => Promise<boolean>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  updateProfile: (payload: ProfilePayload) => Promise<User | null>;
  deleteProfile: (password: string) => Promise<boolean>;
  pendingDiscovery: boolean;
  retryDiscovery: () => Promise<void>;
}

export const useAuth = (feedback: Feedback): UseAuth => {
  const [user, setUser] = useState<User | null>(null);
  const [pendingDiscovery, setPendingDiscovery] = useState(false);

  const applyDiscovery = async (current: User) => {
    const draft = readDiscovery();
    if (!draft || !current.id || (draft.ownerId && draft.ownerId !== current.id)) return true;
    writeDiscovery({ ...draft, ownerId: current.id });
    try {
      await discoveryApi.save(draft.postIds);
      clearDiscovery();
      setPendingDiscovery(false);
      return true;
    } catch {
      setPendingDiscovery(true);
      feedback.fail("Você entrou, mas não foi possível aplicar os posts selecionados. Tente novamente ou refaça a seleção.");
      return false;
    }
  };

  useEffect(() => {
    const stored = readStoredUser();
    if (!stored) return;

    authApi
      .me()
      .then((fresh) => {
        writeStoredUser(fresh);
        setUser(fresh);
        const draft = readDiscovery();
        setPendingDiscovery(Boolean(draft && draft.ownerId === fresh.id));
      })
      .catch(() => { clearStoredUser(); setUser(null); });
  }, []);

  const login = async (email: string, password: string) => {
    feedback.begin();
    try {
      const found = await authApi.login(email.trim(), password);

      setUser(found);
      writeStoredUser(found);
      if (await applyDiscovery(found)) feedback.succeed(`Bem-vindo(a), ${found.name}!`);
      return true;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Erro ao consultar a API de usuários.",
          network: "Falha na conexão com o servidor.",
        })
      );
      return false;
    } finally {
      feedback.setLoading(false);
    }
  };

  const register = async (payload: RegisterPayload) => {
    feedback.begin();
    try {
      const newUser = await authApi.register({
        name: payload.name.trim(),
        email: payload.email.trim(),
        password: payload.password,
        interestAreas: payload.interest_areas,
      });

      setUser(newUser);
      writeStoredUser(newUser);
      if (await applyDiscovery(newUser)) feedback.succeed(`Cadastro realizado com sucesso! Bem-vindo(a), ${newUser.name}.`);
      return true;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Não foi possível realizar o cadastro. Verifique os dados.",
          network: "Falha ao comunicar com o servidor.",
        })
      );
      return false;
    } finally {
      feedback.setLoading(false);
    }
  };

  const googleLogin = async (credential: string, interestAreas: string[]) => {
    feedback.begin();
    try {
      const found = await authApi.google(credential, interestAreas);
      setUser(found);
      writeStoredUser(found);
      if (await applyDiscovery(found)) feedback.succeed(`Bem-vindo(a), ${found.name}!`);
      return true;
    } catch (error) {
      feedback.fail(messageOf(error, { api: "Falha na entrada com Google.", network: "Falha de conexão." }));
      return false;
    } finally { feedback.setLoading(false); }
  };

  const logout = () => {
    void authApi.logout().catch(console.error);
    setUser(null);
    setPendingDiscovery(false);
    clearStoredUser();
    feedback.succeed("Sessão encerrada com sucesso.");
  };

  const refreshUser = async () => {
    if (!user) return;
    try {
      const found = await authApi.me();
      if (!found) return;
      setUser(found);
      writeStoredUser(found);
    } catch (error) {
      console.error("Erro ao buscar dados do usuário para o perfil:", error);
    }
  };

  const updateProfile = async (payload: ProfilePayload) => {
    if (!user?.id) {
      feedback.fail(
        "Não foi possível localizar o ID do usuário para alteração."
      );
      return null;
    }

    feedback.begin();
    try {
      const updatedUser = await usersApi.update(user.id, {
        name: payload.name.trim(),
        email: payload.email.trim(),
        interest_areas: payload.interest_areas,
        authorId: payload.authorId,
        ...(payload.password?.trim()
          ? { password: payload.password.trim() }
          : {}),
      });

      setUser(updatedUser);
      writeStoredUser(updatedUser);
      feedback.succeed("Perfil atualizado com sucesso!");
      return updatedUser;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Erro ao atualizar dados do perfil.",
          network: "Falha na comunicação com o servidor ao atualizar perfil.",
        })
      );
      return null;
    } finally {
      feedback.setLoading(false);
    }
  };

  const deleteProfile = async (password: string) => {
    if (!user?.id) {
      feedback.fail(
        "Não foi possível identificar o usuário para exclusão."
      );
      return false;
    }

    feedback.begin();
    try {
      await usersApi.remove(user.id, password.trim());
      return true;
    } catch (error) {
      feedback.fail(
        messageOf(error, {
          api: "Senha incorreta ou erro ao tentar excluir a conta.",
          network: "Falha na comunicação com o servidor.",
        })
      );
      return false;
    } finally {
      feedback.setLoading(false);
    }
  };

  return {
    user,
    login,
    googleLogin,
    register,
    logout,
    refreshUser,
    updateProfile,
    deleteProfile,
    pendingDiscovery,
    retryDiscovery: async () => { if (user && await applyDiscovery(user)) feedback.succeed("Seleção aplicada às recomendações."); },
  };
};
