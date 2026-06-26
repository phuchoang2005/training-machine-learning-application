import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";
import { configureStore } from "@reduxjs/toolkit";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { FileDrop } from "../shared/components/Form";
import { RegisterProjectPage } from "../pages/projects/RegisterProjectPage";
import { projectSlice } from "../store/slices/projectSlice";
import { authSlice } from "../store/slices/authSlice";
import { jobSlice } from "../store/slices/jobSlice";
import { notificationSlice, adminSlice, themeSlice } from "../store/slices/supportSlices";
import axios from "axios";

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeZipFile(name = "project.zip", sizeKB = 10): File {
  const bytes = new Uint8Array(sizeKB * 1024);
  return new File([bytes], name, { type: "application/zip" });
}

function makeStore() {
  return configureStore({
    reducer: {
      auth: authSlice.reducer,
      projects: projectSlice.reducer,
      jobs: jobSlice.reducer,
      notifications: notificationSlice.reducer,
      admin: adminSlice.reducer,
      theme: themeSlice.reducer,
    },
  });
}

function renderRegisterPage() {
  const store = makeStore();
  render(
    <Provider store={store}>
      <MemoryRouter>
        <RegisterProjectPage />
      </MemoryRouter>
    </Provider>,
  );
  return store;
}

// ── FileDrop component ────────────────────────────────────────────────────────

describe("FileDrop", () => {
  it("shows empty-state hint when no file is set", () => {
    const onChange = vi.fn();
    render(<FileDrop onChange={onChange} />);
    expect(screen.getByText(/Drop a ZIP archive here/)).toBeInTheDocument();
    expect(screen.getByText(/Only .zip files are accepted/)).toBeInTheDocument();
  });

  it("displays file name and size after a zip is dropped", () => {
    const onChange = vi.fn();
    const file = makeZipFile("iris.zip", 20);
    render(<FileDrop onChange={onChange} />);
    const zone = screen.getByText(/Drop a ZIP archive here/).closest("div")!;
    fireEvent.drop(zone, { dataTransfer: { files: [file] } });
    expect(onChange).toHaveBeenCalledWith(file);
  });

  it("does not call onChange for non-zip files dropped", () => {
    const onChange = vi.fn();
    const txtFile = new File(["content"], "data.txt", { type: "text/plain" });
    render(<FileDrop onChange={onChange} />);
    const zone = screen.getByText(/Drop a ZIP archive here/).closest("div")!;
    fireEvent.drop(zone, { dataTransfer: { files: [txtFile] } });
    expect(onChange).not.toHaveBeenCalled();
  });

  it("shows the accepted file's name and size", () => {
    const file = makeZipFile("sample-projects.zip", 50);
    render(<FileDrop file={file} onChange={vi.fn()} />);
    expect(screen.getByText("sample-projects.zip")).toBeInTheDocument();
    expect(screen.getByText(/50 KB/)).toBeInTheDocument();
  });

  it("calls onChange(undefined) when clear button is clicked", async () => {
    const onChange = vi.fn();
    const file = makeZipFile("sample-projects.zip");
    render(<FileDrop file={file} onChange={onChange} />);
    const clearBtn = screen.getByRole("button", { name: /Remove file/i });
    await userEvent.click(clearBtn);
    expect(onChange).toHaveBeenCalledWith(undefined);
  });

  it("accepts a file via the hidden file input", async () => {
    const onChange = vi.fn();
    const file = makeZipFile("upload.zip");
    render(<FileDrop onChange={onChange} />);
    const input = document.querySelector("input[type=file]") as HTMLInputElement;
    await userEvent.upload(input, file);
    expect(onChange).toHaveBeenCalledWith(file);
  });

  it("rejects a non-zip file via file input", async () => {
    const onChange = vi.fn();
    const csvFile = new File(["a,b"], "data.csv", { type: "text/csv" });
    render(<FileDrop onChange={onChange} />);
    const input = document.querySelector("input[type=file]") as HTMLInputElement;
    await userEvent.upload(input, csvFile);
    expect(onChange).not.toHaveBeenCalled();
  });

  it("sets dragging class on dragover and clears on dragleave", () => {
    const onChange = vi.fn();
    render(<FileDrop onChange={onChange} />);
    const zone = screen.getByText(/Drop a ZIP archive here/).closest("div")!;
    fireEvent.dragOver(zone, { dataTransfer: {} });
    expect(zone.className).toContain("file-drop--over");
    fireEvent.dragLeave(zone);
    expect(zone.className).not.toContain("file-drop--over");
  });
});

// ── RegisterProjectPage ───────────────────────────────────────────────────────

