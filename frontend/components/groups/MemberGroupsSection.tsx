import { GroupBadge } from "@/components/ui/GroupBadge";
import { ResearchFieldTags } from "@/components/ui/ResearchFieldTags";
import type { ResearchGroup } from "@/types";

interface MemberGroupsSectionProps {
  groups: ResearchGroup[];
}

export function MemberGroupsSection({ groups }: MemberGroupsSectionProps) {
  return (
    <section className="bg-white p-6 rounded border border-gray-300">
      <h2 className="text-lg font-bold text-[#333333] mb-4">
        Meus Grupos de Pesquisa (Membro)
      </h2>

      {groups.length === 0 ? (
        <p className="text-sm text-gray-500 italic">
          Você ainda não é membro de nenhum grupo de pesquisa. Use a caixa de
          busca ou crie um novo grupo.
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {groups.map((group) => (
            <div
              key={group.group_id}
              className="p-4 border border-gray-200 rounded bg-[#F6F4F3]"
            >
              <div className="flex justify-between items-start mb-2">
                <GroupBadge>{group.group_id}</GroupBadge>
              </div>
              <h3 className="font-bold text-[#333333] text-base mb-1">
                {group.name}
              </h3>
              {group.research_fields && group.research_fields.length > 0 && (
                <ResearchFieldTags
                  fields={group.research_fields}
                  className="mt-2"
                />
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
