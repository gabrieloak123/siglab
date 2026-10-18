"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { PostEditor } from "@/components/posts/PostEditor";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { groupsApi, messageOf, postsApi, socialApi } from "@/lib/api";
import { readStoredUser } from "@/lib/storage";
import type { Post, ResearchGroup } from "@/types";

export default function NetworkPostsPage() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [groups, setGroups] = useState<ResearchGroup[]>([]);
  const [groupId, setGroupId] = useState("");
  const [followedOnly, setFollowedOnly] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const user = typeof window === "undefined" ? null : readStoredUser();
  useEffect(() => {
    queueMicrotask(() => { if (new URLSearchParams(window.location.search).has("create")) setCreating(true); });
  }, []);
  const load = useCallback(async () => {
    try { setPosts(await socialApi.feed(followedOnly)); setError(""); }
    catch (caught) { setError(messageOf(caught, { api: "Não foi possível carregar os posts.", network: "Falha de conexão." })); }
  }, [followedOnly]);
  useEffect(() => { void (async () => { await load(); })(); }, [load]);
  useEffect(() => {
    if (!user?.id) return;
    void groupsApi.list().then(async all => {
      const membership = await Promise.all(all.map(async group => ({ group, members: await groupsApi.members(group.group_id) })));
      setGroups(membership.filter(item => item.members.some(member => member.id === user.id && member.isLeader)).map(item => item.group));
    }).catch(() => setGroups([]));
  }, [user?.id]);
  return <main className="max-w-4xl mx-auto w-full p-6 space-y-5">
    <nav className="flex gap-4 text-sm"><Link href="/" className="text-green-700 font-semibold">← Área do usuário</Link><Link href="/posts">Posts da rede</Link></nav>
    <div className="flex justify-between items-center gap-4"><div><h1 className="text-2xl font-bold">Posts da rede</h1><p className="text-sm text-gray-600">Ordenados por afinidade aos seus interesses e depois pela data.</p></div>
      {groups.length > 0 && <button className="btn-green text-sm" onClick={() => setCreating(value => !value)}>{creating ? "Fechar" : "Criar post"}</button>}
    </div>
    {creating && <div className="bg-white border rounded p-5 space-y-3">
      <label className="block text-sm font-semibold">Grupo da publicação
        <select required value={groupId} onChange={event => setGroupId(event.target.value)} className="block w-full border rounded p-2 mt-1">
          <option value="">Selecione o grupo</option>{groups.map(group => <option key={group.group_id} value={group.group_id}>{group.name}</option>)}
        </select>
      </label>
      <PostEditor submitLabel="Publicar post" onSubmit={async payload => {
        if (!user || !groupId) { setError("Selecione o grupo da publicação."); return false; }
        try { await postsApi.create(groupId, user.email, payload); setCreating(false); setNotice("Post publicado."); await load(); return true; }
        catch (caught) { setError(messageOf(caught, { api: "Não foi possível publicar o post.", network: "Falha de conexão." })); return false; }
      }} />
    </div>}
    <label className="flex gap-2 items-center text-sm"><input type="checkbox" checked={followedOnly} onChange={event => setFollowedOnly(event.target.checked)} /> Só publicações de pesquisadores ou grupos que sigo</label>
    <AlertBanner message={error} variant="error" /><AlertBanner message={notice} variant="success" />
    {posts.length === 0 && !error && <p className="text-gray-500 text-sm">Nenhum post encontrado.</p>}
    {posts.map(post => <article key={post.id} className="bg-white border rounded p-5 space-y-2">
      <p className="text-xs text-gray-500">Grupo {post.groupId} · {post.postType === "ARTIGO" ? "Artigo" : "Outros"}</p>
      <h2 className="font-bold text-lg"><Link className="text-green-700 hover:underline" href={`/groups/${encodeURIComponent(post.groupId)}/posts/${post.id}`}>{post.title}</Link></h2>
      {post.description && <p className="text-sm">{post.description}</p>}
      <p className="text-xs text-gray-500">{post.keywords?.join(" · ")}</p>
    </article>)}
  </main>;
}
