import { GroupBadge } from "@/components/ui/GroupBadge";
import { ResearchFieldTags } from "@/components/ui/ResearchFieldTags";
import type { ResearchGroup } from "@/types";

interface PublicGroupsSectionProps {
  groups: ResearchGroup[];
}

export function PublicGroupsSection({ groups }: PublicGroupsSectionProps) {
  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded border border-gray-300">
        <h2 className="text-xl font-bold text-[#333333] mb-2">
          Grupos de Pesquisa Existentes
        </h2>
        <p className="text-sm text-gray-600">
          Abaixo estão listados todos os grupos de pesquisa cadastrados no
          sistema SIGLab. Faça login ou cadastre-se para participar de um grupo.
        </p>
      </div>

      {groups.length === 0 ? (
        <div className="bg-white p-8 rounded text-center border border-gray-300 text-gray-500">
          Nenhum grupo de pesquisa cadastrado no sistema até o momento.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {groups.map((group) => (
            <div
              key={group.group_id}
              className="bg-white p-5 rounded border border-gray-300 flex flex-col justify-between"
            >
              <div>
                <GroupBadge className="inline-block mb-3">
                  {group.group_id}
                </GroupBadge>
                <h3 className="font-bold text-[#333333] text-lg mb-2">
                  {group.name}
                </h3>

                {group.research_fields && group.research_fields.length > 0 && (
                  <div className="mt-3">
                    <span className="text-xs text-gray-500 block mb-1">
                      Linhas de Pesquisa:
                    </span>
                    <ResearchFieldTags
                      fields={group.research_fields}
                      variant="muted"
                    />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
