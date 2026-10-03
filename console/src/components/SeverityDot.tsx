const COLORS: Record<string, string> = {
  critical: "bg-block",
  high: "bg-block/70",
  medium: "bg-review",
  low: "bg-slate-500",
  info: "bg-slate-600"
};

export default function SeverityDot({ severity }: { severity: string }) {
  return <span className={`inline-block w-2 h-2 rounded-full ${COLORS[severity] ?? "bg-slate-600"}`} />;
}
