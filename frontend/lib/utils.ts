export const isSameEmail = (a?: string, b?: string) =>
  (a ?? "").toLowerCase() === (b ?? "").toLowerCase();

export const parseList = (value: string) =>
  value
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);

export const formatList = (value?: string[]) => (value ?? []).join(", ");
