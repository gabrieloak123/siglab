import type { ReactNode } from "react";

interface GroupBadgeProps {
  children: ReactNode;
  tone?: "dark" | "green";
  className?: string;
}

const TONE_STYLES = {
  dark: "bg-[#333333]",
  green: "bg-[#3E885B]",
};

export function GroupBadge({
  children,
  tone = "dark",
  className = "",
}: GroupBadgeProps) {
  return (
    <span
      className={`text-xs font-bold ${TONE_STYLES[tone]} text-white px-2 py-0.5 rounded ${className}`.trim()}
    >
      {children}
    </span>
  );
}
