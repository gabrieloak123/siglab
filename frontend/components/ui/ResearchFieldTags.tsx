interface ResearchFieldTagsProps {
  fields?: string[];
  variant?: "plain" | "muted";
  className?: string;
}

const CHIP_STYLES = {
  plain: "bg-white",
  muted: "bg-[#F6F4F3]",
};

export function ResearchFieldTags({
  fields,
  variant = "plain",
  className = "",
}: ResearchFieldTagsProps) {
  if (!fields?.length) return null;

  return (
    <div className={`flex flex-wrap gap-1 ${className}`.trim()}>
      {fields.map((field, index) => (
        <span
          key={index}
          className={`text-xs ${CHIP_STYLES[variant]} text-[#333333] border border-gray-300 px-2 py-0.5 rounded`}
        >
          {field}
        </span>
      ))}
    </div>
  );
}
