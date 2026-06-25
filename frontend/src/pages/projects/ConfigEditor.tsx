import { Check } from "lucide-react";
import { Banner } from "../../shared/components/Feedback";

export function ConfigEditor(props: {
  yaml: string;
  setYaml: (value: string) => void;
  validation: string;
  onValidate: () => void;
  onSave: () => void;
  saving?: boolean;
}) {
  return (
    <div className="config-editor">
      <div className="editor-toolbar">
        <button className="button secondary" onClick={props.onValidate}><Check size={16} /> Validate</button>
        <button className="button primary" onClick={props.onSave} disabled={props.saving}>{props.saving ? "Saving…" : "Save"}</button>
      </div>
      {props.validation === "valid" && <Banner tone="success">YAML validation passed.</Banner>}
      {props.validation === "invalid" && <Banner tone="danger">YAML validation failed. Check the configuration and try again.</Banner>}
      <textarea className="code-editor" value={props.yaml} onChange={(event) => props.setYaml(event.target.value)} spellCheck={false} />
    </div>
  );
}
