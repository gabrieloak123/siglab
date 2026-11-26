import type { GroupDashboard } from "@/types";

type Items = Extract<GroupDashboard["metrics"][number], { type: "breakdown" }>["items"];

export function DashboardWordCloud({ items }: { items: Items }) {
  if (!items.length) {
    return <p className="mt-2 text-sm text-gray-600">Sem dados para exibir.</p>;
  }

  const maxCount = items.reduce((max, item) => Math.max(max, item.value), 1);

  return (
    <ul className="mt-4 flex flex-wrap items-baseline gap-x-4 gap-y-2">
      {items.map((item) => (
        <li key={item.id} className="inline-flex max-w-full items-baseline gap-1">
          <span
            className="break-all font-semibold text-green-800"
            style={{ fontSize: `${1 + item.value / maxCount}rem` }}
          >
            {item.label}
          </span>
          <span className="text-sm tabular-nums text-gray-600">{item.value}</span>
        </li>
      ))}
    </ul>
  );
}
