import type { ConferenceSuggestion } from "@/types";

/** Formata "2027-07-12" como data local, sem deslocar o fuso horário (12/07/2027). */
export function formatConferenceDate(iso?: string | null): string {
  if (!iso) return "";
  const [year, month, day] = iso.split("-").map(Number);
  if (!year || !month || !day) return "";
  return new Date(year, month - 1, day).toLocaleDateString("pt-BR");
}

export function ConferenceCard({ item }: { item: ConferenceSuggestion }) {
  return (
    <article className="bg-white p-5 rounded border border-gray-300 space-y-2">
      <div className="flex items-start justify-between gap-2">
        <p className="font-bold">{item.shortName || item.fullName}</p>
        <div className="flex gap-1">
          {item.ccfRank && <span className="text-xs font-semibold bg-gray-900 text-white rounded px-1.5 py-0.5">CCF {item.ccfRank}</span>}
          {item.cpIndexScore != null && <span className="text-xs bg-green-100 text-green-800 rounded px-1.5 py-0.5">CP-I {item.cpIndexScore.toFixed(1)}</span>}
        </div>
      </div>
      {item.shortName && item.fullName && <p className="text-sm text-gray-700">{item.fullName}</p>}
      <p className="text-xs text-gray-500">
        {formatConferenceDate(item.conferenceDate)}
        {item.location ? ` · ${item.location}` : ""}
      </p>
      {item.submissionDate && <p className="text-xs text-gray-500">Submissões até {formatConferenceDate(item.submissionDate)}</p>}
      {item.detailPage && (
        <a href={item.detailPage} target="_blank" rel="noopener noreferrer" className="inline-block text-sm font-semibold text-green-700 hover:underline">
          Ver na Conference Partner
        </a>
      )}
    </article>
  );
}
