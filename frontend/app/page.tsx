"use client";

import { useEffect, useState } from "react";

interface ResearchGroup {
  group_id: string;
  name: string;
  research_fields?: string[];
}

interface GroupMember {
  id: number;
  email: string;
  name: string;
  interest_areas?: string[];
  isLeader: boolean;
}

interface User {
  id?: number;
  email: string;
  name: string;
  interest_areas?: string[];
}

const getApiUrl = () => {
  if (typeof window !== "undefined") {
    return "http://localhost:8080";
  }
  return process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
};

export default function Home() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [allGroups, setAllGroups] = useState<ResearchGroup[]>([]);
  const [memberGroups, setMemberGroups] = useState<ResearchGroup[]>([]);
  const [leaderGroups, setLeaderGroups] = useState<ResearchGroup[]>([]);
  const [generatedTokens, setGeneratedTokens] = useState<Record<string, string>>({});

  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [authTab, setAuthTab] = useState<"login" | "register">("login");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const [loginEmail, setLoginEmail] = useState("");
  const [registerName, setRegisterName] = useState("");
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");
  const [registerInterests, setRegisterInterests] = useState("");

  const [joinHashInput, setJoinHashInput] = useState("");

  useEffect(() => {
    const savedUser = localStorage.getItem("siglab_user");
    if (savedUser) {
      try {
        setCurrentUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem("siglab_user");
      }
    }
    fetchPublicGroups();
  }, []);

  useEffect(() => {
    if (currentUser) {
      fetchUserGroups(currentUser.email);
    } else {
      setMemberGroups([]);
      setLeaderGroups([]);
    }
  }, [currentUser, allGroups]);

  const fetchPublicGroups = async () => {
    try {
      const res = await fetch(`${getApiUrl()}/api/research-groups`);
      if (res.ok) {
        const data = await res.json();
        setAllGroups(data);
      }
    } catch (err) {
      console.error("Erro ao carregar grupos públicos:", err);
    }
  };

  const fetchUserGroups = async (userEmail: string) => {
    try {
      const res = await fetch(`${getApiUrl()}/api/research-groups`);
      if (!res.ok) return;

      const groups: ResearchGroup[] = await res.json();
      const mGroups: ResearchGroup[] = [];
      const lGroups: ResearchGroup[] = [];

      for (const group of groups) {
        try {
          const membersRes = await fetch(
            `${getApiUrl()}/api/research-groups/${encodeURIComponent(group.group_id)}/users`
          );
          if (membersRes.ok) {
            const members: GroupMember[] = await membersRes.json();
            const userMember = members.find(
              (m) => m.email.toLowerCase() === userEmail.toLowerCase()
            );

            if (userMember) {
              mGroups.push(group);
              if (userMember.isLeader) {
                lGroups.push(group);
              }
            }
          }
        } catch (e) {
          console.error("Erro ao checar membros do grupo:", group.group_id, e);
        }
      }

      setMemberGroups(mGroups);
      setLeaderGroups(lGroups);
    } catch (err) {
      console.error("Erro ao buscar grupos do usuário:", err);
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!loginEmail.trim()) {
      setErrorMessage("Por favor, informe seu e-mail.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${getApiUrl()}/api/users`);
      if (res.ok) {
        const users: User[] = await res.json();
        const found = users.find(
          (u) => u.email.toLowerCase() === loginEmail.trim().toLowerCase()
        );

        if (found) {
          setCurrentUser(found);
          localStorage.setItem("siglab_user", JSON.stringify(found));
          setIsAuthModalOpen(false);
          setLoginEmail("");
          setSuccessMessage(`Bem-vindo(a), ${found.name}!`);
        } else {
          setErrorMessage(
            "Usuário não encontrado com este e-mail. Se for novo, selecione a aba 'Cadastrar'."
          );
        }
      } else {
        setErrorMessage("Erro ao consultar a API de usuários.");
      }
    } catch (err) {
      setErrorMessage("Falha na conexão com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!registerName.trim() || !registerEmail.trim() || !registerPassword.trim()) {
      setErrorMessage("Por favor, preencha todos os campos obrigatórios.");
      return;
    }

    setLoading(true);
    try {
      const interestArray = registerInterests
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

      const payload = {
        name: registerName.trim(),
        email: registerEmail.trim(),
        password: registerPassword.trim(),
        interest_areas: interestArray,
      };

      const res = await fetch(`${getApiUrl()}/api/users`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (res.ok || res.status === 201) {
        const newUser: User = await res.json();
        setCurrentUser(newUser);
        localStorage.setItem("siglab_user", JSON.stringify(newUser));
        setIsAuthModalOpen(false);
        setRegisterName("");
        setRegisterEmail("");
        setRegisterPassword("");
        setRegisterInterests("");
        setSuccessMessage(`Cadastro realizado com sucesso! Bem-vindo(a), ${newUser.name}.`);
      } else {
        const errorData = await res.json().catch(() => null);
        setErrorMessage(
          errorData?.message || "Não foi possível realizar o cadastro. Verifique os dados."
        );
      }
    } catch (err) {
      setErrorMessage("Falha ao comunicar com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    localStorage.removeItem("siglab_user");
    setSuccessMessage("Sessão encerrada com sucesso.");
    setGeneratedTokens({});
  };

  const handleJoinGroupWithHash = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!currentUser) {
      setErrorMessage("Você precisa estar logado para entrar em um grupo.");
      return;
    }

    if (!joinHashInput.trim()) {
      setErrorMessage("Digite uma hash de convite válida.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${getApiUrl()}/api/research-groups/join`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          inviteToken: joinHashInput.trim(),
          userEmail: currentUser.email,
        }),
      });

      if (res.ok) {
        setSuccessMessage("Ingresso realizado com sucesso no grupo de pesquisa!");
        setJoinHashInput("");
        fetchUserGroups(currentUser.email);
        fetchPublicGroups();
      } else {
        const errData = await res.json().catch(() => null);
        setErrorMessage(
          errData?.message || "Hash de convite inválida, expirada ou já utilizada."
        );
      }
    } catch (err) {
      setErrorMessage("Erro ao tentar entrar no grupo.");
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateInvite = async (groupId: string) => {
    if (!currentUser) return;
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const res = await fetch(
        `${getApiUrl()}/api/research-groups/${encodeURIComponent(groupId)}/invites?expirationDays=7`,
        {
          method: "POST",
          headers: {
            "X-Requester-Email": currentUser.email,
          },
        }
      );

      if (res.ok) {
        const data = await res.json();
        setGeneratedTokens((prev) => ({
          ...prev,
          [groupId]: data.inviteToken,
        }));
        setSuccessMessage(`Hash gerada para o grupo ${groupId}: ${data.inviteToken}`);
      } else {
        const errData = await res.json().catch(() => null);
        setErrorMessage(
          errData?.message || "Apenas líderes do grupo têm permissão para gerar hashes de convite."
        );
      }
    } catch (err) {
      setErrorMessage("Erro ao gerar hash de convite.");
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#F6F4F3] text-[#333333]">
      <header className="bg-[#333333] text-[#F6F4F3] py-4 px-6 shadow-sm flex justify-between items-center">
        <div>
          <h1 className="text-xl font-bold tracking-tight">SIGLab</h1>
          <p className="text-xs text-gray-300">Sistema de Grupos de Pesquisa</p>
        </div>

        {currentUser ? (
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium">
              Logado como: <strong>{currentUser.name}</strong> ({currentUser.email})
            </span>
            <button onClick={handleLogout} className="btn-rust text-sm">
              Sair
            </button>
          </div>
        ) : (
          <button
            onClick={() => {
              setErrorMessage("");
              setSuccessMessage("");
              setIsAuthModalOpen(true);
            }}
            className="btn-green text-sm"
          >
            Cadastrar / Login
          </button>
        )}
      </header>

      <main className="flex-1 max-w-5xl w-full mx-auto p-6 animate-fade">
        {errorMessage && (
          <div className="mb-6 p-4 bg-[#BF3100] text-white rounded font-medium text-sm">
            {errorMessage}
          </div>
        )}

        {successMessage && (
          <div className="mb-6 p-4 bg-[#3E885B] text-white rounded font-medium text-sm">
            {successMessage}
          </div>
        )}

        {currentUser ? (
          <div className="space-y-8">
            <section className="bg-white p-6 rounded border border-gray-300">
              <h2 className="text-lg font-bold text-[#333333] mb-2">
                Entrar em um Grupo de Pesquisa
              </h2>
              <p className="text-sm text-gray-600 mb-4">
                Cole abaixo a hash/token de convite gerada pelo orientador/líder do grupo.
              </p>
              <form onSubmit={handleJoinGroupWithHash} className="flex gap-3 max-w-xl">
                <input
                  type="text"
                  placeholder="Ex: INV-IA-001-A1B2C3D4"
                  value={joinHashInput}
                  onChange={(e) => setJoinHashInput(e.target.value)}
                  className="flex-1 px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                />
                <button
                  type="submit"
                  disabled={loading}
                  className="btn-green text-sm whitespace-nowrap"
                >
                  {loading ? "Entrando..." : "Entrar no Grupo"}
                </button>
              </form>
            </section>

            <section className="bg-white p-6 rounded border border-gray-300">
              <h2 className="text-lg font-bold text-[#333333] mb-4">
                Meus Grupos de Pesquisa (Membro)
              </h2>

              {memberGroups.length === 0 ? (
                <p className="text-sm text-gray-500 italic">
                  Você ainda não é membro de nenhum grupo de pesquisa. Use o campo acima para entrar com uma hash.
                </p>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {memberGroups.map((group) => (
                    <div
                      key={group.group_id}
                      className="p-4 border border-gray-200 rounded bg-[#F6F4F3]"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <span className="text-xs font-bold bg-[#333333] text-white px-2 py-0.5 rounded">
                          {group.group_id}
                        </span>
                      </div>
                      <h3 className="font-bold text-[#333333] text-base mb-1">{group.name}</h3>
                      {group.research_fields && group.research_fields.length > 0 && (
                        <div className="flex flex-wrap gap-1 mt-2">
                          {group.research_fields.map((field, idx) => (
                            <span
                              key={idx}
                              className="text-xs bg-white text-[#333333] border border-gray-300 px-2 py-0.5 rounded"
                            >
                              {field}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </section>

            <section className="bg-white p-6 rounded border border-gray-300">
              <h2 className="text-lg font-bold text-[#333333] mb-4">
                Grupos em que sou Orientador / Líder
              </h2>

              {leaderGroups.length === 0 ? (
                <p className="text-sm text-gray-500 italic">
                  Você não é líder de nenhum grupo de pesquisa no momento.
                </p>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {leaderGroups.map((group) => (
                    <div
                      key={group.group_id}
                      className="p-4 border border-[#3E885B] rounded bg-white"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <span className="text-xs font-bold bg-[#3E885B] text-white px-2 py-0.5 rounded">
                          {group.group_id} (Líder)
                        </span>
                      </div>
                      <h3 className="font-bold text-[#333333] text-base mb-2">{group.name}</h3>

                      <button
                        onClick={() => handleGenerateInvite(group.group_id)}
                        className="btn-dark text-xs mb-3"
                      >
                        Gerar Hash de Convite
                      </button>

                      {generatedTokens[group.group_id] && (
                        <div className="p-2 bg-[#F6F4F3] border border-gray-300 rounded text-xs">
                          <span className="text-gray-600 block mb-1">Hash Gerada (Válida por 7 dias):</span>
                          <code className="font-mono font-bold text-[#BF3100] break-all">
                            {generatedTokens[group.group_id]}
                          </code>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        ) : (

          <div className="space-y-6">
            <div className="bg-white p-6 rounded border border-gray-300">
              <h2 className="text-xl font-bold text-[#333333] mb-2">
                Grupos de Pesquisa Existentes
              </h2>
              <p className="text-sm text-gray-600">
                Abaixo estão listados todos os grupos de pesquisa cadastrados no sistema SIGLab.
                Faça login ou cadastre-se para participar de um grupo.
              </p>
            </div>

            {allGroups.length === 0 ? (
              <div className="bg-white p-8 rounded text-center border border-gray-300 text-gray-500">
                Nenhum grupo de pesquisa cadastrado no sistema até o momento.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {allGroups.map((group) => (
                  <div
                    key={group.group_id}
                    className="bg-white p-5 rounded border border-gray-300 flex flex-col justify-between"
                  >
                    <div>
                      <span className="inline-block text-xs font-bold bg-[#333333] text-white px-2 py-0.5 rounded mb-3">
                        {group.group_id}
                      </span>
                      <h3 className="font-bold text-[#333333] text-lg mb-2">{group.name}</h3>

                      {group.research_fields && group.research_fields.length > 0 && (
                        <div className="mt-3">
                          <span className="text-xs text-gray-500 block mb-1">Linhas de Pesquisa:</span>
                          <div className="flex flex-wrap gap-1">
                            {group.research_fields.map((field, idx) => (
                              <span
                                key={idx}
                                className="text-xs bg-[#F6F4F3] text-[#333333] border border-gray-300 px-2 py-0.5 rounded"
                              >
                                {field}
                              </span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>


      {isAuthModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex justify-center items-center p-4 z-50 animate-fade">
          <div className="bg-white max-w-md w-full rounded border border-gray-300 p-6 shadow-md">
            <div className="flex border-b border-gray-300 mb-6">
              <button
                onClick={() => {
                  setAuthTab("login");
                  setErrorMessage("");
                }}
                className={`flex-1 py-2 font-bold text-sm text-center ${
                  authTab === "login"
                    ? "border-b-2 border-[#3E885B] text-[#3E885B]"
                    : "text-gray-500"
                }`}
              >
                Login
              </button>
              <button
                onClick={() => {
                  setAuthTab("register");
                  setErrorMessage("");
                }}
                className={`flex-1 py-2 font-bold text-sm text-center ${
                  authTab === "register"
                    ? "border-b-2 border-[#3E885B] text-[#3E885B]"
                    : "text-gray-500"
                }`}
              >
                Cadastrar
              </button>
            </div>

            {errorMessage && (
              <div className="mb-4 p-3 bg-[#BF3100] text-white rounded text-xs font-medium">
                {errorMessage}
              </div>
            )}

            {authTab === "login" ? (
              <form onSubmit={handleLogin} className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-[#333333] mb-1">
                    E-mail do Usuário
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="seu.email@ufrn.br"
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                  />
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setIsAuthModalOpen(false)}
                    className="btn-dark text-sm"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="btn-green text-sm"
                  >
                    {loading ? "Entrando..." : "Entrar"}
                  </button>
                </div>
              </form>
            ) : (
              <form onSubmit={handleRegister} className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-[#333333] mb-1">
                    Nome Completo
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: Maria da Silva"
                    value={registerName}
                    onChange={(e) => setRegisterName(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#333333] mb-1">
                    E-mail
                  </label>
                  <input
                    type="email"
                    required
                    placeholder="maria@ufrn.br"
                    value={registerEmail}
                    onChange={(e) => setRegisterEmail(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#333333] mb-1">
                    Senha
                  </label>
                  <input
                    type="password"
                    required
                    placeholder="Sua senha"
                    value={registerPassword}
                    onChange={(e) => setRegisterPassword(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#333333] mb-1">
                    Áreas de Interesse (Separadas por vírgula)
                  </label>
                  <input
                    type="text"
                    placeholder="Ex: Inteligência Artificial, Aprendizado de Máquina"
                    value={registerInterests}
                    onChange={(e) => setRegisterInterests(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                  />
                </div>

                <div className="flex justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => setIsAuthModalOpen(false)}
                    className="btn-dark text-sm"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="btn-green text-sm"
                  >
                    {loading ? "Cadastrando..." : "Cadastrar"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      <footer className="bg-[#333333] text-gray-400 text-xs py-4 px-6 text-center mt-auto">
        SIGLab &copy; 2026 - Departamento de Informática e Matemática Aplicada (DIMAP / UFRN)
      </footer>
    </div>
  );
}
