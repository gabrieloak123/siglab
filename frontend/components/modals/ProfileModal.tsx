import { useEffect, useState, type FormEvent } from "react";
import Script from "next/script";

import { AlertBanner } from "@/components/ui/AlertBanner";
import { DangerZone } from "@/components/ui/DangerZone";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { Modal } from "@/components/ui/Modal";
import type { ProfilePayload } from "@/hooks/useAuth";
import type { Feedback } from "@/hooks/useFeedback";
import { formatList, parseList } from "@/lib/utils";
import { authApi, messageOf } from "@/lib/api";
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
  const [authorId, setAuthorId] = useState(user.authorId ?? "");
  const [interests, setInterests] = useState(() =>
    formatList(user.interest_areas)
  );
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  const [orcidEnabled, setOrcidEnabled] = useState(false);
  useEffect(() => { void authApi.orcidStatus().then(value => setOrcidEnabled(value.configured)).catch(() => setOrcidEnabled(false)); }, []);
  const initGoogle = () => {
    if (!googleClientId || !window.google?.accounts?.id) return;
    window.google.accounts.id.initialize({ client_id: googleClientId, callback: async ({ credential }) => {
      try { await authApi.linkGoogle(credential); feedback.succeed("Conta Google vinculada ao perfil."); }
      catch (error) { feedback.fail(messageOf(error, { api: "Não foi possível vincular a conta Google.", network: "Falha de conexão." })); }
    }});
    const target = document.getElementById("siglab-google-link-button");
    if (target) window.google.accounts.id.renderButton(target, { theme: "outline", size: "large", text: "continue_with" });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    await onUpdateProfile({
      name,
      email,
      interest_areas: parseList(interests),
      password,
      authorId,
    });
  };

  return (
    <Modal scrollable>
      {googleClientId && <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" onReady={initGoogle} />}
      <h2 className="text-xl font-bold text-[#333333] mb-4 border-b border-gray-200 pb-2">
        Gerenciar Perfil do Usuário
      </h2>

      <AlertBanner message={feedback.error} variant="error" size="sm" />

      <form onSubmit={handleSubmit} className="space-y-4 mb-6">
        <FormField label="Google Scholar author_id (opcional)">
          <input type="text" value={authorId} onChange={(e) => setAuthorId(e.target.value)} className={inputClassName} />
        </FormField>
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

      {googleClientId && <div className="border-t border-gray-200 py-4 space-y-2">
        <h3 className="text-sm font-bold">Vincular conta Google</h3>
        <p className="text-xs text-gray-600">Use o mesmo e-mail deste perfil para poder entrar com Google.</p>
        <div id="siglab-google-link-button" />
      </div>}
      <div className="border-t border-gray-200 py-4 space-y-2">
        <h3 className="text-sm font-bold">Verificação de identidade</h3>
        <p className="text-xs text-gray-600">Status: {user.verificationLevel === "ORCID" ? `ORCID verificado${user.orcid ? ` (${user.orcid})` : ""}` : user.verificationLevel === "INSTITUCIONAL" ? `Conta institucional verificada${user.institutionalDomain ? ` (${user.institutionalDomain})` : ""}` : "Perfil ainda não verificado"}.</p>
        {orcidEnabled && user.verificationLevel !== "ORCID" && <a href="/backend-api/api/auth/orcid/start" className="btn-green text-sm inline-block">Vincular ORCID</a>}
      </div>

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
