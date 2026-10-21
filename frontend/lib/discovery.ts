export interface DiscoveryDraft { postIds: number[]; areas: string[]; ownerId?: number }
const KEY = "siglab-interest-discovery";

export function readDiscovery(): DiscoveryDraft | null {
  try {
    const value = JSON.parse(sessionStorage.getItem(KEY) ?? "null") as DiscoveryDraft | null;
    if (!value || !Array.isArray(value.postIds) || !Array.isArray(value.areas)) return null;
    return value;
  } catch { return null; }
}
export function writeDiscovery(value: DiscoveryDraft) { sessionStorage.setItem(KEY, JSON.stringify(value)); }
export function clearDiscovery() { sessionStorage.removeItem(KEY); }
