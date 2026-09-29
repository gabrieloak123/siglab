import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface MarkdownContentProps {
  content: string;
  className?: string;
}

export function MarkdownContent({
  content,
  className = "",
}: MarkdownContentProps) {
  return (
    <div
      className={`prose prose-sm max-w-none prose-headings:text-[#333333] prose-a:text-[#3E885B] prose-strong:text-[#333333] prose-code:text-[#BF3100] prose-pre:bg-[#333333] ${className}`.trim()}
    >
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  );
}
