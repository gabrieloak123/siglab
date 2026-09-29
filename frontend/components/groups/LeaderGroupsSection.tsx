import Link from "next/link";

import { GroupBadge } from "@/components/ui/GroupBadge";
import type { ResearchGroup } from "@/types";

interface LeaderGroupsSectionProps {
  groups: ResearchGroup[];
  generatedTokens: Record<string, string>;
  onGenerateInvite: (groupId: string) => void;
}

export function LeaderGroupsSection({
  groups,
  generatedTokens,
  onGenerateInvite,
}: LeaderGroupsSectionProps) {
  return (
    <section className="bg-white p-6 rounded border border-gray-300">
      <h2 className="text-lg font-bold text-[#333333] mb-4">
        Grupos em que sou Orientador / Líder
      </h2>

      {groups.length === 0 ? (
        <p className="text-sm text-gray-500 italic">
          Você não é líder de nenhum grupo de pesquisa no momento. Clique no
          botão &quot;Criar Grupo&quot; acima para criar o seu.
        </p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {groups.map((group) => (
            <div
              key={group.group_id}
              className="p-4 border border-[#3E885B] rounded bg-white flex flex-col justify-between"
            >
              <div>
                <div className="flex justify-between items-start mb-2">
                  <GroupBadge tone="green">
                    {group.group_id} (Líder / Orientador)
                  </GroupBadge>
                </div>
                <h3 className="font-bold text-[#333333] text-base mb-3">
                  <Link
                    href={`/groups/${encodeURIComponent(group.group_id)}`}
                    className="hover:text-[#3E885B] hover:underline underline-offset-4 transition-colors"
                  >
                    {group.name}
                  </Link>
                </h3>
              </div>

              <div>
                <div className="flex gap-2 mb-3">
                  <button
                    onClick={() => onGenerateInvite(group.group_id)}
                    className="btn-dark text-xs flex-1"
                  >
                    Gerar Hash
                  </button>
                  <Link
                    href={`/groups/${encodeURIComponent(group.group_id)}/settings`}
                    className="btn-green text-xs flex-1 text-center"
                  >
                    Gerenciar Grupo
                  </Link>
                </div>

                {generatedTokens[group.group_id] && (
                  <div className="p-2 bg-[#F6F4F3] border border-gray-300 rounded text-xs">
                    <span className="text-gray-600 block mb-1">
                      Hash Gerada (Válida por 7 dias):
                    </span>
                    <code className="font-mono font-bold text-[#BF3100] break-all">
                      {generatedTokens[group.group_id]}
                    </code>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
