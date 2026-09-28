import { useState, type FormEvent } from "react";

import { AlertBanner } from "@/components/ui/AlertBanner";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { Modal } from "@/components/ui/Modal";
import type { Feedback } from "@/hooks/useFeedback";
import type { RegisterPayload } from "@/hooks/useAuth";
import { parseList } from "@/lib/utils";
import type { AuthTab } from "@/types";

interface AuthModalProps {
  open: boolean;
  feedback: Feedback;
  onClose: () => void;
  onLogin: (email: string) => Promise<boolean>;
  onRegister: (payload: RegisterPayload) => Promise<boolean>;
}

export function AuthModal({
  open,
  feedback,
  onClose,
  onLogin,
  onRegister,
}: AuthModalProps) {
  const [tab, setTab] = useState<AuthTab>("login");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [interests, setInterests] = useState("");

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

    if (await onLogin(email)) setEmail("");
  };

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();

    if (!name.trim() || !email.trim() || !password.trim()) {
      feedback.fail("Por favor, preencha todos os campos obrigatórios.");
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

  return (
    <Modal>
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

          <FormField label="Áreas de Interesse (Separadas por vírgula)">
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
    </Modal>
  );
}
