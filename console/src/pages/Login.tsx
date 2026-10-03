import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, setToken } from "../api/client";

export default function Login() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const { token } = await api.devLogin(email);
      setToken(token);
      navigate("/");
    } catch {
      setError("No membership found for that email in this org.");
    }
  }

  return (
    <div className="max-w-sm mx-auto mt-16">
      <h1 className="font-display text-2xl text-slate-100 mb-1">Sign in</h1>
      <p className="text-sm text-slate-400 mb-6">Phase 1 dev login. Production auth is GitHub-App-installer driven.</p>
      <form onSubmit={handleSubmit} className="space-y-3">
        <input
          type="email"
          required
          placeholder="you@org.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="w-full bg-panel border border-line rounded px-3 py-2 text-sm outline-none focus:border-bifrost"
        />
        <button type="submit" className="w-full bg-bifrost text-ink font-medium rounded px-3 py-2 text-sm hover:opacity-90">
          Continue
        </button>
        {error && <p className="text-block text-sm">{error}</p>}
      </form>
    </div>
  );
}
