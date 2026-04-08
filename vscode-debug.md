# Debugging Bitwig extensions with VS Code

This document describes how to attach a Java debugger (VS Code) to Bitwig so you can set breakpoints inside
an extension and inspect runtime behaviour.

High level options
- Remote attach: start Bitwig with the JVM debug agent (JDWP) and attach VS Code's Java debugger.
- Local attach via running a Java test harness: faster and safer for unit tests.

## Remote attach (recommended for runtime inspection)

1. Start Bitwig with JDWP enabled. The agent argument is the same as the IntelliJ guide:

   ```
   -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
   ```

   On macOS you can inject this argument without editing the bundle by launching Bitwig from Terminal with `JAVA_TOOL_OPTIONS`:

   ```bash
   export JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
   open -a "Bitwig Studio.app"
   ```

   Keep the Terminal window open so the environment variable stays in effect, and unset the variable afterwards (`unset JAVA_TOOL_OPTIONS`) so other Java apps are not affected. If you prefer a permanent toggle, follow the IntelliJ tutorial's directions to modify the macOS launcher script but remember to revert the changes once you are done debugging.

2. In VS Code install the "Debugger for Java" (`vscjava.vscode-java-debug`) extension and the Java Extension Pack.

3. Create a `.vscode/launch.json` entry to attach to the remote JVM:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Attach to Bitwig JVM",
      "request": "attach",
      "hostName": "127.0.0.1",
      "port": 5005
    }
  ]
}

if developing in Wsl on windows, it may not be able to connect to 127.0.0.1. Find the ip by executing this in wsl:
`ip route | awk '/default/ {print $3}'`

```

4. Press the green "Attach to Bitwig JVM" button in VS Code once Bitwig has finished launching. Breakpoints placed inside `src/main/java` should now hit whenever the corresponding code executes.

## Live workflow

For controller-script issues, prefer this workflow:

1. Rebuild and copy the extension so Bitwig is running the same code you are looking at.
2. Verify JDWP is available before attaching:

   ```bash
   lsof -nP -iTCP:5005 | rg 'ESTABLISHED|LISTEN'
   ```

   Expected states:
   - `LISTEN` only: Bitwig is ready and no debugger is attached yet.
   - `ESTABLISHED` plus `LISTEN`: VS Code or another debugger is already attached.

3. If you need a terminal debugger instead of VS Code, stop the VS Code debug session first so JDWP is free.

### Prefer logging for controller interaction

Breakpoints in the control-surface thread can freeze or effectively crash the Bitwig UI while input is being processed. For hardware interaction bugs:

- Prefer temporary `driver.getHost().println(...)` logging over breakpoints.
- Use logging to capture:
  - requested source fine/grid start
  - requested target fine/grid start
  - note duration
  - observed callback values after the move

Bitwig controller log on macOS:

```bash
tail -f ~/Documents/Bitwig\ Studio/Controller\ Scripting\ Data/log.txt
```

This is the safest way to debug note movement and observer/caching issues without suspending the UI thread.

### When breakpoints are still useful

Use breakpoints sparingly when you need one exact transition and can avoid touching the controller repeatedly while the VM is suspended. After you are done:

- resume the VM
- stop the debugger

Otherwise Bitwig can remain visually frozen because the control-surface session thread is paused.

## Local test harness debugging

Unit tests (see `README.md`) are run with Gradle and execute on the local JVM, so VS Code can attach automatically when you hit `Run Test` next to a method. This is often the fastest way to iterate on helper classes such as `NoteInputConfigurator`.

## Caveats and tips
- Editing the Bitwig app bundle or launcher is powerful but brittle; prefer the transient `JAVA_TOOL_OPTIONS` approach when possible.
- Bitwig must be restarted after changing JDWP arguments.
- When debugging remote sessions make sure the compiled `.bwextension` matches your sources; rebuild before launching Bitwig if needed.
