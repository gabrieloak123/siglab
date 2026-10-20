"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { groupsApi, messageOf, socialApi, usersApi } from "@/lib/api";
import { readStoredUser } from "@/lib/storage";
import type { ResearchGroup, User } from "@/types";

export function SocialPanel() {
  const currentUser = typeof window === "undefined" ? null : readStoredUser();
  const [researchers, setResearchers] = useState<User[]>([]);
  const [groups, setGroups] = useState<ResearchGroup[]>([]);
  const [following, setFollowing] = useState<{ researchers: number[]; groups: string[] }>({ researchers: [], groups: [] });
  const [allResearchers, setAllResearchers] = useState<User[]>([]);
  const [allGroups, setAllGroups] = useState<ResearchGroup[]>([]);
  const [query, setQuery] = useState("");
  const [error, setError] = useState("");
  const load = useCallback(async () => {
    try {
      const [suggestedResearchers, suggestedGroups, current, users, groupList] = await Promise.all([
        socialApi.researchers(), socialApi.groups(), socialApi.following(), usersApi.list(), groupsApi.list(),
      ]);
      setResearchers(suggestedResearchers);
      setGroups(suggestedGroups);
      setFollowing(current);
      setAllResearchers(users);
      setAllGroups(groupList);
      setError("");
    } catch (caught) { setError(messageOf(caught, { api: "Não foi possível carregar as sugestões.", network: "Falha de conexão." })); }
  }, []);
  useEffect(() => { void (async () => { await load(); })(); }, [load]);
  const toggleResearcher = async (id: number) => {
    try {
      if (following.researchers.includes(id)) await socialApi.unfollowResearcher(id);
      else await socialApi.followResearcher(id);
      await load();
    } catch (caught) { setError(messageOf(caught, { api: "Não foi possível alterar o seguimento.", network: "Falha de conexão." })); }
  };
  const toggleGroup = async (id: string) => {
    try {
      if (following.groups.includes(id)) await socialApi.unfollowGroup(id);
      else await socialApi.followGroup(id);
      await load();
    } catch (caught) { setError(messageOf(caught, { api: "Não foi possível alterar o seguimento.", network: "Falha de conexão." })); }
  };
  const matchingResearchers = allResearchers.filter(item => item.id !== currentUser?.id && item.name.toLowerCase().includes(query.toLowerCase()));
  const matchingGroups = allGroups.filter(item => item.name.toLowerCase().includes(query.toLowerCase()));
  const followedResearchers = allResearchers.filter(item => item.id !== undefined && following.researchers.includes(item.id));
  const followedGroups = allGroups.filter(item => following.groups.includes(item.group_id));
  return <section className="space-y-4 mt-8">
    <div className="flex items-center justify-between gap-4">
      <div><h2 className="text-xl font-bold">Sua rede de pesquisa</h2><p className="text-sm text-gray-600">Os três perfis e grupos mais próximos dos seus interesses.</p></div>
      <div className="flex gap-2">
        <Link href="/posts?create=1" className="btn-dark text-sm">Criar post</Link>
        <Link href="/posts" className="btn-green text-sm">Posts da rede</Link>
      </div>
    </div>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    <div className="grid md:grid-cols-2 gap-4">
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Pesquisadores que eu sigo</h3>
        {followedResearchers.length === 0 && <p className="text-sm text-gray-500">Você ainda não segue pesquisadores.</p>}
        {followedResearchers.map(item => <div key={item.id} className="flex items-center justify-between gap-3 py-2 border-t border-gray-100"><div><p className="font-medium">{item.name}</p><p className="text-xs text-gray-500">{item.interest_areas?.join(", ")}</p></div><button onClick={() => void toggleResearcher(item.id!)} className="text-red-700 text-xs font-semibold">Deixar de seguir</button></div>)}
      </div>
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Grupos que eu sigo</h3>
        {followedGroups.length === 0 && <p className="text-sm text-gray-500">Você ainda não segue grupos.</p>}
        {followedGroups.map(item => <div key={item.group_id} className="flex items-center justify-between gap-3 py-2 border-t border-gray-100"><div><Link href={`/groups/${encodeURIComponent(item.group_id)}`} className="font-medium text-green-700 hover:underline">{item.name}</Link><p className="text-xs text-gray-500">{item.research_fields?.join(", ")}</p></div><button onClick={() => void toggleGroup(item.group_id)} className="text-red-700 text-xs font-semibold">Deixar de seguir</button></div>)}
      </div>
    </div>
    <div className="grid md:grid-cols-2 gap-4">
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Pesquisadores sugeridos</h3>
        {researchers.length === 0 && <p className="text-sm text-gray-500">Nenhuma sugestão disponível.</p>}
        {researchers.map(item => <div key={item.id} className="flex items-center justify-between gap-3 py-2 border-t border-gray-100"><div><p className="font-medium">{item.name}</p><p className="text-xs text-gray-500">{item.interest_areas?.join(", ")}</p></div><button onClick={() => void toggleResearcher(item.id!)} className="btn-dark text-xs">Seguir</button></div>)}
      </div>
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Grupos sugeridos</h3>
        {groups.length === 0 && <p className="text-sm text-gray-500">Nenhuma sugestão disponível.</p>}
        {groups.map(item => <div key={item.group_id} className="flex items-center justify-between gap-3 py-2 border-t border-gray-100"><div><p className="font-medium">{item.name}</p><p className="text-xs text-gray-500">{item.research_fields?.join(", ")}</p></div><button onClick={() => void toggleGroup(item.group_id)} className="btn-dark text-xs">Seguir</button></div>)}
      </div>
    </div>
    <div className="bg-white p-5 rounded border border-gray-300 space-y-3">
      <h3 className="font-bold">Encontrar e seguir</h3>
      <input aria-label="Buscar pesquisador ou grupo" placeholder="Buscar pesquisador ou grupo" value={query} onChange={event => setQuery(event.target.value)} className="w-full border rounded px-3 py-2 text-sm" />
      {query.trim() && <div className="grid md:grid-cols-2 gap-4 text-sm">
        <div>{matchingResearchers.map(item => <div key={item.id} className="flex justify-between py-1"><span>{item.name}</span><button onClick={() => void toggleResearcher(item.id!)} className="text-green-700 font-semibold">{following.researchers.includes(item.id!) ? "Deixar de seguir" : "Seguir"}</button></div>)}</div>
        <div>{matchingGroups.map(item => <div key={item.group_id} className="flex justify-between py-1"><span>{item.name}</span><button onClick={() => void toggleGroup(item.group_id)} className="text-green-700 font-semibold">{following.groups.includes(item.group_id) ? "Deixar de seguir" : "Seguir"}</button></div>)}</div>
      </div>}
    </div>
  </section>;
}
