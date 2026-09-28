import { useState, type FormEvent } from "react";

import { AlertBanner } from "@/components/ui/AlertBanner";
import { DangerZone } from "@/components/ui/DangerZone";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { Modal } from "@/components/ui/Modal";
import type { ProfilePayload } from "@/hooks/useAuth";
import type { Feedback } from "@/hooks/useFeedback";
import { formatList, parseList } from "@/lib/utils";
import type { User } from "@/types";

interface ProfileModalProps {
  user: User;
  feedback: Feedback;
  onClose: () => void;
  onUpdateProfile: (payload: ProfilePayload) => Promise<User | null>;
  onDeleteProfile: (password: string) => Promise<boolean>;
}

export function ProfileModal({
  user,
  feedback,
  onClose,
  onUpdateProfile,
  onDeleteProfile,
}: ProfileModalProps) {
  const [name, setName] = useState(user.name ?? "");
  const [email, setEmail] = useState(user.email ?? "");
  const [password, setPassword] = useState("");
  const [interests, setInterests] = useState(() =>
    formatList(user.interest_areas)
  );

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await onUpdateProfile({
      name,
      email,
      interest_areas: parseList(interests),
      password,
    });
  };

  return (
    <Modal scrollable>
      <h2 className="text-xl font-bold text-[#333333] mb-4 border-b border-gray-200 pb-2">
        Gerenciar Perfil do Usuário
      </h2>

      <AlertBanner message={feedback.error} variant="error" size="sm" />

      <form onSubmit={handleSubmit} className="space-y-4 mb-6">
        <FormField label="Nome Completo">
          <input
            type="text"
            required
            value={name}
            onChange={(e) => setName(e.target.value)}
            className={inputClassName}
          />
        </FormField>

        <FormField label="E-mail">
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClassName}
          />
        </FormField>

        <FormField label="Nova Senha (opcional)">
          <input
            type="password"
            placeholder="Deixe em branco para não alterar"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={inputClassName}
          />
        </FormField>

        <FormField label="Áreas de Interesse (separadas por vírgula)">
          <input
            type="text"
            value={interests}
            onChange={(e) => setInterests(e.target.value)}
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
        title="Zona de Perigo"
        description="Para confirmar a exclusão da sua conta, digite sua senha abaixo:"
        triggerLabel="Excluir Conta de Usuário"
        confirmLabel="Confirmar Exclusão"
        passwordPlaceholder="Sua senha"
        emptyPasswordMessage="Por favor, digite sua senha para confirmar a exclusão da conta."
        onConfirm={onDeleteProfile}
        feedback={feedback}
      />
    </Modal>
  );
}
