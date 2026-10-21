"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { groupsApi, socialApi, usersApi } from "@/lib/api";
import type { Post, ResearchGroup, User } from "@/types";

interface RecentPost extends Post {
  groupName: string;
  researcherName: string;
}

const newestFirst = (left: Post, right: Post) => {
  const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
  const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
  return rightTime - leftTime;
};

export function RecentPosts() {
  const [posts, setPosts] = useState<RecentPost[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    void Promise.all([socialApi.recentPosts(), groupsApi.list(), usersApi.list()])
      .then(([feed, groups, users]) => {
        if (!active) return;
        const groupsById = new Map<string, ResearchGroup>(groups.map(group => [group.group_id, group]));
        const usersById = new Map<number, User>(users.filter(user => user.id !== undefined).map(user => [user.id!, user]));
        setPosts(feed.sort(newestFirst).slice(0, 6).map(post => ({
          ...post,
          groupName: groupsById.get(post.groupId)?.name ?? post.groupId,
          researcherName: post.publisherUserId ? usersById.get(post.publisherUserId)?.name ?? "Pesquisador não identificado" : "Pesquisador não identificado",
        })));
      })
      .catch(() => { if (active) setPosts([]); })
      .finally(() => { if (active) setLoading(false); });

    return () => { active = false; };
  }, []);

  return <section className="mt-8 space-y-3">
    <div className="flex items-center justify-between gap-4">
      <div><h2 className="text-xl font-bold">Posts mais recentes</h2><p className="text-sm text-gray-600">As publicações mais novas da rede.</p></div>
      <Link href="/posts" className="text-sm text-green-700 font-semibold hover:underline">Ver todos</Link>
    </div>
    <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
      {loading && <p className="bg-white rounded border border-gray-300 p-5 text-sm text-gray-500">Carregando posts...</p>}
      {!loading && posts.length === 0 && <p className="bg-white rounded border border-gray-300 p-5 text-sm text-gray-500">Nenhum post encontrado.</p>}
      {posts.map(post => <article key={post.id} className="bg-white p-5 rounded border border-gray-300 space-y-2">
        <h3 className="font-bold"><Link className="text-green-700 hover:underline" href={`/groups/${encodeURIComponent(post.groupId)}/posts/${post.id}`}>{post.title}</Link></h3>
        <p className="text-xs text-gray-500">{post.groupName} · {post.researcherName}</p>
        {post.tags && post.tags.length > 0 && <div className="flex flex-wrap gap-1">{post.tags.map(tag => <span key={tag} className="rounded bg-green-50 px-2 py-1 text-xs text-green-800">{tag}</span>)}</div>}
      </article>)}
    </div>
  </section>;
}
