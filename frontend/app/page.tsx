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
  password?: string;
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

  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
  const [newGroupId, setNewGroupId] = useState("");
  const [newGroupName, setNewGroupName] = useState("");
  const [newGroupFields, setNewGroupFields] = useState("");

  // Modal Gerenciar Perfil (Usuário)
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [profileName, setProfileName] = useState("");
  const [profileEmail, setProfileEmail] = useState("");
  const [profilePassword, setProfilePassword] = useState("");
  const [profileInterests, setProfileInterests] = useState("");
  const [deleteProfilePassword, setDeleteProfilePassword] = useState("");
  const [isDeleteProfileConfirmOpen, setIsDeleteProfileConfirmOpen] = useState(false);

  // Modal Gerenciar Grupo (Líder)
  const [isGroupModalOpen, setIsGroupModalOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<ResearchGroup | null>(null);
  const [editGroupName, setEditGroupName] = useState("");
  const [editGroupFields, setEditGroupFields] = useState("");
  const [deleteGroupPassword, setDeleteGroupPassword] = useState("");
  const [isDeleteGroupConfirmOpen, setIsDeleteGroupConfirmOpen] = useState(false);

  useEffect(() => {
    const savedUser = localStorage.getItem("siglab_user");
    if (savedUser) {
      try {
        const u = JSON.parse(savedUser);
        setCurrentUser(u);
        fetch(`${getApiUrl()}/api/users`)
          .then((res) => (res.ok ? res.json() : null))
          .then((users: User[] | null) => {
            if (users) {
              const found = users.find((x) => x.email.toLowerCase() === u.email.toLowerCase());
              if (found) {
                setCurrentUser(found);
                localStorage.setItem("siglab_user", JSON.stringify(found));
              }
            }
          })
          .catch(() => {});
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

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    if (!currentUser) {
      setErrorMessage("Você precisa estar logado para criar um grupo de pesquisa.");
      return;
    }

    if (!newGroupId.trim() || !newGroupName.trim()) {
      setErrorMessage("Por favor, informe o Código ID e o Nome do Grupo de Pesquisa.");
      return;
    }

    setLoading(true);
    try {
      const fieldsArray = newGroupFields
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

      const groupRes = await fetch(`${getApiUrl()}/api/research-groups`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          group_id: newGroupId.trim(),
          name: newGroupName.trim(),
          research_fields: fieldsArray,
        }),
      });

      if (!groupRes.ok && groupRes.status !== 201) {
        const errData = await groupRes.json().catch(() => null);
        setErrorMessage(
          errData?.message || "Erro ao criar grupo de pesquisa. Verifique se o Código ID já existe."
        );
        setLoading(false);
        return;
      }

      const leaderRes = await fetch(
        `${getApiUrl()}/api/research-groups/${encodeURIComponent(newGroupId.trim())}/members/${encodeURIComponent(currentUser.email)}/leader?isLeader=true`,
        {
          method: "PUT",
        }
      );

      if (leaderRes.ok) {
        setSuccessMessage(`Grupo de pesquisa '${newGroupName.trim()}' criado com sucesso! Você foi definido como líder/orientador.`);
        setNewGroupId("");
        setNewGroupName("");
        setNewGroupFields("");
        setIsCreateGroupOpen(false);
        fetchUserGroups(currentUser.email);
        fetchPublicGroups();
      } else {
        setErrorMessage("Grupo criado, mas ocorreu uma falha ao registrar o líder.");
      }
    } catch (err) {
      setErrorMessage("Falha na comunicação ao tentar criar o grupo.");
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

  // Gerenciar Perfil (Usuário)
  const openProfileModal = async () => {
    if (!currentUser) return;
    setErrorMessage("");
    setSuccessMessage("");
    setProfileName(currentUser.name || "");
    setProfileEmail(currentUser.email || "");
    setProfilePassword("");
    setProfileInterests((currentUser.interest_areas || []).join(", "));
    setDeleteProfilePassword("");
    setIsDeleteProfileConfirmOpen(false);

    if (!currentUser.id) {
      try {
        const res = await fetch(`${getApiUrl()}/api/users`);
        if (res.ok) {
          const users: User[] = await res.json();
          const found = users.find((u) => u.email.toLowerCase() === currentUser.email.toLowerCase());
          if (found) {
            setCurrentUser(found);
            localStorage.setItem("siglab_user", JSON.stringify(found));
          }
        }
      } catch (err) {
        console.error("Erro ao buscar dados do usuário para o perfil:", err);
      }
    }

    setIsProfileModalOpen(true);
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser || !currentUser.id) {
      setErrorMessage("Não foi possível localizar o ID do usuário para alteração.");
      return;
    }

    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const interestArray = profileInterests
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

      const payload: Partial<User> = {
        name: profileName.trim(),
        email: profileEmail.trim(),
        interest_areas: interestArray,
      };

      if (profilePassword.trim()) {
        payload.password = profilePassword.trim();
      }

      const res = await fetch(`${getApiUrl()}/api/users/${currentUser.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (res.ok) {
        const updatedUser: User = await res.json();
        setCurrentUser(updatedUser);
        localStorage.setItem("siglab_user", JSON.stringify(updatedUser));
        setIsProfileModalOpen(false);
        setSuccessMessage("Perfil atualizado com sucesso!");
        fetchUserGroups(updatedUser.email);
      } else {
        const errData = await res.json().catch(() => null);
        setErrorMessage(errData?.message || "Erro ao atualizar dados do perfil.");
      }
    } catch (err) {
      setErrorMessage("Falha na comunicação com o servidor ao atualizar perfil.");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser || !currentUser.id) {
      setErrorMessage("Não foi possível identificar o usuário para exclusão.");
      return;
    }

    if (!deleteProfilePassword.trim()) {
      setErrorMessage("Por favor, digite sua senha para confirmar a exclusão da conta.");
      return;
    }

    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const res = await fetch(
        `${getApiUrl()}/api/users/${currentUser.id}?password=${encodeURIComponent(deleteProfilePassword.trim())}`,
        {
          method: "DELETE",
          headers: {
            "X-Requester-Email": currentUser.email,
          },
        }
      );

      if (res.ok) {
        setIsProfileModalOpen(false);
        handleLogout();
        setSuccessMessage("Sua conta de usuário foi excluída com sucesso.");
        fetchPublicGroups();
      } else {
        const errData = await res.json().catch(() => null);
        setErrorMessage(
          errData?.message || "Senha incorreta ou erro ao tentar excluir a conta."
        );
      }
    } catch (err) {
      setErrorMessage("Falha na comunicação com o servidor.");
    } finally {
      setLoading(false);
    }
  };

  // Gerenciar Grupo (Líder)
  const openGroupModal = (group: ResearchGroup) => {
    setErrorMessage("");
    setSuccessMessage("");
    setSelectedGroup(group);
    setEditGroupName(group.name || "");
    setEditGroupFields((group.research_fields || []).join(", "));
    setDeleteGroupPassword("");
    setIsDeleteGroupConfirmOpen(false);
    setIsGroupModalOpen(true);
  };

  const handleUpdateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedGroup) return;

    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const fieldsArray = editGroupFields
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0);

      const payload = {
        group_id: selectedGroup.group_id,
        name: editGroupName.trim(),
        research_fields: fieldsArray,
      };

      const res = await fetch(
        `${getApiUrl()}/api/research-groups/${encodeURIComponent(selectedGroup.group_id)}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        }
      );

      if (res.ok) {
        setIsGroupModalOpen(false);
        setSuccessMessage(`Grupo '${selectedGroup.group_id}' atualizado com sucesso!`);
        if (currentUser) {
          fetchUserGroups(currentUser.email);
        }
        fetchPublicGroups();
      } else {
        const errData = await res.json().catch(() => null);
        setErrorMessage(errData?.message || "Erro ao atualizar dados do grupo.");
      }
    } catch (err) {
      setErrorMessage("Falha ao comunicar com o servidor ao atualizar grupo.");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedGroup || !currentUser) return;

    if (!deleteGroupPassword.trim()) {
      setErrorMessage("Por favor, digite a senha do orientador para confirmar a exclusão do grupo.");
      return;
    }

    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const res = await fetch(
        `${getApiUrl()}/api/research-groups/${encodeURIComponent(selectedGroup.group_id)}?leaderPassword=${encodeURIComponent(deleteGroupPassword.trim())}`,
        {
          method: "DELETE",
          headers: {
            "X-Requester-Email": currentUser.email,
          },
        }
      );

      if (res.ok) {
        setIsGroupModalOpen(false);
        setSuccessMessage(`Grupo de pesquisa '${selectedGroup.group_id}' excluído com sucesso.`);
        fetchUserGroups(currentUser.email);
        fetchPublicGroups();
      } else {
        const errData = await res.json().catch(() => null);
        setErrorMessage(
          errData?.message || "Senha do orientador incorreta ou você não possui permissão para excluir este grupo."
        );
      }
    } catch (err) {
      setErrorMessage("Falha na comunicação com o servidor ao excluir o grupo.");
    } finally {
      setLoading(false);
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
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium">
              Logado como: <strong>{currentUser.name}</strong> ({currentUser.email})
            </span>
            <button onClick={openProfileModal} className="btn-dark text-sm">
              Gerenciar Perfil
            </button>
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
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <section className="bg-white p-6 rounded border border-gray-300">
                <div className="flex justify-between items-center mb-3">
                  <h2 className="text-lg font-bold text-[#333333]">
                    Criar Grupo de Pesquisa
                  </h2>
                  <button
                    onClick={() => setIsCreateGroupOpen(!isCreateGroupOpen)}
                    className="btn-green text-xs"
                  >
                    {isCreateGroupOpen ? "Cancelar" : "+ Criar Grupo"}
                  </button>
                </div>
                <p className="text-sm text-gray-600 mb-4">
                  Ao criar um grupo, você se torna o <strong>Líder/Orientador</strong> automaticamente.
                </p>

                {isCreateGroupOpen && (
                  <form onSubmit={handleCreateGroup} className="space-y-3 pt-2 border-t border-gray-200">
                    <div>
                      <label className="block text-xs font-bold text-[#333333] mb-1">
                        Código ID do Grupo (Chave Única)
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="Ex: IA-002, ROB-001"
                        value={newGroupId}
                        onChange={(e) => setNewGroupId(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-[#333333] mb-1">
                        Nome do Grupo de Pesquisa
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="Ex: Grupo de Robótica e Automação"
                        value={newGroupName}
                        onChange={(e) => setNewGroupName(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-[#333333] mb-1">
                        Linhas de Pesquisa (Separadas por vírgula)
                      </label>
                      <input
                        type="text"
                        placeholder="Ex: Robótica Móvel, Sistemas Autônomos"
                        value={newGroupFields}
                        onChange={(e) => setNewGroupFields(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                      />
                    </div>

                    <button
                      type="submit"
                      disabled={loading}
                      className="btn-green text-sm w-full"
                    >
                      {loading ? "Criando Grupo..." : "Confirmar e Criar Grupo"}
                    </button>
                  </form>
                )}
              </section>

              <section className="bg-white p-6 rounded border border-gray-300 flex flex-col justify-between">
                <div>
                  <h2 className="text-lg font-bold text-[#333333] mb-2">
                    Entrar em um Grupo (Via Hash)
                  </h2>
                  <p className="text-sm text-gray-600 mb-4">
                    Digite a hash/token enviada pelo orientador para ingressar no grupo.
                  </p>
                </div>
                <form onSubmit={handleJoinGroupWithHash} className="flex gap-2">
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
                    {loading ? "Entrando..." : "Entrar"}
                  </button>
                </form>
              </section>
            </div>

            <section className="bg-white p-6 rounded border border-gray-300">
              <h2 className="text-lg font-bold text-[#333333] mb-4">
                Meus Grupos de Pesquisa (Membro)
              </h2>

              {memberGroups.length === 0 ? (
                <p className="text-sm text-gray-500 italic">
                  Você ainda não é membro de nenhum grupo de pesquisa. Use a caixa de busca ou crie um novo grupo.
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
                  Você não é líder de nenhum grupo de pesquisa no momento. Clique no botão &quot;Criar Grupo&quot; acima para criar o seu.
                </p>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {leaderGroups.map((group) => (
                    <div
                      key={group.group_id}
                      className="p-4 border border-[#3E885B] rounded bg-white flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex justify-between items-start mb-2">
                          <span className="text-xs font-bold bg-[#3E885B] text-white px-2 py-0.5 rounded">
                            {group.group_id} (Líder / Orientador)
                          </span>
                        </div>
                        <h3 className="font-bold text-[#333333] text-base mb-3">{group.name}</h3>
                      </div>

                      <div>
                        <div className="flex gap-2 mb-3">
                          <button
                            onClick={() => handleGenerateInvite(group.group_id)}
                            className="btn-dark text-xs flex-1"
                          >
                            Gerar Hash
                          </button>
                          <button
                            onClick={() => openGroupModal(group)}
                            className="btn-green text-xs flex-1"
                          >
                            Gerenciar Grupo
                          </button>
                        </div>

                        {generatedTokens[group.group_id] && (
                          <div className="p-2 bg-[#F6F4F3] border border-gray-300 rounded text-xs">
                            <span className="text-gray-600 block mb-1">Hash Gerada (Válida por 7 dias):</span>
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

      {isProfileModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex justify-center items-center p-4 z-50 animate-fade">
          <div className="bg-white max-w-md w-full rounded border border-gray-300 p-6 shadow-md max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-[#333333] mb-4 border-b border-gray-200 pb-2">
              Gerenciar Perfil do Usuário
            </h2>

            {errorMessage && (
              <div className="mb-4 p-3 bg-[#BF3100] text-white rounded text-xs font-medium">
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleUpdateProfile} className="space-y-4 mb-6">
              <div>
                <label className="block text-xs font-bold text-[#333333] mb-1">
                  Nome Completo
                </label>
                <input
                  type="text"
                  required
                  value={profileName}
                  onChange={(e) => setProfileName(e.target.value)}
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
                  value={profileEmail}
                  onChange={(e) => setProfileEmail(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#333333] mb-1">
                  Nova Senha (opcional)
                </label>
                <input
                  type="password"
                  placeholder="Deixe em branco para não alterar"
                  value={profilePassword}
                  onChange={(e) => setProfilePassword(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#333333] mb-1">
                  Áreas de Interesse (separadas por vírgula)
                </label>
                <input
                  type="text"
                  value={profileInterests}
                  onChange={(e) => setProfileInterests(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsProfileModalOpen(false)}
                  className="btn-dark text-sm"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="btn-green text-sm"
                >
                  {loading ? "Salvando..." : "Salvar Alterações"}
                </button>
              </div>
            </form>

            <div className="pt-4 border-t border-gray-200">
              <h3 className="text-sm font-bold text-[#BF3100] mb-2">Zona de Perigo</h3>
              {!isDeleteProfileConfirmOpen ? (
                <button
                  type="button"
                  onClick={() => setIsDeleteProfileConfirmOpen(true)}
                  className="btn-rust text-xs w-full"
                >
                  Excluir Conta de Usuário
                </button>
              ) : (
                <form onSubmit={handleDeleteProfile} className="space-y-3 bg-[#F6F4F3] p-3 rounded border border-[#BF3100]">
                  <p className="text-xs text-[#333333] font-medium">
                    Para confirmar a exclusão da sua conta, digite sua senha abaixo:
                  </p>
                  <input
                    type="password"
                    required
                    placeholder="Sua senha"
                    value={deleteProfilePassword}
                    onChange={(e) => setDeleteProfilePassword(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#BF3100]"
                  />
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setIsDeleteProfileConfirmOpen(false)}
                      className="btn-dark text-xs flex-1"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      disabled={loading}
                      className="btn-rust text-xs flex-1"
                    >
                      {loading ? "Excluindo..." : "Confirmar Exclusão"}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      )}

      {isGroupModalOpen && selectedGroup && (
        <div className="fixed inset-0 bg-black/50 flex justify-center items-center p-4 z-50 animate-fade">
          <div className="bg-white max-w-md w-full rounded border border-gray-300 p-6 shadow-md max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center mb-4 border-b border-gray-200 pb-2">
              <h2 className="text-xl font-bold text-[#333333]">
                Gerenciar Grupo de Pesquisa
              </h2>
              <span className="text-xs font-bold bg-[#333333] text-white px-2 py-0.5 rounded">
                {selectedGroup.group_id}
              </span>
            </div>

            {errorMessage && (
              <div className="mb-4 p-3 bg-[#BF3100] text-white rounded text-xs font-medium">
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleUpdateGroup} className="space-y-4 mb-6">
              <div>
                <label className="block text-xs font-bold text-[#333333] mb-1">
                  Nome do Grupo de Pesquisa
                </label>
                <input
                  type="text"
                  required
                  value={editGroupName}
                  onChange={(e) => setEditGroupName(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#333333] mb-1">
                  Linhas de Pesquisa (separadas por vírgula)
                </label>
                <input
                  type="text"
                  value={editGroupFields}
                  onChange={(e) => setEditGroupFields(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#3E885B]"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsGroupModalOpen(false)}
                  className="btn-dark text-sm"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="btn-green text-sm"
                >
                  {loading ? "Salvando..." : "Salvar Alterações"}
                </button>
              </div>
            </form>

            <div className="pt-4 border-t border-gray-200">
              <h3 className="text-sm font-bold text-[#BF3100] mb-2">Zona de Perigo (Apenas Líder)</h3>
              {!isDeleteGroupConfirmOpen ? (
                <button
                  type="button"
                  onClick={() => setIsDeleteGroupConfirmOpen(true)}
                  className="btn-rust text-xs w-full"
                >
                  Excluir Grupo de Pesquisa
                </button>
              ) : (
                <form onSubmit={handleDeleteGroup} className="space-y-3 bg-[#F6F4F3] p-3 rounded border border-[#BF3100]">
                  <p className="text-xs text-[#333333] font-medium">
                    Para confirmar a exclusão do grupo, digite a <strong>senha do orientador</strong>:
                  </p>
                  <input
                    type="password"
                    required
                    placeholder="Senha do orientador"
                    value={deleteGroupPassword}
                    onChange={(e) => setDeleteGroupPassword(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none focus:border-[#BF3100]"
                  />
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setIsDeleteGroupConfirmOpen(false)}
                      className="btn-dark text-xs flex-1"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      disabled={loading}
                      className="btn-rust text-xs flex-1"
                    >
                      {loading ? "Excluindo..." : "Confirmar Exclusão"}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      )}

      <footer className="bg-[#333333] text-gray-400 text-xs py-4 px-6 text-center mt-auto">
        SIGLab &copy; 2026 - Departamento de Informática e Matemática Aplicada (DIMAP / UFRN)
      </footer>
    </div>
  );
}
