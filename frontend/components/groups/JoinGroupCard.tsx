import { useState, type FormEvent } from "react";

import { inlineInputClassName } from "@/components/ui/FormField";
import type { UseResearchGroups } from "@/hooks/useResearchGroups";
import type { Feedback } from "@/hooks/useFeedback";

interface JoinGroupCardProps {
  groups: UseResearchGroups;
  feedback: Feedback;
}

export function JoinGroupCard({ groups, feedback }: JoinGroupCardProps) {
  const [inviteToken, setInviteToken] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!inviteToken.trim()) {
      feedback.fail("Digite uma hash de convite válida.");
      return;
    }

    const joined = await groups.joinGroup(inviteToken);
    if (joined) setInviteToken("");
  };

  return (
    <section className="bg-white p-6 rounded border border-gray-300 flex flex-col justify-between">
      <div>
        <h2 className="text-lg font-bold text-[#333333] mb-2">
          Entrar em um Grupo (Via Hash)
        </h2>
        <p className="text-sm text-gray-600 mb-4">
          Digite a hash/token enviada pelo orientador para ingressar no grupo.
        </p>
      </div>
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          placeholder="Ex: INV-IA-001-A1B2C3D4"
          value={inviteToken}
          onChange={(e) => setInviteToken(e.target.value)}
          className={inlineInputClassName}
        />
        <button
          type="submit"
          disabled={feedback.loading}
          className="btn-green text-sm whitespace-nowrap"
        >
          {feedback.loading ? "Entrando..." : "Entrar"}
        </button>
      </form>
    </section>
  );
}
