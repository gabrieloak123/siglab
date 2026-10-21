"use client";

import { useEffect, useState } from "react";
import { ConferenceCard } from "@/components/groups/ConferenceCard";
import { messageOf, socialApi } from "@/lib/api";
import type { ConferenceSuggestion } from "@/types";

export function ConferencePanel() {
  const [conferences, setConferences] = useState<ConferenceSuggestion[]>([]);
  const [error, setError] = useState("");
  useEffect(() => {
    void (async () => {
      try {
        setConferences(await socialApi.conferences());
        setError("");
      } catch (caught) {
        setError(messageOf(caught, { api: "Não foi possível carregar as conferências sugeridas.", network: "Falha de conexão." }));
      }
    })();
  }, []);
  return (
    <section className="space-y-4 mt-8">
      <div>
        <h2 className="text-xl font-bold">Conferências sugeridas</h2>
        <p className="text-sm text-gray-600">Eventos futuros mais próximos dos seus interesses, ordenados por similaridade de conteúdo.</p>
      </div>
      {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      {!error && conferences.length === 0 && <p className="text-sm text-gray-500">Nenhuma sugestão disponível.</p>}
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
        {conferences.map(item => <ConferenceCard key={item.id} item={item} />)}
      </div>
      <p className="text-xs text-gray-500">Conferências fornecidas por <a href="https://www.myhuiban.com" target="_blank" rel="noopener noreferrer" className="underline">Conference Partner</a>.</p>
    </section>
  );
}