describe("RegisterProjectPage", () => {
  it("defaults to GITHUB mode", () => {
    renderRegisterPage();
    expect(screen.getByPlaceholderText(/https:\/\/github.com/)).toBeInTheDocument();
  });

  it("switches to ZIP mode when ZIP Upload tab is clicked", async () => {
    renderRegisterPage();
    await userEvent.click(screen.getByRole("button", { name: /ZIP Upload/i }));
    expect(screen.getByText(/Drop a ZIP archive here/)).toBeInTheDocument();
  });

  it("disables Create Project when in ZIP mode with no file selected", async () => {
    renderRegisterPage();
    await userEvent.click(screen.getByRole("button", { name: /ZIP Upload/i }));
    const nameInput = screen.getAllByRole("textbox")[0];
    await userEvent.type(nameInput, "My Project");
    expect(screen.getByRole("button", { name: /Create Project/i })).toBeDisabled();
  });

  it("disables Create Project when project name is empty in ZIP mode", async () => {
    renderRegisterPage();
    await userEvent.click(screen.getByRole("button", { name: /ZIP Upload/i }));
    const file = makeZipFile("sample-projects.zip");
    const zone = screen.getByText(/Drop a ZIP archive here/).closest("div")!;
    fireEvent.drop(zone, { dataTransfer: { files: [file] } });
    expect(screen.getByRole("button", { name: /Create Project/i })).toBeDisabled();
  });

  it("enables Create Project when name, entrypoint, and zip file are all provided", async () => {
    renderRegisterPage();
    await userEvent.click(screen.getByRole("button", { name: /ZIP Upload/i }));

    const inputs = screen.getAllByRole("textbox");
    await userEvent.type(inputs[0], "Test Project");

    const file = makeZipFile("sample-projects.zip");
    const zone = screen.getByText(/Drop a ZIP archive here/).closest("div")!;
    fireEvent.drop(zone, { dataTransfer: { files: [file] } });

    expect(screen.getByRole("button", { name: /Create Project/i })).not.toBeDisabled();
  });

  it("registers an optimistic build and marks it FAILED when the zip upload fails", async () => {
    // Submitting now navigates away and tracks the build in the store: a pending
    // build appears immediately, then flips to FAILED when the request rejects
    // (jsdom has no network, so apiClient rejects with a Network Error).
    const store = renderRegisterPage();
    await userEvent.click(screen.getByRole("button", { name: /ZIP Upload/i }));

    const inputs = screen.getAllByRole("textbox");
    await userEvent.type(inputs[0], "Failing Project");

    const file = makeZipFile("sample-projects.zip");
    const zone = screen.getByText(/Drop a ZIP archive here/).closest("div")!;
    fireEvent.drop(zone, { dataTransfer: { files: [file] } });

    await userEvent.click(screen.getByRole("button", { name: /Create Project/i }));

    // Optimistic record is added synchronously on click.
    const pending = store.getState().projects.pendingBuilds;
    expect(pending).toHaveLength(1);
    expect(pending[0].projectName).toBe("Failing Project");
    expect(pending[0].status).toBe("BUILDING");

    // Once the request rejects, the build flips to FAILED (and is not created).
    await waitFor(() => {
      expect(store.getState().projects.pendingBuilds[0].status).toBe("FAILED");
    });
    expect(store.getState().projects.items).toHaveLength(0);
  });
});

// ── Axios Content-Type regression ─────────────────────────────────────────────

describe("apiClient Content-Type for FormData", () => {
  it("does not force Content-Type: application/json when sending FormData", async () => {
    // Import lazily so the module is loaded with the current axios instance.
    const { apiClient } = await import("../shared/api/axios-client");

    let capturedContentType: string | undefined;
    vi.spyOn(axios, "request").mockImplementation(async (config) => {
      capturedContentType = (config.headers as Record<string, string>)?.["Content-Type"] ??
        (config.headers as Record<string, string>)?.["content-type"];
      return { data: { projectId: "test-id" }, status: 201, headers: {}, config: config as never, statusText: "Created" };
    });

    const form = new FormData();
    form.append("file", makeZipFile("test.zip"));

    await apiClient.post("/projects/upload-zip", form).catch(() => {});

    // For FormData the Content-Type must NOT be forced to application/json.
    // Either it is absent (browser sets it with boundary) or it starts with multipart/.
    expect(capturedContentType).not.toBe("application/json");

    vi.restoreAllMocks();
  });
});

// ── projectService.createZip FormData shape ───────────────────────────────────

describe("projectService.createZip", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("posts to /projects/upload-zip with metadata and file parts", async () => {
    const { projectService } = await import("../shared/api/services/projects");
    const { apiClient } = await import("../shared/api/axios-client");

    const postSpy = vi.spyOn(apiClient, "post").mockResolvedValue({
      data: { projectId: "new-id" },
      status: 201,
      headers: {},
      config: {} as never,
      statusText: "Created",
    });

    const file = makeZipFile("sample-projects.zip");
    await projectService.createZip(
      { projectName: "Iris", trainingEntrypoint: "python main.py" },
      file,
    );

    expect(postSpy).toHaveBeenCalledWith("/projects/upload-zip", expect.any(FormData));

    const [, form] = postSpy.mock.calls[0] as [string, FormData];
    expect(form.get("file")).toBe(file);

    const metaBlob = form.get("metadata") as Blob;
    expect(metaBlob).toBeInstanceOf(Blob);
    // Read blob content via FileReader (jsdom-compatible; Blob.text() is not available in jsdom).
    const metaText = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsText(metaBlob);
    });
    const meta = JSON.parse(metaText);
    expect(meta.projectName).toBe("Iris");
    expect(meta.trainingEntrypoint).toBe("python main.py");
  });

  it("metadata Blob is typed as application/json", async () => {
    const { projectService } = await import("../shared/api/services/projects");
    const { apiClient } = await import("../shared/api/axios-client");

    const postSpy = vi.spyOn(apiClient, "post").mockResolvedValue({
      data: { projectId: "new-id" },
      status: 201,
      headers: {},
      config: {} as never,
      statusText: "Created",
    });

    const file = makeZipFile("sample-projects.zip");
    await projectService.createZip({ projectName: "Test", trainingEntrypoint: "python train.py" }, file);

    const [, form] = postSpy.mock.calls[0] as [string, FormData];
    const metaBlob = form.get("metadata") as Blob;
    expect(metaBlob.type).toBe("application/json");
  });
});
