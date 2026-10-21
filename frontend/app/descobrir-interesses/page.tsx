"use client";

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authApi, discoveryApi, messageOf, type DiscoveryPost } from "@/lib/api";
import { clearDiscovery, writeDiscovery } from "@/lib/discovery";
import type { User } from "@/types";

function suggestAreas(posts: DiscoveryPost[]): string[] {
  const counts = new Map<string, { label: string; count: number }>();
  for (const post of posts) {
    const terms = post.keywords.length ? post.keywords : post.tags;
    const seen = new Set<string>();
    for (const term of terms) {
      const label = term?.trim();
      if (!label || label.length > 255) continue;
      const key = label.toLocaleLowerCase();
      if (seen.has(key)) continue;
      seen.add(key);
      const previous = counts.get(key);
      counts.set(key, { label: previous?.label ?? label, count: (previous?.count ?? 0) + 1 });
    }
  }
  return [...counts.values()].sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, "pt-BR"))
    .slice(0, 3).map(term => term.label);
}

function DiscoveryContent() {
  const router = useRouter();
  const embedded = useSearchParams().get("embedded") === "1";
  const [posts, setPosts] = useState<DiscoveryPost[]>([]);
  const [selected, setSelected] = useState<DiscoveryPost[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [user, setUser] = useState<User | null>(null);
  const [sessionChecked, setSessionChecked] = useState(false);
  useEffect(() => {
    let active = true;
    authApi.me().then(value => { if (active) setUser(value); }).catch(() => {}).finally(() => { if (active) setSessionChecked(true); });
    return () => { active = false; };
  }, []);
  useEffect(() => {
    let active = true;
    discoveryApi.posts(page).then(result => {
      if (active) { setPosts(result.posts); setHasMore(result.hasMore); setError(""); }
    }).catch(err => { if (active) setError(messageOf(err, { api: "Não foi possível carregar as publicações.", network: "Falha de conexão." })); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page]);

  const toggle = (post: DiscoveryPost) => {
    setSelected(current => current.some(item => item.id === post.id)
      ? current.filter(item => item.id !== post.id)
      : current.length < 30 ? [...current, post] : current);
  };
  const finish = async () => {
    if (!selected.length) return;
    setSaving(true); setError("");
    const draft = { postIds: selected.map(post => post.id), areas: suggestAreas(selected), ...(user?.id ? { ownerId: user.id } : {}) };
    writeDiscovery(draft);
    if (embedded) {
      window.parent.postMessage({ type: "siglab-interest-discovery", areas: draft.areas }, window.location.origin);
      setSaving(false);
      return;
    }
    if (user) {
      try { await discoveryApi.save(draft.postIds); clearDiscovery(); router.push("/"); }
      catch (err) { setError(messageOf(err, { api: "Não foi possível aplicar sua seleção. Tente novamente.", network: "Falha de conexão." })); setSaving(false); }
    } else router.push("/?descoberta=1");
  };
  const changePage = (next: number) => { setLoading(true); setPage(next); };

  return <div className="min-h-screen bg-[#F6F4F3] text-[#333]">
    {!embedded && <header className="bg-[#333] text-white px-6 py-4"><Link href="/" className="font-bold text-xl">SIGLab</Link></header>}
    <main className="mx-auto max-w-4xl p-6 space-y-5">
      {!embedded && <Link href="/" className="text-sm font-semibold text-[#3E885B] underline">Voltar para o início</Link>}
      <div><h1 className="text-2xl font-bold">Descobrir minhas áreas de interesse</h1>
        <p className="mt-2 text-sm text-gray-600">Selecione até 30 publicações que despertam seu interesse. Usaremos seus vetores para personalizar as recomendações e suas palavras-chave para sugerir áreas.</p></div>
      <div className="rounded border border-gray-200 bg-white p-4 text-sm">
        <strong>{selected.length} selecionada(s)</strong> · As áreas sugeridas poderão ser editadas no cadastro. Posts ainda não indexados não podem ser selecionados.
      </div>
      {error && <p role="alert" className="rounded bg-red-50 p-3 text-red-700">{error}</p>}
      {loading ? <p>Carregando publicações...</p> : posts.length === 0 ? <p>Nenhuma publicação disponível nesta página.</p> :
        <div className="space-y-3">{posts.map(post => <label key={post.id} className={`flex gap-3 rounded border bg-white p-4 ${post.indexed ? "cursor-pointer" : "opacity-60"}`}>
          <input type="checkbox" checked={selected.some(item => item.id === post.id)} disabled={!post.indexed || (selected.length >= 30 && !selected.some(item => item.id === post.id))} onChange={() => toggle(post)} aria-label={`Selecionar ${post.title}`} />
          <span><strong className="block">{post.title}</strong><span className="text-sm text-gray-600">{post.groupName}</span>
            {post.description && <span className="block mt-1 text-sm">{post.description}</span>}
            <span className="block mt-2 text-xs text-gray-500">{(post.keywords.length ? post.keywords : post.tags).join(" · ") || "Sem palavras-chave"}{!post.indexed && " · Aguardando indexação"}</span>
          </span></label>)}</div>}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-2"><button className="btn-dark text-sm" disabled={page === 0 || loading} onClick={() => changePage(page - 1)}>Anterior</button><span className="py-2 text-sm">Página {page + 1}</span><button className="btn-dark text-sm" disabled={!hasMore || loading} onClick={() => changePage(page + 1)}>Próxima</button></div>
        <button className="btn-green text-sm" disabled={!selected.length || saving || !sessionChecked} onClick={() => void finish()}>{saving ? "Salvando..." : user ? "Aplicar seleção" : "Confirmar seleção e continuar"}</button>
      </div>
    </main>
  </div>;
}

export default function DiscoveryPage() {
  return <Suspense fallback={<div className="min-h-screen bg-[#F6F4F3]" />}><DiscoveryContent /></Suspense>;
}
