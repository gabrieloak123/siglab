"use client";

import { useParams, useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";

import { GroupAreaHeader } from "@/components/groups/GroupAreaHeader";
import { AlertBanner } from "@/components/ui/AlertBanner";
import { DangerZone } from "@/components/ui/DangerZone";
import { FormField, inputClassName } from "@/components/ui/FormField";
import { useFeedback } from "@/hooks/useFeedback";
import { useGroupArea } from "@/hooks/useGroupArea";
import { groupsApi, messageOf } from "@/lib/api";
import { formatList, parseList } from "@/lib/utils";
import type { ResearchGroup } from "@/types";

interface GroupSettingsFormProps {
  group: ResearchGroup;
  isLeader: boolean;
  email: string;
}

function GroupSettingsForm({
  group,
  isLeader,
  email,
}: GroupSettingsFormProps) {
  const router = useRouter();
  const feedback = useFeedback();
  const [name, setName] = useState(group.name ?? "");
  const [fields, setFields] = useState(() => formatList(group.research_fields));

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (parseList(fields).length !== 3) {
      feedback.fail("Informe exatamente três linhas de pesquisa.");
      return;
    }
    feedback.begin();

    try {
      await groupsApi.update(group.group_id, {
        name: name.trim(),
        research_fields: parseList(fields),
      });
      feedback.succeed("Grupo de pesquisa atualizado com sucesso!");
    } catch (caught) {
      feedback.fail(
        messageOf(caught, {
          api: "Erro ao atualizar o grupo de pesquisa.",
          network: "Falha na comunicação ao tentar salvar as alterações.",
        })
      );
    } finally {
      feedback.setLoading(false);
    }
  };

  const handleDelete = async (password: string) => {
    feedback.begin();

    try {
      await groupsApi.remove(group.group_id, password.trim(), email);
      feedback.succeed("Grupo de pesquisa excluído com sucesso.");
      router.push("/");
      return true;
    } catch (caught) {
      feedback.fail(
        messageOf(caught, {
          api: "Senha do orientador incorreta ou você não possui permissão para excluir este grupo.",
          network: "Falha na comunicação ao tentar excluir o grupo.",
        })
      );
      return false;
    } finally {
      feedback.setLoading(false);
    }
  };

  return (
    <>
      <AlertBanner message={feedback.error} variant="error" />
      <AlertBanner message={feedback.success} variant="success" />

      <div className="bg-white p-6 rounded border border-gray-300 mb-6">
        <h2 className="text-lg font-bold text-[#333333] mb-4">
          Gerenciar Grupo de Pesquisa
        </h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          <FormField label="Nome do Grupo de Pesquisa">
            <input
              type="text"
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
              className={inputClassName}
            />
          </FormField>

          <FormField label="Linhas de Pesquisa (separadas por vírgula)">
            <input
              type="text"
              value={fields}
              onChange={(event) => setFields(event.target.value)}
              className={inputClassName}
            />
          </FormField>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={feedback.loading}
              className="btn-green text-sm"
            >
              {feedback.loading ? "Salvando..." : "Salvar Alterações"}
            </button>
          </div>
        </form>
      </div>

      <div className="bg-white p-6 rounded border border-gray-300">
        <h2 className="text-lg font-bold text-[#333333] mb-2">Posts do Grupo</h2>
        <p className="text-sm text-gray-600 mb-4">
          {isLeader
            ? "Como líder, você pode publicar posts em Markdown para o grupo."
            : "Qualquer usuário cadastrado pode ler os posts do grupo. Apenas os líderes podem publicá-los."}
        </p>
        <button
          type="button"
          onClick={() =>
            router.push(`/groups/${encodeURIComponent(group.group_id)}/posts`)
          }
          className="btn-green text-sm"
        >
          Ir para os Posts
        </button>
      </div>

      {isLeader && (
        <div className="bg-white p-6 rounded border border-gray-300 mt-6">
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
            onConfirm={handleDelete}
            feedback={feedback}
          />
        </div>
      )}
    </>
  );
}

export default function GroupSettingsPage() {
  const params = useParams<{ groupId: string }>();
  const groupId = decodeURIComponent(params.groupId);
  const router = useRouter();
  const area = useGroupArea(groupId);

  if (area.loading) {
    return (
      <main className="flex-1 max-w-3xl w-full mx-auto p-6">
        <p className="text-sm text-gray-600">Carregando dados do grupo...</p>
      </main>
    );
  }

  if (!area.user) {
    return (
      <main className="flex-1 max-w-3xl w-full mx-auto p-6">
        <h1 className="text-xl font-bold text-[#333333] mb-2">
          Entre para acessar o grupo
        </h1>
        <p className="text-sm text-gray-600 mb-4">
          Sua sessão não foi encontrada. Faça login para gerenciar este grupo de
          pesquisa.
        </p>
        <button onClick={() => router.push("/")} className="btn-dark text-sm">
          Voltar para a página inicial
        </button>
      </main>
    );
  }

  if (!area.group) {
    return (
      <main className="flex-1 max-w-3xl w-full mx-auto p-6">
        <h1 className="text-xl font-bold text-[#333333] mb-2">
          Grupo indisponível
        </h1>
        <p className="text-sm text-gray-600 mb-4">
          {area.error || "Grupo de pesquisa não encontrado."}
        </p>
        <button onClick={() => router.push("/")} className="btn-dark text-sm">
          Voltar para a página inicial
        </button>
      </main>
    );
  }

  return (
    <main className="flex-1 max-w-3xl w-full mx-auto p-6">
      <GroupAreaHeader group={area.group} isLeader={area.isLeader} />

      <GroupSettingsForm
        key={area.group.group_id}
        group={area.group}
        isLeader={area.isLeader}
        email={area.user.email}
      />
    </main>
  );
}
