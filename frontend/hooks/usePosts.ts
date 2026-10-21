"use client";

import { useCallback, useEffect, useState } from "react";

import { messageOf, postsApi, type PostPayload } from "@/lib/api";
import type { Post } from "@/types";

export interface UsePosts {
  posts: Post[];
  loading: boolean;
  error: string;
  reload: () => Promise<void>;
  create: (payload: PostPayload) => Promise<Post | null>;
  update: (postId: number, payload: PostPayload) => Promise<Post | null>;
  remove: (postId: number) => Promise<boolean>;
}

export const usePosts = (
  groupId: string,
  requesterEmail: string | null
): UsePosts => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    setError("");
    setLoading(true);

    try {
      // A listagem é pública: visitantes anônimos também conseguem ler os
      // posts do grupo. Apenas as mutações exigem um e-mail de requisitante.
      setPosts(await postsApi.list(groupId, requesterEmail));
    } catch (caught) {
      setError(
        messageOf(caught, {
          api: "Não foi possível carregar os posts do grupo.",
          network: "Falha de conexão com o servidor.",
        })
      );
    } finally {
      setLoading(false);
    }
  }, [groupId, requesterEmail]);

  useEffect(() => {
    void (async () => {
      await reload();
    })();
  }, [reload]);

  const create = async (payload: PostPayload) => {
    if (!requesterEmail) return null;

    try {
      return await postsApi.create(groupId, requesterEmail, payload);
    } catch (caught) {
      setError(
        messageOf(caught, {
          api: "Não foi possível publicar o post. Verifique os campos informados.",
          network: "Falha de conexão ao tentar publicar o post.",
        })
      );
      return null;
    }
  };

  const update = async (postId: number, payload: PostPayload) => {
    if (!requesterEmail) return null;

    try {
      return await postsApi.update(groupId, postId, requesterEmail, payload);
    } catch (caught) {
      setError(
        messageOf(caught, {
          api: "Não foi possível atualizar o post.",
          network: "Falha de conexão ao tentar atualizar o post.",
        })
      );
      return null;
    }
  };

  const remove = async (postId: number) => {
    if (!requesterEmail) return false;

    try {
      await postsApi.remove(groupId, postId, requesterEmail);
      await reload();
      return true;
    } catch (caught) {
      setError(
        messageOf(caught, {
          api: "Não foi possível excluir o post.",
          network: "Falha de conexão ao tentar excluir o post.",
        })
      );
      return false;
    }
  };

  return { posts, loading, error, reload, create, update, remove };
};
