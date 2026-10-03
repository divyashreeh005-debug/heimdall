import { Outlet, Link } from "react-router-dom";

/**
 * No business rules live here (docs/01-c4-architecture.md: "React Console
 * ... No business rules. JWT bearer to API."). This shell only renders
 * navigation and hands off to the pages, which call the console-api module.
 */
export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b border-line px-6 py-4 flex items-center justify-between">
        <Link to="/" className="font-display text-lg tracking-tight text-slate-100">
          Heimdall <span className="text-bifrost">/</span> Console
        </Link>
        <nav className="text-sm text-slate-400 font-mono">MERGE / REVIEW / BLOCK</nav>
      </header>
      <main className="flex-1 px-6 py-8 max-w-5xl w-full mx-auto">
        <Outlet />
      </main>
    </div>
  );
}
