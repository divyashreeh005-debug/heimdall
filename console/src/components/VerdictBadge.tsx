const STYLES: Record<string, string> = {
  MERGE: "bg-merge/15 text-merge border-merge/40",
  REVIEW: "bg-review/15 text-review border-review/40",
  BLOCK: "bg-block/15 text-block border-block/40"
};

export default function VerdictBadge({ verdict }: { verdict: string | null }) {
  if (!verdict) return <span className="text-slate-500 font-mono text-sm">pending</span>;
  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded border text-xs font-mono tracking-wide ${STYLES[verdict] ?? ""}`}>
      {verdict}
    </span>
  );
}
