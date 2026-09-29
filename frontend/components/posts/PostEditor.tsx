"use client";

import { useState, type FormEvent } from "react";

import {
  MarkdownDropzone,
  type MarkdownFile,
} from "@/components/posts/MarkdownDropzone";
import { AlertBanner } from "@/components/ui/AlertBanner";
import {
  FormField,
  inputClassName,
  textareaClassName,
} from "@/components/ui/FormField";
import { useFeedback } from "@/hooks/useFeedback";
import { parseList } from "@/lib/utils";
import type { Post } from "@/types";

interface PostEditorProps {
  post?: Post;
  submitLabel: string;
  onSubmit: (payload: {
    title: string;
    description: string | null;
    content: string;
    tags: string[];
    filePath: string | null;
  }) => Promise<boolean>;
  onCancel?: () => void;
}

export function PostEditor({
  post,
  submitLabel,
  onSubmit,
  onCancel,
}: PostEditorProps) {
  const feedback = useFeedback();
  const [title, setTitle] = useState(post?.title ?? "");
  const [description, setDescription] = useState(post?.description ?? "");
  const [tags, setTags] = useState((post?.tags ?? []).join(", "));
  const [file, setFile] = useState<MarkdownFile | null>(
    post?.content
      ? { fileName: post.filePath ?? "post.md", content: post.content }
      : null
  );

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    if (!file) {
      feedback.fail(
        "Arraste um arquivo Markdown (.md) para definir o conteúdo do post."
      );
      return;
    }

    feedback.begin();

    const saved = await onSubmit({
      title: title.trim(),
      description: description.trim() || null,
      content: file.content,
      tags: parseList(tags),
      filePath: file.fileName,
    });

    if (saved) {
      feedback.succeed(
        post
          ? "Post atualizado com sucesso!"
          : "Post publicado com sucesso no grupo de pesquisa!"
      );
    }

    feedback.setLoading(false);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <AlertBanner message={feedback.error} variant="error" size="sm" />
      <AlertBanner message={feedback.success} variant="success" size="sm" />

      <FormField label="Título do Post">
        <input
          type="text"
          required
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          className={inputClassName}
        />
      </FormField>

      <FormField label="Descrição (resumo opcional)">
        <textarea
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          className={textareaClassName}
        />
      </FormField>

      <FormField label="Tags (separadas por vírgula)">
        <input
          type="text"
          placeholder="artigo, dissertação"
          value={tags}
          onChange={(event) => setTags(event.target.value)}
          className={inputClassName}
        />
      </FormField>

      <FormField label="Conteúdo em Markdown (arquivo .md)">
        <MarkdownDropzone value={file} onChange={setFile} />
      </FormField>

      <div className="flex justify-end gap-2 pt-2">
        {onCancel && (
          <button type="button" onClick={onCancel} className="btn-dark text-sm">
            Cancelar
          </button>
        )}
        <button
          type="submit"
          disabled={feedback.loading}
          className="btn-green text-sm"
        >
          {feedback.loading ? "Salvando..." : submitLabel}
        </button>
      </div>
    </form>
  );
}
