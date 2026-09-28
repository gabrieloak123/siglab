import type { ReactNode } from "react";

const BASE_INPUT =
  "px-3 py-2 border border-gray-300 rounded text-sm text-[#333333] focus:outline-none";

export const inputClassName = `${BASE_INPUT} w-full focus:border-[#3E885B]`;

export const dangerInputClassName = `${BASE_INPUT} w-full focus:border-[#BF3100]`;

export const inlineInputClassName = `${BASE_INPUT} flex-1 focus:border-[#3E885B]`;

interface FormFieldProps {
  label: string;
  children: ReactNode;
}

export function FormField({ label, children }: FormFieldProps) {
  return (
    <div>
      <label className="block text-xs font-bold text-[#333333] mb-1">
        {label}
      </label>
      {children}
    </div>
  );
}
