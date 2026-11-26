import type { GroupDashboard } from "@/types";

type Items = Extract<GroupDashboard["metrics"][number], { type: "breakdown" }>["items"];
const colors = ["#276749", "#3b65a1"];

export function PostsByTypeChart({ items }: { items: Items }) {
  const total = items.reduce((sum, item) => sum + item.value, 0);

  if (total === 0) {
    return <p className="mt-2 text-sm text-gray-600">O grupo ainda não tem posts.</p>;
  }

  const slices: string[] = [];
  let start = 0;
  for (const [index, item] of items.entries()) {
    const end = start + (item.value / total) * 100;
    slices.push(`${colors[index % colors.length]} ${start}% ${end}%`);
    start = end;
  }

  return (
    <div className="mt-4 flex flex-wrap items-center gap-6">
      <div
        aria-hidden="true"
        className="size-32 shrink-0 rounded-full"
        style={{ background: `conic-gradient(${slices.join(", ")})` }}
      />
      <ul aria-label="Contagem de posts por tipo" className="min-w-36 space-y-2">
        {items.map((item, index) => (
          <li key={item.id} className="flex items-center gap-2">
            <span
              aria-hidden="true"
              className="size-3 rounded-full"
              style={{ backgroundColor: colors[index % colors.length] }}
            />
            <span>{item.label}</span>
            <strong className="ml-auto tabular-nums">{item.value}</strong>
          </li>
        ))}
      </ul>
    </div>
  );
}
