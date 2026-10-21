"use client";

import { useEffect, useState, type FormEvent } from "react";
import { ConferenceCard } from "@/components/groups/ConferenceCard";
import { messageOf, socialApi } from "@/lib/api";
import type { ConferenceSuggestion } from "@/types";

/** Os 21 slugs de `field` da Conference Partner API. */
const FIELDS: { value: string; label: string }[] = [
  { value: "ai", label: "Inteligência Artificial" },
  { value: "vision", label: "Visão Computacional" },
  { value: "nlp", label: "Processamento de Linguagem Natural" },
  { value: "data", label: "Ciência de Dados" },
  { value: "network", label: "Redes de Computadores" },
  { value: "security", label: "Segurança" },
  { value: "se", label: "Engenharia de Software" },
  { value: "systems", label: "Sistemas" },
  { value: "theory", label: "Teoria da Computação" },
  { value: "hci", label: "Interação Humano-Computador" },
  { value: "graphics", label: "Computação Gráfica" },
  { value: "bio", label: "Bioinformática" },
  { value: "robotics", label: "Robótica" },
  { value: "web", label: "Web" },
  { value: "ee", label: "Engenharia Elétrica" },
  { value: "energy", label: "Energia" },
  { value: "materials", label: "Materiais" },
  { value: "civil", label: "Engenharia Civil" },
  { value: "education", label: "Educação" },
  { value: "management", label: "Gestão" },
  { value: "math", label: "Matemática" },
];

/** Data local no formato AAAA-MM-DD, sem deslocar o fuso horário. */
function isoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
}

export function ConferenceSearch() {
  const [start, setStart] = useState(() => isoDate(new Date()));
  const [end, setEnd] = useState(() => {
    const now = new Date();
    return isoDate(new Date(now.getFullYear() + 1, now.getMonth(), now.getDate()));
  });
  const [field, setField] = useState("");
  const [country, setCountry] = useState("");
  const [countries, setCountries] = useState<string[]>([]);
  const [results, setResults] = useState<ConferenceSuggestion[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  useEffect(() => {
    void (async () => {
      try {
        setCountries(await socialApi.conferenceCountries());
      } catch {
        // Lista de países é opcional: sem ela o filtro continua funcionando com "Todos".
      }
    })();
  }, []);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!start || !end) {
      setError("Informe as datas de início e fim.");
      return;
    }
    if (end < start) {
      setError("A data final deve ser igual ou posterior à data inicial.");
      return;
    }
    if (!field) {
      setError("Escolha uma área de pesquisa.");
      return;
    }
    setLoading(true);
    setError("");
    setResults([]);
    setSearched(false);
    try {
      setResults(await socialApi.searchConferences({ start, end, field, country: country || undefined }));
      setSearched(true);
    } catch (caught) {
      setError(messageOf(caught, { api: "Não foi possível buscar conferências.", network: "Falha de conexão." }));
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="space-y-4 mt-8">
      <div>
        <h2 className="text-xl font-bold">Buscar conferências</h2>
        <p className="text-sm text-gray-600">Filtre por período e área de pesquisa; o país é opcional.</p>
      </div>
      <form onSubmit={event => void submit(event)} className="bg-white p-5 rounded border border-gray-300 space-y-3">
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-3">
          <label className="text-sm">
            Data inicial *
            <input type="date" required value={start} onChange={event => setStart(event.target.value)}
              className="mt-1 w-full border rounded px-3 py-2 text-sm" />
          </label>
          <label className="text-sm">
            Data final *
            <input type="date" required min={start || undefined} value={end} onChange={event => setEnd(event.target.value)}
              className="mt-1 w-full border rounded px-3 py-2 text-sm" />
          </label>
          <label className="text-sm">
            Área de pesquisa *
            <select required value={field} onChange={event => setField(event.target.value)}
              className="mt-1 w-full border rounded px-3 py-2 text-sm">
              <option value="">Selecione</option>
              {FIELDS.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
          </label>
          <label className="text-sm">
            País (opcional)
            <select value={country} onChange={event => setCountry(event.target.value)}
              className="mt-1 w-full border rounded px-3 py-2 text-sm">
              <option value="">Todos os países</option>
              {countries.map(name => <option key={name} value={name}>{name}</option>)}
            </select>
          </label>
        </div>
        <div className="flex items-center gap-3">
          <button type="submit" disabled={loading} className="btn-dark text-sm">{loading ? "Buscando..." : "Buscar"}</button>
          {searched && !loading && <span className="text-xs text-gray-500">{results.length} conferência(s) encontrada(s)</span>}
        </div>
        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
      </form>
      {results.length > 0 && (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
          {results.map(item => <ConferenceCard key={item.id} item={item} />)}
        </div>
      )}
      {searched && !loading && results.length === 0 && (
        <p className="text-sm text-gray-500">Nenhuma conferência encontrada para esses filtros.</p>
      )}
      <p className="text-xs text-gray-500">Conferências fornecidas por <a href="https://www.myhuiban.com" target="_blank" rel="noopener noreferrer" className="underline">Conference Partner</a>.</p>
    </section>
  );
}
