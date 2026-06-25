import { Bell, FolderKanban, Gauge, Shield, Users } from "lucide-react";
import { Link, useLocation } from "react-router-dom";

export function NavLinks({ role }: { role: "USER" | "ADMIN" }) {
  const location = useLocation();
  const items = [
    { to: "/projects", label: "Projects", icon: FolderKanban },
    { to: "/notifications", label: "Notifications", icon: Bell },
    ...(role === "ADMIN" ? adminItems : []),
  ];

  return (
    <nav className="nav-links" aria-label="Primary navigation">
      {items.map((item) => {
        const Icon = item.icon;
        return (
          <Link
            key={item.to}
            className={
              location.pathname.startsWith(item.to)
                ? "nav-link active"
                : "nav-link"
            }
            to={item.to}
          >
            <Icon size={18} />
            <span>{item.label}</span>
          </Link>
        );
      })}
    </nav>
  );
}

const adminItems = [
  { to: "/admin/queue", label: "Admin Queue", icon: Gauge },
  { to: "/admin/users", label: "Admin Users", icon: Users },
  { to: "/admin/audit", label: "Audit", icon: Shield },
];
