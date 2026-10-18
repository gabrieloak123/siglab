import { useState, type FormEvent } from "react";

import { FormField, inputClassName } from "@/components/ui/FormField";
import type { UseResearchGroups } from "@/hooks/useResearchGroups";
import type { Feedback } from "@/hooks/useFeedback";
import { parseList } from "@/lib/utils";

interface CreateGroupCardProps {
  groups: UseResearchGroups;
  feedback: Feedback;
}

export function CreateGroupCard({
  groups,
  feedback,
}: CreateGroupCardProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [groupId, setGroupId] = useState("");
  const [name, setName] = useState("");
  const [fields, setFields] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!groupId.trim() || !name.trim() || parseList(fields).length !== 3) {
      feedback.fail(
        "Informe o Código ID, o nome e exatamente três linhas de pesquisa."
      );
      return;
    }

    const created = await groups.createGroup({
      group_id: groupId,
      name,
      research_fields: parseList(fields),
    });

    if (!created) return;

    setGroupId("");
    setName("");
    setFields("");
    setIsOpen(false);
  };

  return (
    <section className="bg-white p-6 rounded border border-gray-300">
      <div className="flex justify-between items-center mb-3">
        <h2 className="text-lg font-bold text-[#333333]">
          Criar Grupo de Pesquisa
        </h2>
        <button
          onClick={() => setIsOpen(!isOpen)}
          className="btn-green text-xs"
        >
          {isOpen ? "Cancelar" : "+ Criar Grupo"}
        </button>
      </div>
      <p className="text-sm text-gray-600 mb-4">
        Ao criar um grupo, você se torna o <strong>Líder/Orientador</strong>
        automaticamente.
      </p>

      {isOpen && (
        <form
          onSubmit={handleSubmit}
          className="space-y-3 pt-2 border-t border-gray-200"
        >
          <FormField label="Código ID do Grupo (Chave Única)">
            <input
              type="text"
              required
              placeholder="Ex: IA-002, ROB-001"
              value={groupId}
              onChange={(e) => setGroupId(e.target.value)}
              className={inputClassName}
            />
          </FormField>

          <FormField label="Nome do Grupo de Pesquisa">
            <input
              type="text"
              required
              placeholder="Ex: Grupo de Robótica e Automação"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={inputClassName}
            />
          </FormField>

          <FormField label="Três linhas de pesquisa (separadas por vírgula)">
            <input
              type="text"
              required
              placeholder="Ex: Robótica Móvel, Sistemas Autônomos"
              value={fields}
              onChange={(e) => setFields(e.target.value)}
              className={inputClassName}
            />
          </FormField>

          <button
            type="submit"
            disabled={feedback.loading}
            className="btn-green text-sm w-full"
          >
            {feedback.loading ? "Criando Grupo..." : "Confirmar e Criar Grupo"}
          </button>
        </form>
      )}
    </section>
  );
}
