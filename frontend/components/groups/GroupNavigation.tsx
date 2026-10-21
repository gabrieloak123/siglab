import Link from "next/link";

interface GroupNavigationProps {
  groupId: string;
  active: "board" | "posts" | "settings";
  isLeader: boolean;
}

export function GroupNavigation({ groupId, active, isLeader }: GroupNavigationProps) {
  const basePath = `/groups/${encodeURIComponent(groupId)}`;
  const itemClass = (item: GroupNavigationProps["active"]) =>
    `px-4 py-2 rounded text-sm font-semibold transition-colors ${active === item ? "bg-[#3E885B] text-white" : "text-[#333333] hover:bg-gray-100"}`;

  return (
    <nav aria-label="Navegação da área do grupo" className="flex flex-wrap gap-1 mb-6 rounded border border-gray-300 bg-white p-1">
      <Link href={basePath} className={itemClass("board")} aria-current={active === "board" ? "page" : undefined}>Quadro</Link>
      <Link href={`${basePath}/posts`} className={itemClass("posts")} aria-current={active === "posts" ? "page" : undefined}>Posts</Link>
      {isLeader && <Link href={`${basePath}/settings`} className={itemClass("settings")} aria-current={active === "settings" ? "page" : undefined}>Configurações</Link>}
    </nav>
  );
}
