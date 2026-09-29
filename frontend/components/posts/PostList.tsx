import Link from "next/link";

import { GroupBadge } from "@/components/ui/GroupBadge";
import type { Post } from "@/types";

interface PostListProps {
  groupId: string;
  posts: Post[];
}

const formatDate = (value?: string | null) => {
  if (!value) return "";

  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? ""
    : date.toLocaleDateString("pt-BR", { dateStyle: "long" });
};

export function PostList({ groupId, posts }: PostListProps) {
  if (posts.length === 0) {
    return (
      <p className="text-sm text-gray-500 italic">
        Nenhum post publicado neste grupo até o momento.
      </p>
    );
  }

  return (
    <div className="space-y-3">
      {posts.map((post) => (
        <article
          key={post.id}
          className="p-4 border border-gray-200 rounded bg-white"
        >
          <div className="flex justify-between items-start gap-3 mb-2">
            <div className="flex flex-col gap-1">
              <h3 className="font-bold text-[#333333] text-base">
                <Link
                  href={`/groups/${encodeURIComponent(groupId)}/posts/${post.id}`}
                  className="hover:text-[#3E885B] hover:underline underline-offset-4 transition-colors"
                >
                  {post.title}
                </Link>
              </h3>
              <span className="text-xs text-gray-500">
                {formatDate(post.createdAt)}
                {post.filePath ? ` · arquivo: ${post.filePath}` : ""}
              </span>
            </div>
            {post.tags && post.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 justify-end">
                {post.tags.map((tag) => (
                  <GroupBadge key={tag} tone="green">
                    {tag}
                  </GroupBadge>
                ))}
              </div>
            )}
          </div>

          {post.description && (
            <p className="text-sm text-[#333333]">{post.description}</p>
          )}
        </article>
      ))}
    </div>
  );
}
