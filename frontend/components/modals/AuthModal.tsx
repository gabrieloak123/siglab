import { useEffect, useState, type FormEvent } from "react";
import Script from "next/script";

import { AlertBanner } from "@/components/ui/AlertBanner";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { Modal } from "@/components/ui/Modal";
import type { Feedback } from "@/hooks/useFeedback";
import type { RegisterPayload } from "@/hooks/useAuth";
import { parseList } from "@/lib/utils";
import { authApi, messageOf } from "@/lib/api";
import { readDiscovery } from "@/lib/discovery";
import type { AuthTab } from "@/types";

interface AuthModalProps {
  open: boolean;
  feedback: Feedback;
  onClose: () => void;
  onLogin: (email: string, password: string) => Promise<boolean>;
  onGoogle: (credential: string, interestAreas: string[]) => Promise<boolean>;
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
  const [tab, setTab] = useState<AuthTab>(() => readDiscovery() ? "register" : "login");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [interests, setInterests] = useState(() => readDiscovery()?.areas.join(", ") ?? "");
  const [googleCredential, setGoogleCredential] = useState("");
  const [discovering, setDiscovering] = useState(false);
  const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

  useEffect(() => {
    const receive = (event: MessageEvent) => {
      if (event.origin !== window.location.origin || event.data?.type !== "siglab-interest-discovery") return;
      if (Array.isArray(event.data.areas)) setInterests(event.data.areas.join(", "));
      setDiscovering(false);
    };
    window.addEventListener("message", receive);
    return () => window.removeEventListener("message", receive);
  }, []);

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
    });

    if (!registered) return;

    setName("");
    setEmail("");
    setPassword("");
    setInterests("");
  };

  const initGoogle = () => {
    if (!googleClientId || !window.google?.accounts?.id) return;
    window.google.accounts.id.initialize({ client_id: googleClientId, callback: async ({ credential }) => {
      try {
        const preview=await authApi.googlePreview(credential);
        if (preview.existingUser) { if (await onGoogle(credential, [])) onClose(); return; }
        setGoogleCredential(credential);
        setName(preview.name);
        setEmail(preview.email);
        setTab("register");
        feedback.clear();
      } catch (error) { feedback.fail(messageOf(error, { api: "Falha ao preparar entrada com Google.", network: "Falha de conexão." })); }
    }});
    const target = document.getElementById("siglab-google-button");
    if (target) window.google.accounts.id.renderButton(target, { theme: "outline", size: "large", text: "signin_with" });
  };

  const finishGoogle = async () => {
    const areas=parseList(interests);
    if (new Set(areas.map(area => area.toLocaleLowerCase())).size !== 3) {
      feedback.fail("Confirme exatamente três áreas de interesse distintas."); return;
    }
    if (await onGoogle(googleCredential, areas)) onClose();
  };

  if (discovering) return <Modal scrollable>
    <div className="mb-3 flex items-center justify-between gap-3"><h2 className="text-lg font-bold">Descobrir minhas áreas de interesse</h2><button type="button" className="btn-dark text-sm" onClick={() => setDiscovering(false)}>Voltar ao cadastro</button></div>
    <iframe title="Seleção de publicações" src="/descobrir-interesses?embedded=1" sandbox="allow-scripts allow-same-origin" className="h-[70vh] w-full rounded border border-gray-300" />
  </Modal>;

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
          <FormField label="Três áreas de interesse (complete as ausentes)">
            <input
              type="text"
              placeholder="Ex: Inteligência Artificial, Aprendizado de Máquina"
              value={interests}
              onChange={(e) => setInterests(e.target.value)}
              className={inputClassName}
            />
          </FormField>
          <div className="rounded border border-green-200 bg-green-50 p-3 text-sm">
            <button type="button" onClick={() => setDiscovering(true)} className="cursor-pointer font-bold text-[#3E885B] underline">Descobrir minhas áreas de interesse</button>
            {readDiscovery() && <p className="mt-1 text-xs">Seleção pronta. Confira e complete as três áreas antes de se cadastrar.</p>}
          </div>

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
        <p className="text-xs text-gray-600">Entre com Google. No primeiro acesso, nome e e-mail serão preenchidos automaticamente; confirme somente suas três áreas.</p>
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
