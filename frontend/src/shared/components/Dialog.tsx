import { X } from "lucide-react";
import type { ReactNode } from "react";

export function Dialog(props: { title: string; children: ReactNode; onClose: () => void; danger?: boolean }) {
  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={props.onClose}>
      <div
        className={`dialog ${props.danger ? "danger-dialog" : ""}`}
        role="dialog"
        aria-modal="true"
        aria-label={props.title}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <div className="dialog-header">
          <h2>{props.title}</h2>
          <button className="icon-button" aria-label="Close dialog" onClick={props.onClose}><X size={18} /></button>
        </div>
        {props.children}
      </div>
    </div>
  );
}
