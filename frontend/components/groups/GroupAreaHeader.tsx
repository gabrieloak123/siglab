import Link from "next/link";

import { GroupBadge } from "@/components/ui/GroupBadge";
import { ResearchFieldTags } from "@/components/ui/ResearchFieldTags";
import type { ResearchGroup } from "@/types";

interface GroupAreaHeaderProps {
  group: ResearchGroup;
  isLeader: boolean;
}

export function GroupAreaHeader({ group, isLeader }: GroupAreaHeaderProps) {
  return (
    <div className="bg-white p-6 rounded border border-gray-300 mb-6">
      <div className="flex justify-between items-start gap-4 mb-2">
        <div className="flex flex-col gap-2">
          <GroupBadge tone={isLeader ? "green" : "dark"}>
            {group.group_id}
            {isLeader ? " (Líder / Orientador)" : ""}
          </GroupBadge>
          <h2 className="text-xl font-bold text-[#333333]">{group.name}</h2>
        </div>
        <Link
          href={`/groups/${encodeURIComponent(group.group_id)}`}
          className="btn-dark text-xs whitespace-nowrap"
        >
          Voltar para o Quadro
        </Link>
      </div>

      {group.research_fields && group.research_fields.length > 0 && (
        <ResearchFieldTags fields={group.research_fields} className="mt-2" />
      )}
    </div>
  );
}
