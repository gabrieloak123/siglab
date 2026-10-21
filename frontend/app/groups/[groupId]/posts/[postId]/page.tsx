"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import { MarkdownContent } from "@/components/posts/MarkdownContent";
import { PostEditor } from "@/components/posts/PostEditor";
import { PostLikeButton } from "@/components/posts/PostLikeButton";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { GroupBadge } from "@/components/ui/GroupBadge";
import { useFeedback } from "@/hooks/useFeedback";
import { useGroupArea } from "@/hooks/useGroupArea";
import { messageOf, postsApi } from "@/lib/api";
import type { Post } from "@/types";

const formatDate = (value?: string | null) => {
  if (!value) return "";

  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleDateString("pt-BR", { dateStyle: "long" });
};

export default function PostDetailPage() {
  const params = useParams<{ groupId: string; postId: string }>();
  const groupId = decodeURIComponent(params.groupId);
  const postId = Number(params.postId);
  const router = useRouter();

  const area = useGroupArea(groupId);
  const feedback = useFeedback();
  const [post, setPost] = useState<Post | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [isEditing, setIsEditing] = useState(false);

  const requesterEmail = area.user?.email ?? null;

  const reload = useCallback(async () => {
    if (Number.isNaN(postId)) {
      setPost(null);
      setLoading(false);
      return;
    }

    setError("");
    setLoading(true);

    try {
      setPost(await postsApi.get(groupId, postId, requesterEmail));
    } catch (caught) {
      setError(
        messageOf(caught, {
          api: "Não foi possível carregar o post.",
          network: "Falha de conexão com o servidor.",
        })
      );
    } finally {
      setLoading(false);
    }
  }, [groupId, postId, requesterEmail]);

  useEffect(() => {
    void (async () => {
      await reload();
    })();
  }, [reload]);

  const basePath = `/groups/${encodeURIComponent(groupId)}`;

  if (loading || area.loading) {
    return (
      <main className="flex-1 max-w-4xl w-full mx-auto p-6">
        <p className="text-sm text-gray-600">Carregando post...</p>
      </main>
    );
  }

  if (!post) {
    return (
      <main className="flex-1 max-w-4xl w-full mx-auto p-6">
        <h1 className="text-xl font-bold text-[#333333] mb-2">
          Post indisponível
        </h1>
        <p className="text-sm text-gray-600 mb-4">
          {error || area.error || "Post não encontrado neste grupo."}
        </p>
        <Link href={`${basePath}/posts`} className="btn-dark text-sm">
          Voltar para os Posts
        </Link>
      </main>
    );
  }

  const handleDelete = async () => {
    if (!requesterEmail) return;

    feedback.begin();

    try {
      await postsApi.remove(groupId, post.id, requesterEmail);
      router.push(`${basePath}/posts`);
    } catch (caught) {
      feedback.fail(
        messageOf(caught, {
          api: "Não foi possível excluir o post.",
          network: "Falha de conexão ao tentar excluir o post.",
        })
      );
      feedback.setLoading(false);
    }
  };

  const isAnonymous = !area.user;

  return (
    <main className="flex-1 max-w-4xl w-full mx-auto p-6">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <Link href={`${basePath}/posts`} className="text-sm text-green-700 font-semibold hover:underline">
          ← Voltar para os posts do grupo
        </Link>
        {isAnonymous && (
          <Link href="/?descoberta=1" className="btn-green text-sm">
            Entrar / Cadastrar-se
          </Link>
        )}
      </div>

      <AlertBanner message={feedback.error} variant="error" />
      <AlertBanner message={feedback.success} variant="success" />

      {isEditing ? (
        <div className="bg-white p-6 rounded border border-gray-300">
          <h2 className="text-base font-bold text-[#333333] mb-4">
            Editar post
          </h2>
          <PostEditor
            post={post}
            submitLabel="Salvar Alterações"
            onCancel={() => setIsEditing(false)}
            onSubmit={async (payload) => {
              feedback.begin();
              try {
                const updated = await postsApi.update(groupId, post.id, requesterEmail as string, payload);
                setPost(updated);
                setIsEditing(false);
                feedback.succeed("Post atualizado com sucesso!");
                return true;
              } catch (caught) {
                feedback.fail(messageOf(caught, { api: "Não foi possível atualizar o post.", network: "Falha de conexão." }));
                return false;
              } finally {
                feedback.setLoading(false);
              }
            }}
          />
        </div>
      ) : (
        <article className="bg-white p-6 rounded border border-gray-300">
          <div className="flex justify-between items-start gap-4 mb-4">
            <div>
              <h1 className="text-xl font-bold text-[#333333] mb-1">
                {post.title}
              </h1>
              <p className="text-xs text-gray-500">
                {formatDate(post.createdAt)}
                {post.filePath ? ` · arquivo: ${post.filePath}` : ""}
              </p>
            </div>

            {area.isLeader && (
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setIsEditing(true)}
                  className="btn-dark text-xs"
                >
                  Editar
                </button>
                <button
                  type="button"
                  onClick={() => {
                    void handleDelete();
                  }}
                  disabled={feedback.loading}
                  className="btn-red text-xs"
                >
                  {feedback.loading ? "Excluindo..." : "Excluir"}
                </button>
              </div>
            )}
          </div>

          {post.description && (
            <p className="text-sm text-[#333333] mb-4 border-l-2 border-[#3E885B] pl-3">
              {post.description}
            </p>
          )}

          {post.tags && post.tags.length > 0 && (
            <div className="flex flex-wrap gap-1 mb-4">
              {post.tags.map((tag) => (
                <GroupBadge key={tag} tone="green">
                  {tag}
                </GroupBadge>
              ))}
            </div>
          )}

          <MarkdownContent content={post.content ?? ""} />
          {!isAnonymous && (
            <div className="mt-6 border-t border-gray-200 pt-4"><PostLikeButton postId={post.id} /></div>
          )}
        </article>
      )}
    </main>
  );
}
