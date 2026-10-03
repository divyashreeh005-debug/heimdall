import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api, PrDetailResponse } from "../api/client";
import VerdictBadge from "../components/VerdictBadge";
import SeverityDot from "../components/SeverityDot";

export default function PrDetail() {
  const { prId } = useParams();
  const [data, setData] = useState<PrDetailResponse | null>(null);

  useEffect(() => {
    if (!prId) return;
    api.prDetail(prId).then(setData);
  }, [prId]);

  if (!data) return <p className="text-slate-500 font-mono text-sm">Loading…</p>;
  const { overview, findings, audit } = data;

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-2xl text-slate-100">
            #{overview.number} <span className="text-slate-500">{overview.title}</span>
          </h1>
          <p className="text-sm text-slate-500 font-mono mt-1">{overview.head_sha.slice(0, 12)}</p>
        </div>
        <div className="text-right">
          <VerdictBadge verdict={overview.verdict} />
          {overview.score != null && (
            <p className="text-sm text-slate-400 font-mono mt-1">
              {overview.score} / 100 · {overview.band}
            </p>
          )}
        </div>
      </div>

      <section>
        <h2 className="font-display text-lg text-slate-200 mb-3">Findings</h2>
        <div className="border border-line rounded divide-y divide-line">
          {findings.map((f) => (
            <div key={f.id} className="px-4 py-3">
              <div className="flex items-center gap-2">
                <SeverityDot severity={f.severity} />
                <span className="text-slate-200">{f.title}</span>
                <span className="ml-auto text-xs font-mono text-slate-500">{f.claim_class}</span>
                <span
                  className={`text-xs font-mono px-2 py-0.5 rounded border ${
                    f.verification_status === "VERIFIED"
                      ? "border-merge/40 text-merge"
                      : f.verification_status === "REJECTED" || f.verification_status === "FALSE_POSITIVE"
                        ? "border-slate-600 text-slate-500"
                        : "border-review/40 text-review"
                  }`}
                >
                  {f.verification_status ?? "PENDING"}
                </span>
              </div>
              <p className="text-sm text-slate-400 mt-1">{f.claim}</p>
              {f.verification_reason && <p className="text-xs text-slate-500 mt-1">{f.verification_reason}</p>}
            </div>
          ))}
          {findings.length === 0 && <p className="px-4 py-6 text-slate-500 text-sm">No findings on this run.</p>}
        </div>
      </section>

      <section>
        <h2 className="font-display text-lg text-slate-200 mb-3">Audit trail</h2>
        <ul className="font-mono text-xs text-slate-500 space-y-1">
          {audit.map((a) => (
            <li key={a.id}>
              {new Date(a.created_at).toLocaleTimeString()} — {a.actor_type} — {a.event_type}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
