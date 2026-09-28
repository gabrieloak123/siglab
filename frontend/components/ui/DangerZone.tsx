import { useState, type FormEvent, type ReactNode } from "react";

import { dangerInputClassName } from "@/components/ui/FormField";
import type { Feedback } from "@/hooks/useFeedback";

interface DangerZoneProps {
  title: string;
  description: ReactNode;
  triggerLabel: string;
  confirmLabel: string;
  passwordPlaceholder: string;
  emptyPasswordMessage: string;
  onConfirm: (password: string) => void;
  feedback: Feedback;
}

export function DangerZone({
  title,
  description,
  triggerLabel,
  confirmLabel,
  passwordPlaceholder,
  emptyPasswordMessage,
  onConfirm,
  feedback,
}: DangerZoneProps) {
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const [password, setPassword] = useState("");

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (!password.trim()) {
      feedback.fail(emptyPasswordMessage);
      return;
    }

    onConfirm(password.trim());
  };

  const handleCancel = () => {
    setIsConfirmOpen(false);
    setPassword("");
  };

  return (
    <div className="pt-4 border-t border-gray-200">
      <h3 className="text-sm font-bold text-[#BF3100] mb-2">{title}</h3>

      {!isConfirmOpen ? (
        <button
          type="button"
          onClick={() => setIsConfirmOpen(true)}
          className="btn-rust text-xs w-full"
        >
          {triggerLabel}
        </button>
      ) : (
        <form
          onSubmit={handleSubmit}
          className="space-y-3 bg-[#F6F4F3] p-3 rounded border border-[#BF3100]"
        >
          <p className="text-xs text-[#333333] font-medium">{description}</p>
          <input
            type="password"
            required
            placeholder={passwordPlaceholder}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className={dangerInputClassName}
          />
          <div className="flex gap-2">
            <button
              type="button"
              onClick={handleCancel}
              className="btn-dark text-xs flex-1"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={feedback.loading}
              className="btn-rust text-xs flex-1"
            >
              {feedback.loading ? "Excluindo..." : confirmLabel}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
