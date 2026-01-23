# BoxLang Attach Plan (REPL, Debugger, Admin)

## Goals
- Allow a BoxLang process (Instance B) to attach to a running BoxLang process (Instance A) and evaluate arbitrary BoxLang code in the target runtime.
- Reuse the same attach channel for debugger integration (bx-debugger) and admin introspection (via eval, no special command list required).
- Provide secure, opt-in access that defaults to localhost but can be enabled for Docker/remote use.

## Background (bx-debugger)
- The debugger module provides a DAP server (`BoxDebugger`) that attaches to a running JVM via JDWP (`BareJDWPConnection`).
- Debug evaluation in `VMController` already calls `BoxRuntime.executeStatement(String, IBoxContext)` inside the debuggee via JDI.
- It supports path mapping for remote debugging (`PathMappingService`).

## Architecture Overview
### A. Attach Mechanism (No Agent Jar)
- The debugger/eval hook code ships inside BoxLang itself so no external agent JAR is required.
- A dedicated, always-available debug helper thread exists in the runtime and exposes JDI-friendly entry points for evaluation and control.
- The attach client connects via JDI (like a debugger) and invokes BoxLang runtime methods directly on the target JVM.
- This removes the need for a custom JSON/TCP server and aligns REPL/admin with debugger attach semantics.

### B. Attach Client (REPL)
- Refactor `BoxRepl` to accept an `IReplExecutor` abstraction.
  - `LocalReplExecutor`: current in-process behavior.
  - `JdiReplExecutor`: attaches to the target JVM via JDI and invokes `BoxRuntime.executeStatement(...)` inside the running process.
- `MiniConsole` stays unchanged; only execution is swapped.

### C. Debugger Integration
- Use bx-debugger as the debugger front-end, attaching to a running BoxLang JVM via JDWP.
- JDWP must be enabled at JVM startup using `-agentlib:jdwp=...`; BoxLang cannot toggle JDWP on after the JVM is running.
- Any CLI flags around debug/JDWP are only for launch-time pass-through or validation of `-agentlib` arguments; they do not enable JDWP at runtime.
- Embed the debug helper/agent code inside BoxLang so there is no external agent JAR to load.
- Reuse existing path mapping options from bx-debugger to support Docker/remote file system layouts.

### D. Admin Introspection via Eval
- No special admin command list is required.
- Admin tools run arbitrary BoxLang code via the attach channel to access services, config, caches, and runtime objects.
- Example eval targets:
  - `boxRuntime.getConfiguration()`
  - `boxRuntime.getCacheService().getCache("default").getStats()`
  - `boxRuntime.getSchedulerService().getAllSchedulers()`

## Security Model
### Defaults
- Bind to `127.0.0.1` only.
- Auth token is optional for localhost binds.
- Reject all non-local connections unless explicitly enabled.

### Remote/Docker Enablement
- Add a `--repl-bind` option (e.g., `0.0.0.0`) and a config flag to allow non-local bind.
- Add a `--repl-allowed-subnets` option to restrict external access (CIDR allow list).
- Support TLS (self-signed or provided certs) and/or SSH tunneling as recommended deployment modes.
- Require `--repl-token` (or token file) and enforce token checks for any non-local bind.

### Audit and Safety
- Log each attach session and eval call with client IP, session id, and execution time.
- Optional rate limiting or concurrency limits per token.
- Mutation is explicitly allowed; attach sessions are full-runtime execution contexts.

## Protocol (Attach Channel)
- No custom JSON protocol required for eval.
- The attach client uses JDI to call `BoxRuntime.executeStatement(String, IBoxContext)` (or a dedicated helper method) inside the target JVM.
- The response is read back from the returned `Value` and the context buffer (via JDI invocation).
- This mirrors how bx-debugger evaluates expressions (see `VMController.evaluateExpressionInFrame`).

## CLI and Config Flags
- `--repl-attach host:port` (client mode, uses JDI/JDWP)
- `--repl-context <app|runtime>` (optional, selects evaluation context)

Debugger/JDWP flags (launch-time only):
- `--debug-jdwp` (bool, only for launching a JVM with `-agentlib`)
- `--debug-port` (default 5005)
- `--debug-suspend` (optional)
- `--debug-args` (optional pass-through for `-agentlib:jdwp=...` when launching)

## Phased Implementation Plan
### Phase 1: In-Process Debug Helper + Eval
- Add an always-available debug helper inside BoxLang runtime (no external agent JAR).
- Expose a JDI-friendly API to:
  - create/get a persistent `ScriptingRequestBoxContext`
  - call `executeStatement` and return results + buffer output
  - optionally target application contexts
- Wire helper methods so they can be invoked by JDI (mirrors `InvokeTools.submitAndInvoke`).

### Phase 2: JDI REPL Client
- Refactor `BoxRepl` to use `IReplExecutor` and implement `JdiReplExecutor`.
- Add CLI entry: `boxlang --repl-attach host:port`.
- Preserve REPL history and UX in Instance B.

### Phase 3: Security Hardening
- Use JDWP bind options to control exposure (localhost default).
- Add optional auth for localhost and required auth for non-local binds (validated via JDI call).
- Add rate limiting, max sessions, and per-session timeouts enforced by the helper.
- Emit structured audit logs.

### Phase 4: Debugger Attach (bx-debugger)
- Require JDWP to be enabled at JVM startup using `-agentlib:jdwp=...`.
- Provide a helper script/config to launch `BoxDebugger` and attach via DAP to the running BoxLang server.
- Document how to set `localRoot`/`remoteRoot` for Docker path mapping.
- Ensure helper code is already in the runtime (no external agent JAR).

### Phase 5: Admin Tooling via Eval
- Provide sample admin scripts and reference eval snippets for common introspection tasks.
- Optional: ship a basic admin CLI that is just a thin wrapper over attach REPL (future enhancement).

### Phase 6: Tests + Docs
- Unit tests for attach helper methods and token validation.
- Integration tests for REPL attach to a running runtime.
- Documentation with Docker examples and SSH tunneling guidance.

## Docker/Remote Usage Examples
- **Local Docker (port exposed):**
  - Start JVM with `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
  - `boxlang --repl-attach 127.0.0.1:5005`
- **Remote (recommended):**
  - SSH tunnel: `ssh -L 5005:127.0.0.1:5005 user@host`
  - Client attaches to `localhost:5005`.

## Open Decisions
- DAP server stays in bx-debugger (separate process).
