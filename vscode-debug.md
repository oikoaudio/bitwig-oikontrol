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
```

4. Press the green "Attach to Bitwig JVM" button in VS Code once Bitwig has finished launching. Breakpoints placed inside `src/main/java` should now hit whenever the corresponding code executes.

## Local test harness debugging

Unit tests (see `README.md`) are run with Gradle and execute on the local JVM, so VS Code can attach automatically when you hit `Run Test` next to a method. This is often the fastest way to iterate on helper classes such as `NoteInputConfigurator`.

## Caveats and tips
- Editing the Bitwig app bundle or launcher is powerful but brittle; prefer the transient `JAVA_TOOL_OPTIONS` approach when possible.
- Bitwig must be restarted after changing JDWP arguments.
- When debugging remote sessions make sure the compiled `.bwextension` matches your sources; rebuild before launching Bitwig if needed.
