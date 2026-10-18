import { useState, type FormEvent } from "react";
import Script from "next/script";

import { AlertBanner } from "@/components/ui/AlertBanner";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { Modal } from "@/components/ui/Modal";
import type { Feedback } from "@/hooks/useFeedback";
import type { RegisterPayload } from "@/hooks/useAuth";
import { parseList } from "@/lib/utils";
import { authApi, messageOf } from "@/lib/api";
import type { AuthTab, ScholarCandidate } from "@/types";

interface AuthModalProps {
  open: boolean;
  feedback: Feedback;
  onClose: () => void;
  onLogin: (email: string, password: string) => Promise<boolean>;
  onGoogle: (credential: string, authorId: string, interestAreas: string[]) => Promise<boolean>;
  onRegister: (payload: RegisterPayload) => Promise<boolean>;
}

export function AuthModal({
  open,
  feedback,
  onClose,
  onLogin,
  onRegister,
  onGoogle,
}: AuthModalProps) {
  const [tab, setTab] = useState<AuthTab>("login");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [interests, setInterests] = useState("");
  const [authorId, setAuthorId] = useState("");
  const [scholarAreas, setScholarAreas] = useState<string[]>([]);
  const [googleCredential, setGoogleCredential] = useState("");
  const [candidates, setCandidates] = useState<ScholarCandidate[]>([]);
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

  if (!open) return null;

  const switchTab = (next: AuthTab) => {
    setTab(next);
    feedback.clear();
  };

  const handleLogin = async (e: FormEvent) => {
    e.preventDefault();

    if (!email.trim()) {
      feedback.fail("Por favor, informe seu e-mail.");
      return;
    }

    if (await onLogin(email, password)) { setEmail(""); setPassword(""); }
  };

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();

    if (!name.trim() || !email.trim() || !password.trim()) {
      feedback.fail("Por favor, preencha todos os campos obrigatórios.");
      return;
    }

    if (new Set(parseList(interests).map((area) => area.toLocaleLowerCase())).size !== 3) {
      feedback.fail("Informe exatamente três áreas de interesse distintas.");
      return;
    }

    const registered = await onRegister({
      name,
      email,
      password,
      interest_areas: parseList(interests),
      authorId,
    });

    if (!registered) return;

    setName("");
    setEmail("");
    setPassword("");
    setInterests("");
    setAuthorId("");
  };

  const fetchScholar = async () => {
    if (!authorId.trim()) {
      feedback.fail("Informe o author_id para consultar o Google Scholar.");
      return;
    }
    try {
      const areas = await authApi.scholar(authorId.trim());
      setScholarAreas(areas);
      setInterests(areas.slice(0, 3).join(", "));
      feedback.clear();
    } catch (error) {
      feedback.fail(messageOf(error, { api: "Não foi possível consultar o perfil Scholar.", network: "Falha de conexão." }));
    }
  };

  const initGoogle = () => {
    if (!googleClientId || !window.google?.accounts?.id) return;
    window.google.accounts.id.initialize({ client_id: googleClientId, callback: async ({ credential }) => {
      try {
        const preview=await authApi.googlePreview(credential);
        if (preview.existingUser) { if (await onGoogle(credential, "", [])) onClose(); return; }
        setGoogleCredential(credential); setCandidates(preview.candidates);
        if (preview.candidates.length > 0) selectCandidate(preview.candidates[0]);
        feedback.clear();
      } catch (error) { feedback.fail(messageOf(error, { api: "Falha ao preparar entrada com Google.", network: "Falha de conexão." })); }
    }});
    const target = document.getElementById("siglab-google-button");
    if (target) window.google.accounts.id.renderButton(target, { theme: "outline", size: "large", text: "signin_with" });
  };

  const selectCandidate = (candidate: ScholarCandidate) => {
    setAuthorId(candidate.authorId); setScholarAreas(candidate.interests);
    setInterests(candidate.interests.slice(0, 3).join(", "));
  };
  const finishGoogle = async () => {
    const areas=parseList(interests);
    if (new Set(areas.map(area => area.toLocaleLowerCase())).size !== 3) {
      feedback.fail("Confirme exatamente três áreas de interesse distintas."); return;
    }
    if (await onGoogle(googleCredential, authorId, areas)) onClose();
  };

  return (
    <Modal>
      {googleClientId && <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" onReady={initGoogle} />}
      <div className="flex border-b border-gray-300 mb-6">
        <button
          onClick={() => switchTab("login")}
          className={`flex-1 py-2 font-bold text-sm text-center ${
            tab === "login"
              ? "border-b-2 border-[#3E885B] text-[#3E885B]"
              : "text-gray-500"
          }`}
        >
          Login
        </button>
        <button
          onClick={() => switchTab("register")}
          className={`flex-1 py-2 font-bold text-sm text-center ${
            tab === "register"
              ? "border-b-2 border-[#3E885B] text-[#3E885B]"
              : "text-gray-500"
          }`}
        >
          Cadastrar
        </button>
      </div>

      <AlertBanner message={feedback.error} variant="error" size="sm" />

      {tab === "login" ? (
        <form onSubmit={handleLogin} className="space-y-4">
          <FormField label="E-mail do Usuário">
            <input
              type="email"
              required
              placeholder="seu.email@ufrn.br"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={inputClassName}
            />
          </FormField>
          <FormField label="Senha">
            <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)} className={inputClassName} />
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
              {feedback.loading ? "Entrando..." : "Entrar"}
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={handleRegister} className="space-y-4">
          <FormField label="Nome Completo">
            <input
              type="text"
              required
              placeholder="Ex: Maria da Silva"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={inputClassName}
            />
          </FormField>

          <FormField label="E-mail">
            <input
              type="email"
              required
              placeholder="maria@ufrn.br"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={inputClassName}
            />
          </FormField>

          <FormField label="Senha">
            <input
              type="password"
              required
              placeholder="Sua senha"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={inputClassName}
            />
          </FormField>
          <FormField label="Google Scholar author_id (opcional)">
            <div className="flex gap-2">
              <input value={authorId} onChange={(e) => setAuthorId(e.target.value)} className={inputClassName} />
              <button type="button" onClick={fetchScholar} className="btn-dark text-xs">Buscar áreas</button>
            </div>
          </FormField>
          {scholarAreas.length > 0 && <p className="text-xs text-gray-600">Áreas encontradas: {scholarAreas.join(", ")}</p>}

          <FormField label="Três áreas de interesse (complete as ausentes)">
            <input
              type="text"
              placeholder="Ex: Inteligência Artificial, Aprendizado de Máquina"
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
              {feedback.loading ? "Cadastrando..." : "Cadastrar"}
            </button>
          </div>
        </form>
      )}
      <div className="border-t border-gray-200 pt-4 mt-4 space-y-2">
        <p className="text-xs text-gray-600">Entre com Google. No primeiro acesso, localizamos possíveis perfis Scholar para você apenas confirmar.</p>
        {googleCredential && candidates.length > 0 && <div className="space-y-2"><p className="text-xs font-bold">Confirme seu perfil Scholar (opcional):</p>{candidates.map(candidate => <button type="button" key={candidate.authorId} onClick={() => selectCandidate(candidate)} className={`w-full text-left border rounded p-2 text-xs ${authorId === candidate.authorId ? "border-[#3E885B] bg-green-50" : "border-gray-200"}`}><span className="font-bold">{candidate.name}</span><br />{candidate.affiliation}{candidate.verifiedDomain && ` · ${candidate.verifiedDomain}`}</button>)}</div>}
        {googleCredential && candidates.length === 0 && <p className="text-xs text-amber-700">Nenhum perfil Scholar foi localizado. Informe somente suas três áreas.</p>}
        {tab === "login" && googleCredential && <div className="flex gap-2"><input value={authorId} onChange={(e) => setAuthorId(e.target.value)} placeholder="author_id (opcional)" className={inputClassName} /><button type="button" onClick={fetchScholar} className="btn-dark text-xs">Buscar áreas</button></div>}
        {tab === "login" && scholarAreas.length > 0 && <p className="text-xs text-gray-600">Áreas do Scholar: {scholarAreas.join(", ")}. Complete as ausentes abaixo até ter três.</p>}
        {tab === "login" && googleCredential && <input value={interests} onChange={(e) => setInterests(e.target.value)} placeholder="Três áreas, separadas por vírgula" className={inputClassName} />}
        {googleCredential ? <button type="button" onClick={finishGoogle} disabled={feedback.loading} className="btn-green text-sm">Confirmar e entrar</button> : googleClientId ? <div id="siglab-google-button" /> : <p className="text-xs text-gray-500">Configure NEXT_PUBLIC_GOOGLE_CLIENT_ID para habilitar entrada com Google.</p>}
      </div>
    </Modal>
  );
}

declare global {
  interface Window {
    google?: { accounts: { id: {
      initialize: (options: { client_id: string; callback: (response: { credential: string }) => void }) => void;
      renderButton: (element: HTMLElement, options: { theme: string; size: string; text: string }) => void;
    } } };
  }
}
