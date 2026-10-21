"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";

import { GroupAreaHeader } from "@/components/groups/GroupAreaHeader";
import { GroupNavigation } from "@/components/groups/GroupNavigation";
import { PostEditor } from "@/components/posts/PostEditor";
import { PostList } from "@/components/posts/PostList";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { useGroupArea } from "@/hooks/useGroupArea";
import { usePosts } from "@/hooks/usePosts";

export default function GroupPostsPage() {
  const params = useParams<{ groupId: string }>();
  const groupId = decodeURIComponent(params.groupId);
  const area = useGroupArea(groupId);
  const posts = usePosts(groupId, area.user?.email ?? null);
  const [isCreating, setIsCreating] = useState(false);
  const [notice, setNotice] = useState("");

  if (area.loading) {
    return (
      <main className="flex-1 max-w-4xl w-full mx-auto p-6">
        <p className="text-sm text-gray-600">Carregando posts do grupo...</p>
      </main>
    );
  }

  if (!area.group) {
    return (
      <main className="flex-1 max-w-4xl w-full mx-auto p-6">
        <h1 className="text-xl font-bold text-[#333333] mb-2">
          Grupo indisponível
        </h1>
        <p className="text-sm text-gray-600 mb-4">
          {area.error || "Grupo de pesquisa não encontrado."}
        </p>
        <Link href="/" className="btn-dark text-sm">
          Voltar para a página inicial
        </Link>
      </main>
    );
  }

  const isAnonymous = !area.user;

  return (
    <main className="flex-1 max-w-4xl w-full mx-auto p-6">
      {isAnonymous && (
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <Link
            href="/"
            className="text-sm text-green-700 font-semibold hover:underline"
          >
            ← Voltar para a página inicial
          </Link>
          <Link href="/?descoberta=1" className="btn-green text-sm">
            Entrar / Cadastrar-se
          </Link>
        </div>
      )}

      <GroupAreaHeader group={area.group} isLeader={area.isLeader} />
      {!isAnonymous && (
        <GroupNavigation groupId={groupId} active="posts" isLeader={area.isLeader} />
      )}

      {isAnonymous && (
        <p className="mb-4 rounded border border-amber-300 bg-amber-50 p-3 text-sm text-[#333333]">
          Você está navegando como visitante. Faça login para curtir e publicar
          posts neste grupo de pesquisa.
        </p>
      )}

      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-bold text-[#333333]">Posts do Grupo</h2>

        {area.isLeader && !isCreating && (
          <button
            type="button"
            onClick={() => setIsCreating(true)}
            className="btn-green text-sm"
          >
            Novo Post
          </button>
        )}
      </div>

      {isCreating && (
        <div className="bg-white p-6 rounded border border-gray-300 mb-6">
          <h3 className="text-base font-bold text-[#333333] mb-4">
            Publicar novo post
          </h3>
          <PostEditor
            submitLabel="Publicar Post"
            onCancel={() => setIsCreating(false)}
            onSubmit={async (payload) => {
              const created = await posts.create(payload);
              if (created) {
                setIsCreating(false);
                setNotice("Post publicado com sucesso no grupo de pesquisa!");
                await posts.reload();
              }
              return created !== null;
            }}
          />
        </div>
      )}

      <AlertBanner message={posts.error} variant="error" />
      <AlertBanner message={notice} variant="success" />

      {posts.loading ? (
        <p className="text-sm text-gray-600">Carregando posts...</p>
      ) : (
        <PostList groupId={groupId} posts={posts.posts} showLike={!isAnonymous} />
      )}
    </main>
  );
}
