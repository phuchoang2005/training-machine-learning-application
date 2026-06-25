import { Bell, Menu, Moon, Sun } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { ConnectionBadge } from "../../shared/components/Badges";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";

export function Topbar({ onMenu }: { onMenu: () => void }) {
  const unread = useAppSelector(
    (state) =>
      state.notifications.items.filter((item) => item.status !== "READ").length,
  );
  const connection = useAppSelector((state) => state.jobs.connection);

  return (
    <header className="topbar">
      <button
        className="icon-button mobile-menu"
        aria-label="Open navigation"
        onClick={onMenu}
      >
        <Menu size={20} />
      </button>
      <Breadcrumbs />
      <div className="topbar-actions">
        <ConnectionBadge state={connection} />
        <Link
          className="icon-button badge-button"
          to="/notifications"
          aria-label={`${unread} unread notifications`}
        >
          <Bell size={18} />
          {unread > 0 && <span>{unread}</span>}
        </Link>
        <ThemeToggle />
      </div>
    </header>
  );
}

function Breadcrumbs() {
  const labels = useLocation().pathname.split("/").filter(Boolean);
  return (
    <div className="breadcrumbs">
      {(labels.length ? labels : ["projects"]).map((part, index) => (
        <span key={`${part}-${index}`}>
          {index > 0 && <span className="crumb-separator">/</span>}
          {part.split("-").join(" ")}
        </span>
      ))}
    </div>
  );
}

function ThemeToggle() {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.theme.mode);
  return (
    <button
      className="icon-button"
      aria-label="Toggle theme"
      onClick={() =>
        dispatch(actions.setTheme(mode === "dark" ? "light" : "dark"))
      }
    >
      {mode === "dark" ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}
