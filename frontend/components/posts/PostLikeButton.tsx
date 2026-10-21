"use client";

import { useEffect, useState } from "react";
import { messageOf, socialApi } from "@/lib/api";

export function PostLikeButton({ postId }: { postId: number }) {
  const [liked, setLiked] = useState(false);
  const [count, setCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    socialApi.postLike(postId).then(value => {
      if (active) { setLiked(value.liked); setCount(value.count); }
    }).catch(caught => {
      if (active) setError(messageOf(caught, { api: "Não foi possível carregar as curtidas.", network: "Falha de conexão." }));
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [postId]);

  const toggle = async () => {
    setLoading(true); setError("");
    try {
      const value = liked ? await socialApi.unlikePost(postId) : await socialApi.likePost(postId);
      setLiked(value.liked); setCount(value.count);
    } catch (caught) {
      setError(messageOf(caught, { api: "Não foi possível alterar a curtida.", network: "Falha de conexão." }));
    } finally { setLoading(false); }
  };

  return <div className="flex flex-wrap items-center gap-2">
    <button type="button" aria-pressed={liked} disabled={loading} onClick={() => void toggle()}
      className={`rounded border px-3 py-2 text-sm font-semibold transition-colors ${liked ? "border-[#3E885B] bg-green-50 text-[#2f6845]" : "border-gray-300 bg-white text-gray-700 hover:bg-gray-50"}`}>
      <span aria-hidden="true">{liked ? "♥" : "♡"}</span> {liked ? "Curtido" : "Curtir"} · {count}
    </button>
    {error && <span role="alert" className="text-xs text-red-700">{error}</span>}
  </div>;
}
