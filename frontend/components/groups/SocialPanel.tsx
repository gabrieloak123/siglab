"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { groupsApi, messageOf, socialApi, usersApi, type ResearcherFollow } from "@/lib/api";
import { readStoredUser } from "@/lib/storage";
import type { ResearchGroup, User } from "@/types";

export function SocialPanel() {
  const unfollowClass = "w-full sm:w-auto shrink-0 cursor-pointer rounded border border-red-300 bg-red-50 px-3 py-2 text-xs font-semibold text-red-700 transition-colors duration-200 hover:border-red-400 hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-60";
  const followClass = "w-full sm:w-auto shrink-0 cursor-pointer rounded border border-green-300 bg-green-50 px-3 py-2 text-xs font-semibold text-green-800 transition-colors duration-200 hover:border-green-400 hover:bg-green-100 disabled:cursor-not-allowed disabled:opacity-60";
  const currentUser = typeof window === "undefined" ? null : readStoredUser();
  const [researchers, setResearchers] = useState<User[]>([]);
  const [groups, setGroups] = useState<ResearchGroup[]>([]);
  const [following, setFollowing] = useState<{ researchers: number[]; researcherFollows: ResearcherFollow[]; groups: string[] }>({ researchers: [], researcherFollows: [], groups: [] });
  const [allResearchers, setAllResearchers] = useState<User[]>([]);
  const [allGroups, setAllGroups] = useState<ResearchGroup[]>([]);
  const [query, setQuery] = useState("");
  const [error, setError] = useState("");
  const [choosing, setChoosing] = useState<User | null>(null);
  const [selectedAreas, setSelectedAreas] = useState<string[]>([]);
  const [refreshing, setRefreshing] = useState(false);
  const load = useCallback(async () => {
    setRefreshing(true);
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
    finally { setRefreshing(false); }
  }, []);
  useEffect(() => { void (async () => { await load(); })(); }, [load]);
  const unfollowResearcher = async (id: number) => {
    try {
      await socialApi.unfollowResearcher(id);
      await load();
    } catch (caught) { setError(messageOf(caught, { api: "Não foi possível alterar o seguimento.", network: "Falha de conexão." })); }
  };
  const openFollow = (researcher: User) => {
    const current=following.researcherFollows.find(follow => follow.researcherId === researcher.id);
    setSelectedAreas(current?.interestAreas ?? []);
    setChoosing(researcher);
  };
  const saveResearcherFollow = async () => {
    if (!choosing?.id) return;
    try {
      await socialApi.followResearcher(choosing.id, selectedAreas);
      setChoosing(null);
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
      <div className="flex flex-col sm:flex-row gap-2">
        <button type="button" onClick={() => void load()} disabled={refreshing} className="cursor-pointer rounded border border-gray-300 bg-white px-3 py-2 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-100 disabled:cursor-wait disabled:opacity-60" aria-label="Recarregar sugestões">
          <span aria-hidden="true" className={`mr-1 inline-block ${refreshing ? "animate-spin" : ""}`}>↻</span> {refreshing ? "Recarregando..." : "Recarregar sugestões"}
        </button>
        <Link href="/posts?create=1" className="btn-dark text-sm">Criar post</Link>
        <Link href="/posts" className="btn-green text-sm">Posts da rede</Link>
      </div>
    </div>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    <div className="grid md:grid-cols-2 gap-4">
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Pesquisadores que eu sigo</h3>
        {followedResearchers.length === 0 && <p className="text-sm text-gray-500">Você ainda não segue pesquisadores.</p>}
        {followedResearchers.map(item => { const scope=following.researcherFollows.find(f => f.researcherId === item.id)?.interestAreas ?? []; return <div key={item.id} className="flex flex-col gap-3 py-3 border-t border-gray-100"><div className="min-w-0"><p className="font-medium break-words">{item.name}</p><p className="text-xs text-gray-500 break-words">{scope.length ? `Seguindo: ${scope.join(", ")}` : "Seguindo todas as áreas"}</p></div><div className="flex flex-col sm:flex-row gap-2"><button onClick={() => openFollow(item)} className={followClass}>Alterar áreas</button><button onClick={() => void unfollowResearcher(item.id!)} className={unfollowClass}>Deixar de seguir</button></div></div>; })}
      </div>
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Grupos que eu sigo</h3>
        {followedGroups.length === 0 && <p className="text-sm text-gray-500">Você ainda não segue grupos.</p>}
        {followedGroups.map(item => <div key={item.group_id} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 py-3 border-t border-gray-100"><div className="min-w-0"><p className="font-medium break-words">{item.name}</p><p className="text-xs text-gray-500 break-words">{item.research_fields?.join(", ")}</p></div><button onClick={() => void toggleGroup(item.group_id)} className={unfollowClass}>Deixar de seguir</button></div>)}
      </div>
    </div>
    <div className="grid md:grid-cols-2 gap-4">
      <div className="bg-white p-5 rounded border border-gray-300"><h3 className="font-bold mb-3">Pesquisadores sugeridos</h3>
        {researchers.length === 0 && <p className="text-sm text-gray-500">Nenhuma sugestão disponível.</p>}
        {researchers.map(item => <div key={item.id} className="flex items-center justify-between gap-3 py-2 border-t border-gray-100"><div><p className="font-medium">{item.name}</p><p className="text-xs text-gray-500">{item.interest_areas?.join(", ")}</p></div><button onClick={() => openFollow(item)} className="btn-dark text-xs">Seguir</button></div>)}
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
        <div>{matchingResearchers.map(item => { const isFollowing = following.researchers.includes(item.id!); return <div key={item.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 py-2"><span className="break-words">{item.name}</span><button onClick={() => isFollowing ? void unfollowResearcher(item.id!) : openFollow(item)} className={isFollowing ? unfollowClass : followClass}>{isFollowing ? "Deixar de seguir" : "Seguir"}</button></div>; })}</div>
        <div>{matchingGroups.map(item => { const isFollowing = following.groups.includes(item.group_id); return <div key={item.group_id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 py-2"><span className="break-words">{item.name}</span><button onClick={() => void toggleGroup(item.group_id)} className={isFollowing ? unfollowClass : followClass}>{isFollowing ? "Deixar de seguir" : "Seguir"}</button></div>; })}</div>
      </div>}
    </div>
    {choosing && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true" aria-labelledby="follow-scope-title">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <h3 id="follow-scope-title" className="text-lg font-bold">Seguir {choosing.name}</h3>
        <p className="mt-1 text-sm text-gray-600">Escolha todas as publicações ou somente as áreas que interessam a você.</p>
        <label className="mt-4 flex cursor-pointer items-center gap-3 rounded border p-3 hover:bg-gray-50">
          <input type="radio" name="follow-scope" checked={selectedAreas.length === 0} onChange={() => setSelectedAreas([])} />
          <span className="font-medium">Todas as áreas</span>
        </label>
        <div className="mt-2 space-y-2">
          {(choosing.interest_areas ?? []).map(area => <label key={area} className="flex cursor-pointer items-center gap-3 rounded border p-3 hover:bg-gray-50">
            <input type="checkbox" checked={selectedAreas.includes(area)} onChange={event => setSelectedAreas(current => event.target.checked ? [...current, area] : current.filter(value => value !== area))} />
            <span>{area}</span>
          </label>)}
        </div>
        <div className="mt-5 flex justify-end gap-2"><button className="btn-dark" onClick={() => setChoosing(null)}>Cancelar</button><button className="btn-green" onClick={() => void saveResearcherFollow()}>Salvar seguimento</button></div>
      </div>
    </div>}
  </section>;
}
