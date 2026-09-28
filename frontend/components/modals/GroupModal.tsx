import { useState, type FormEvent } from "react";

import { AlertBanner } from "@/components/ui/AlertBanner";
import { DangerZone } from "@/components/ui/DangerZone";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { GroupBadge } from "@/components/ui/GroupBadge";
import { Modal } from "@/components/ui/Modal";
import type { Feedback } from "@/hooks/useFeedback";
import type { GroupDraft } from "@/hooks/useResearchGroups";
import { formatList, parseList } from "@/lib/utils";
import type { ResearchGroup } from "@/types";

interface GroupModalProps {
  group: ResearchGroup;
  feedback: Feedback;
  onClose: () => void;
  onUpdateGroup: (payload: GroupDraft) => Promise<boolean>;
  onDeleteGroup: (password: string) => Promise<boolean>;
}

export function GroupModal({
  group,
  feedback,
  onClose,
  onUpdateGroup,
  onDeleteGroup,
}: GroupModalProps) {
  const [name, setName] = useState(group.name ?? "");
  const [fields, setFields] = useState(() =>
    formatList(group.research_fields)
  );

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    await onUpdateGroup({ name, research_fields: parseList(fields) });
  };

  return (
    <Modal scrollable>
      <div className="flex justify-between items-center mb-4 border-b border-gray-200 pb-2">
        <h2 className="text-xl font-bold text-[#333333]">
          Gerenciar Grupo de Pesquisa
        </h2>
        <GroupBadge>{group.group_id}</GroupBadge>
      </div>

      <AlertBanner message={feedback.error} variant="error" size="sm" />

      <form onSubmit={handleSubmit} className="space-y-4 mb-6">
        <FormField label="Nome do Grupo de Pesquisa">
          <input
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClassName}
          />
        </FormField>

        <FormField label="Linhas de Pesquisa (separadas por vírgula)">
          <input
            type="text"
            value={fields}
            onChange={(e) => setFields(e.target.value)}
            className={inputClassName}
          />
        </FormField>

        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className="btn-dark text-sm">
            Cancelar
          </button>
          <button
            type="submit"
            disabled={feedback.loading}
            className="btn-green text-sm"
          >
            {feedback.loading ? "Salvando..." : "Salvar Alterações"}
          </button>
        </div>
      </form>

      <DangerZone
        title="Zona de Perigo (Apenas Líder)"
        description={
          <>
            Para confirmar a exclusão do grupo, digite a{" "}
            <strong>senha do orientador</strong>:
          </>
        }
        triggerLabel="Excluir Grupo de Pesquisa"
        confirmLabel="Confirmar Exclusão"
        passwordPlaceholder="Senha do orientador"
        emptyPasswordMessage="Por favor, digite a senha do orientador para confirmar a exclusão do grupo."
        onConfirm={onDeleteGroup}
        feedback={feedback}
      />
    </Modal>
  );
}
