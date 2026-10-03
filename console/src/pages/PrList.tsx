import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api, PrOverview } from "../api/client";
import VerdictBadge from "../components/VerdictBadge";

export default function PrList() {
  const { repoId } = useParams();
  const [prs, setPrs] = useState<PrOverview[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!repoId) return;
    api.listPrs(repoId).then(setPrs).finally(() => setLoading(false));
  }, [repoId]);

  if (loading) return <p className="text-slate-500 font-mono text-sm">Loading…</p>;

  return (
    <div>
      <h1 className="font-display text-2xl text-slate-100 mb-6">Pull requests</h1>
      <div className="border border-line rounded divide-y divide-line">
        {prs.map((pr) => (
          <Link
            key={pr.pull_request_id}
            to={`/pull-requests/${pr.pull_request_id}`}
            className="flex items-center justify-between px-4 py-3 hover:bg-panel"
          >
            <div>
              <span className="font-mono text-slate-500 mr-2">#{pr.number}</span>
              <span className="text-slate-200">{pr.title}</span>
            </div>
            <div className="flex items-center gap-4 text-sm">
              <span className="text-slate-500 font-mono">{pr.finding_count} findings</span>
              {pr.score != null && <span className="font-mono text-slate-400">{pr.score}</span>}
              <VerdictBadge verdict={pr.verdict} />
            </div>
          </Link>
        ))}
        {prs.length === 0 && <p className="px-4 py-6 text-slate-500 text-sm">No pull requests analyzed yet.</p>}
      </div>
    </div>
  );
}
