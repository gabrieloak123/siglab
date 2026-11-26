"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import { GroupAreaHeader } from "@/components/groups/GroupAreaHeader";
import { GroupNavigation } from "@/components/groups/GroupNavigation";
import { useGroupArea } from "@/hooks/useGroupArea";
import { groupsApi, messageOf, ApiError } from "@/lib/api";
import type { GroupDashboard } from "@/types";

export default function GroupDashboardPage() {
  const { groupId } = useParams<{ groupId: string }>();
  return <GroupOverview key={groupId} groupId={groupId} />;
}

function GroupOverview({ groupId }: { groupId: string }) {
  const area = useGroupArea(groupId);

  return (
    <main className="mx-auto w-full max-w-5xl p-6">
      {area.loading ? (
        <p role="status">Carregando grupo...</p>
      ) : area.group ? (
        <>
          <GroupAreaHeader group={area.group} isLeader={area.isLeader} />
          <GroupNavigation groupId={groupId} active="dashboard" isLeader={area.isLeader} />
          <section aria-labelledby="group-dashboard-title" className="space-y-4">
            <h2 id="group-dashboard-title" className="text-xl font-bold">Indicadores do grupo</h2>
            {area.user ? (
              <DashboardMetrics groupId={groupId} />
            ) : (
              <p>
                <Link href="/" className="underline">Entre na sua conta</Link>{" "}
                para consultar os indicadores do grupo.
              </p>
            )}
          </section>
        </>
      ) : (
        <>
          <h1 className="text-xl font-bold">Grupo indisponível</h1>
          <p>{area.error || "Grupo de pesquisa não encontrado."}</p>
          <Link href="/" className="underline">Voltar à página inicial</Link>
        </>
      )}
    </main>
  );
}

function DashboardMetrics({ groupId }: { groupId: string }) {
  const [dashboard, setDashboard] = useState<GroupDashboard | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();

    groupsApi.dashboard(groupId, controller.signal).then(
      (result) => {
        if (!controller.signal.aborted) setDashboard(result);
      },
      (caught: unknown) => {
        if (controller.signal.aborted) return;
        setError(caught instanceof ApiError && caught.status === 401
          ? "Sua sessão expirou. Entre novamente para consultar os indicadores."
          : messageOf(caught, {
              api: "Não foi possível carregar os indicadores.",
              network: "Falha de conexão com o servidor.",
            }));
      },
    );

    return () => controller.abort();
  }, [groupId]);

  if (error) return <p role="alert">{error}</p>;
  if (!dashboard) return <p role="status">Carregando indicadores...</p>;
  if (!dashboard.metrics.length) return <p>Nenhum indicador disponível.</p>;

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {dashboard.metrics.map((metric) => (
        <article
          key={metric.id}
          className={`rounded border border-gray-300 bg-white p-4 ${metric.type === "breakdown" ? "sm:col-span-2" : ""}`}
        >
          <h3 className="font-semibold">{metric.label}</h3>
          {metric.type === "total" ? (
            <p className="mt-2 text-3xl font-bold tabular-nums">{metric.value}</p>
          ) : metric.items.length ? (
            <ul className="mt-3 divide-y divide-gray-200">
              {metric.items.map((item) => (
                <li key={item.id} className="flex justify-between gap-4 py-2">
                  <span>{item.label}</span>
                  <strong className="tabular-nums">{item.value}</strong>
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-sm text-gray-600">Sem dados para exibir.</p>
          )}
        </article>
      ))}
    </div>
  );
}
