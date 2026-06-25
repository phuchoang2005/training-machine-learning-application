import type { ReactNode } from "react";

export function Page({ children, width = "ops" }: { children: ReactNode; width?: "ops" | "form" }) {
  return (
    <div className={`page ${width === "form" ? "form-content" : "ops-content"}`}>
      {children}
    </div>
  );
}

export function PageHeader({ title, subtitle, action }: { title: string; subtitle: string; action?: ReactNode }) {
  return (
    <header className="page-header">
      <div><h1>{title}</h1><p>{subtitle}</p></div>
      {action && <div className="page-action">{action}</div>}
    </header>
  );
}
