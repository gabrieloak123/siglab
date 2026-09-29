"use client";

import { useRef, useState, type ChangeEvent, type DragEvent } from "react";

import { MarkdownContent } from "@/components/posts/MarkdownContent";

const ACCEPTED_EXTENSIONS = [".md", ".markdown"];
const MAX_SIZE_BYTES = 512 * 1024;

export interface MarkdownFile {
  fileName: string;
  content: string;
}

interface MarkdownDropzoneProps {
  value: MarkdownFile | null;
  onChange: (value: MarkdownFile | null) => void;
}

export function MarkdownDropzone({
  value,
  onChange,
}: MarkdownDropzoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState("");

  const handleFile = async (file: File | undefined) => {
    if (!file) return;

    const fileName = file.name.toLowerCase();
    if (!ACCEPTED_EXTENSIONS.some((extension) => fileName.endsWith(extension))) {
      setError("Formato inválido. Envie um arquivo Markdown (.md ou .markdown).");
      return;
    }

    if (file.size > MAX_SIZE_BYTES) {
      setError("O arquivo enviado excede o limite de 512 KB.");
      return;
    }

    setError("");
    onChange({ fileName: file.name, content: await file.text() });
  };

  const handleDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setIsDragging(false);
    void handleFile(event.dataTransfer.files[0]);
  };

  const handleSelect = (event: ChangeEvent<HTMLInputElement>) => {
    void handleFile(event.target.files?.[0]);
    event.target.value = "";
  };

  return (
    <div className="space-y-3">
      <div
        onDragOver={(event) => {
          event.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        className={`border-2 border-dashed rounded p-6 text-center transition-colors ${
          isDragging
            ? "border-[#3E885B] bg-[#3E885B]/5"
            : "border-gray-300 bg-[#F6F4F3]"
        }`}
      >
        <p className="text-sm font-bold text-[#333333] mb-1">
          Arraste o arquivo Markdown do post aqui
        </p>
        <p className="text-xs text-gray-600 mb-3">
          O conteúdo do post é o próprio arquivo .md, que será renderizado
          automaticamente. Limite de 512 KB.
        </p>

        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="btn-dark text-xs"
        >
          Selecionar arquivo .md
        </button>

        <input
          ref={inputRef}
          type="file"
          accept=".md,.markdown,text/markdown"
          onChange={handleSelect}
          className="hidden"
        />
      </div>

      {error && <p className="text-xs text-[#BF3100] font-medium">{error}</p>}

      {value && (
        <div className="border border-gray-300 rounded">
          <div className="flex justify-between items-center px-3 py-2 bg-[#F6F4F3] border-b border-gray-300">
            <span className="text-xs font-bold text-[#333333]">
              {value.fileName}
            </span>
            <button
              type="button"
              onClick={() => onChange(null)}
              className="text-xs text-[#BF3100] font-bold"
            >
              Remover arquivo
            </button>
          </div>

          <div className="p-4 max-h-96 overflow-auto">
            <MarkdownContent content={value.content} />
          </div>
        </div>
      )}
    </div>
  );
}
