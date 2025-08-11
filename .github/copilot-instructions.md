# Copilot Instructions for BoxLang

## Project Overview

- **BoxLang** is a dynamic JVM language and runtime, supporting multiple deployment targets (CLI, web, lambda, etc.).
- The main entry point is `ortus.boxlang.runtime.BoxRunner` (see `build.gradle: mainClass`) for the CLI.
- The runtime is modular, allowing for extensible components, functions, and services.
- Core runtime logic is in `src/main/java/ortus/boxlang/runtime/`.
- Language parsing uses ANTLR grammars in `src/main/antlr/`.
- Built-in services (e.g., `ComponentService`, `FunctionService`) are in `runtime/services/` and auto-registered via the `IService` interface.
- Components and functions are extensible via annotations (`@BoxComponent`) and service registration.
- Modules (see `modules/`) can provide BIFs, interceptors, tags, and Java libs, and are loaded with their own classloader.

## Key Developer Workflows

- **Development:** Use IntelliJ IDEA or VSCode with the BoxLang extension for code editing and navigation.
- **Build & Test:** Use Gradle for building and testing. The project is structured with a `build.gradle` file in the root directory.
- **Run:** Use the CLI binary (`boxlang`) or run `BoxRunner` directly for scripts/classes.
- **Build:** Use Gradle (`./gradlew build`) to compile, test, and package. The project targets JDK 21.
- **Test:** JUnit 5 is used for tests in `src/test/java/`. Run with `./gradlew test`.
- **Assertions:** Uses Google Truth for assertions in tests.
- **Run:** Use the CLI binary (`boxlang`) or run `BoxRunner` directly for scripts/classes.
- **Debug:** The BoxLang VSCode extension provides debugging, code navigation, and language tooling.

## Project Conventions & Patterns

- **Services:** Implement `IService` and register in `BoxRuntime` for global services.
- **Components:** Annotate with `@BoxComponent` and place in `runtime/components/` for auto-discovery.
- **Functions (BIFs):** Register via `FunctionService` and use `BIFDescriptor`/`BoxBIF` patterns.
- **Modules:** Each module has a `ModuleConfig.bx` and `box.json` for metadata/configuration.
- **Configuration:** Runtime config is loaded via CLI flags or config files (see `BoxRunner` docs).
- **Testing Modules:** Example: `modules/bx-derby` is used for DB testing in CI.
- **Code Style:** Follow Java conventions found in the workbench/ortus-java-style.xml file.
- **New Files**: Must have the workbench header comment at the top of the file.
- **Documentation:** Use Javadoc for public APIs and inline comments for complex logic.
- **Error Handling:** Use `BoxRuntimeException` for runtime errors, and `BoxParseException` for parsing errors.

## Integration Points

- **Java Interop:** 100% Java interop; Java classes can be used directly in BoxLang code.
- **ANTLR:** Language grammar and parsing are defined in `src/main/antlr/`.
- **ServiceLoader:** Used for dynamic service/component/function discovery.
- **VSCode Extension:** See [marketplace](https://marketplace.visualstudio.com/items?itemName=ortus-solutions.vscode-boxlang) for IDE integration.

## Examples

- To add a new component: create a class in `runtime/components/`, annotate with `@BoxComponent`, and implement logic.
- To add a new service: implement `IService`, place in `runtime/services/`, and register in `BoxRuntime`.
- To add a new BIF: create a class in `runtime/bifs/`, annotate with `@BoxBIF`, and register in `FunctionService`.
- To add a module: create a folder in `modules/` with `ModuleConfig.bx` and `box.json`.

## References

- [README.md](../../README.md) for high-level project info
- [BoxLang Docs](https://boxlang.ortusbooks.com/) for language and runtime details

---
If any conventions or workflows are unclear, please ask for clarification or examples from the codebase.
