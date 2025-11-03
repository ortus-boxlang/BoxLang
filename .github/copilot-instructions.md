# Copilot Instructions for BoxLang

## Project Overview

- **BoxLang** is a dynamic JVM language and runtime, supporting multiple deployment targets (CLI, web, lambda, etc.).
- The main entry point is `ortus.boxlang.runtime.BoxRunner` (see `build.gradle: mainClass`) for the CLI.
- The runtime is modular, allowing for extensible components, functions, and services.
- Core runtime logic is in `src/main/java/ortus/boxlang/runtime/`.
- Language parsing uses ANTLR v4 with **multi-parser capabilities** supporting BoxLang, CFML, SQL, and DocBlock grammars.
- Built-in services (e.g., `ComponentService`, `FunctionService`) are in `runtime/services/` and auto-registered via the `IService` interface.
- Components and functions are extensible via annotations (`@BoxComponent`) and service registration.
- Modules (see `modules/`) can provide BIFs, interceptors, tags, and Java libs, and are loaded with their own classloader.

## Project Structure

### Source Organization (`src/main/`)

- **`antlr/`** - ANTLR v4 grammar definitions for multi-parser support:
  - `BoxGrammar.g4` / `BoxLexer.g4` - BoxLang syntax parser
  - `CFGrammar.g4` / `CFLexer.g4` - CFML compatibility parser
  - `SQLGrammar.g4` / `SQLLexer.g4` - SQL query parser
  - `DocGrammar.g4` / `DocLexer.g4` - Documentation/JavaDoc parser
  - Custom lexer extensions in `compiler/parser/` (e.g., `BoxLexerCustom.java`)

- **`java/ortus/boxlang/compiler/`** - Compilation and transpilation layer:
  - `parser/` - Parser implementations wrapping ANTLR-generated code
  - `ast/` - Abstract Syntax Tree node definitions
  - `asmboxpiler/` - ASM-based bytecode compilation
  - `javaboxpiler/` - Java source code generation

- **`java/ortus/boxlang/runtime/`** - Core runtime implementation:
  - `BoxRuntime.java` / `BoxRunner.java` - Runtime initialization and CLI entry point
  - `bifs/global/{category}/` - Built-In Functions organized by category (system, string, array, etc.)
  - `components/` - Core BoxLang components with `@BoxComponent` annotations
  - `services/` - Runtime services implementing `IService` (auto-registered)
  - `context/` - Execution contexts (ApplicationContext, RequestContext, etc.)
  - `scopes/` - Scope implementations (variables, local, arguments, etc.)
  - `types/` - BoxLang type system (Array, Struct, Query, etc.)
  - `loader/` - ClassLocator and dynamic class loading
  - `interop/` - Java interoperability layer (DynamicObject, proxies)
  - `async/` - Asynchronous execution and threading
  - `jdbc/` - Database connectivity
  - `modules/` - Module system and loading
  - `cli/` - Command-line interface components (REPL, console, utilities)
  - `cache/`, `config/`, `events/`, `interceptors/`, `operators/`, `validation/`, etc.

- **`java/ortus/boxlang/runtime/cli/`** - CLI and REPL infrastructure:
  - `BoxRepl.java` - Interactive Read-Eval-Print-Loop implementation
  - `MiniConsole.java` - Lightweight cross-platform terminal input handler with history, tab completion, and color support
  - `BoxInputStreamReader.java` - Specialized stream reader for minimal byte reading (UTF-8 support, character-by-character input)
  - `CLIUtil.java` - CLI argument parsing utilities (supports --flags, -shorthand, --!negation, --key=value)
  - `ColorPrint.java` - Fluent API for ANSI color printing with method chaining
  - `ISyntaxHighlighter.java` - Interface for syntax highlighting implementations
  - `TabCompletion.java` - Represents a single tab completion suggestion
  - `TabCompletionState.java` - Manages tab completion cycling state
  - `providers/` - Tab completion providers (BIF, Component, extensible)

- **`java/ortus/boxlang/debugger/`** - Debugging support and tools

- **`resources/`** - Configuration files, metadata, and resource templates

### Test Organization (`src/test/`)

- **`java/`** - JUnit 5 test cases mirroring the main source structure
- **`bx/`** - BoxLang-based test files and scripts
- **`resources/`** - Test fixtures and resources

## Code Standards & Formatting

- **Formatter:** All Java code MUST follow the Eclipse formatter configuration in `workbench/ortus-java-style.xml`
  - Import this profile into your IDE (IntelliJ IDEA, Eclipse, VS Code with Java extensions)
  - Tabs for indentation (size: 1 tab = 4 spaces for display)
  - Continuation indentation: 1 tab
  - Max line length: 150 characters
  - Space after commas, colons in specific contexts
  - Javadoc formatting enabled

- **File Headers:** All new Java files MUST include the standard BoxLang header comment found in `workbench/CodeHeader.txt`:
  ```java
  /**
   * [BoxLang]
   *
   * Copyright [2023] [Ortus Solutions, Corp]
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * ...
   */
  ```

- **Naming Conventions:**
  - Classes: PascalCase
  - Methods/Variables: camelCase
  - Constants: UPPER_SNAKE_CASE
  - Packages: lowercase dot-separated

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

- **Code Formatting:** CRITICAL - All Java code MUST be formatted using `workbench/ortus-java-style.xml`
  - This is the Eclipse formatter profile that defines Ortus coding standards
  - Configure your IDE (IntelliJ IDEA, Eclipse, VS Code) to use this formatter
  - Use tabs (not spaces) for indentation
  - Max line length: 150 characters
  - Non-negotiable for all contributions and changes

- **Services:** Implement `IService` and register in `BoxRuntime` for global services.
- **Components:** Annotate with `@BoxComponent` and place in `runtime/components/` for auto-discovery.
- **Functions (BIFs):** Register via `FunctionService` and use `BIFDescriptor`/`BoxBIF` patterns.
  - BIFs are organized under `src/main/java/ortus/boxlang/runtime/bifs/global/` by category (e.g., `system/`, `java/`, `string/`, etc.)
  - Each BIF extends `BIF` class and is annotated with `@BoxBIF`
  - Constructor declares arguments using `Argument[]` array
  - Main logic goes in `_invoke(IBoxContext context, ArgumentsScope arguments)` method
  - Use Javadoc with `@argument.*` tags to document each parameter
- **Modules:** Each module has a `ModuleConfig.bx` and `box.json` for metadata/configuration.
- **Configuration:** Runtime config is loaded via CLI flags or config files (see `BoxRunner` docs).
- **Testing Modules:** Example: `modules/bx-derby` is used for DB testing in CI.
- **New Files**: Must have the workbench header comment at the top of the file (see `workbench/CodeHeader.txt`).
- **Documentation:** Use Javadoc for public APIs and inline comments for complex logic.
- **Error Handling:** Use `BoxRuntimeException` for runtime errors, and `BoxParseException` for parsing errors.

## Integration Points

- **ANTLR Multi-Parser Architecture:** BoxLang uses ANTLR v4 with multiple grammar parsers for different syntaxes:
  - **BoxParser** - Native BoxLang syntax (primary)
  - **CFParser** - CFML/ColdFusion compatibility mode
  - **SQLParser** - SQL query parsing for query manipulation
  - **DocParser** - JavaDoc-style documentation parsing
  - All parsers share common infrastructure via `AbstractParser` and custom lexer extensions
  - Parser selection is automatic based on file extension and source type detection
  - Generated parsers are in `build/generated-src/antlr/` and extended in `compiler/parser/`

- **Java Interop:** 100% Java interop; Java classes can be used directly in BoxLang code.
  - Use `createObject("java", "fully.qualified.ClassName")` to load Java classes
  - Use `createDynamicProxy()` to create Java interface proxies from BoxLang classes
  - Custom ClassLoaders can be provided to both `createObject()` and `createDynamicProxy()` for advanced class loading scenarios
  - The `classLoader` argument defaults to the request class loader if not specified

- **ServiceLoader:** Used for dynamic service/component/function discovery via Java's ServiceLoader mechanism.

- **VSCode Extension:** See [marketplace](https://marketplace.visualstudio.com/items?itemName=ortus-solutions.vscode-boxlang) for IDE integration.

## CLI and REPL Architecture

BoxLang includes a sophisticated command-line interface (CLI) and Read-Eval-Print-Loop (REPL) implementation for interactive development.

### Core CLI Components

- **`BoxRepl`** - Main REPL implementation providing interactive code execution
  - Multi-line input support with automatic brace balancing
  - Magic commands (`:history`, `:clear`, `:dark`, `:light`)
  - History shortcuts (`!!` for last command, `!n` for command n)
  - Syntax highlighting integration
  - Color theme support (dark/light palettes)

- **`MiniConsole`** - Zero-dependency terminal input handler
  - Cross-platform support (Windows, macOS, Linux)
  - Arrow key navigation through command history
  - Tab completion with provider-based extensibility
  - SHIFT+TAB for backward cycling through completions
  - Control sequences (Ctrl+C, Ctrl+D, Ctrl+L for clear)
  - 256-color ANSI support via `CODES` enum
  - UTF-8 and multi-byte character support
  - Automatic history management (no duplicates, configurable max size)

- **`BoxInputStreamReader`** - Specialized input stream reader
  - Minimal byte reading (only reads what's needed for character decoding)
  - Proper UTF-8 and charset support
  - Non-blocking ready checks
  - Essential for character-by-character terminal input without read-ahead buffering

- **`CLIUtil`** - Command-line argument parsing
  - Supports `--long-option` and `-short` flags
  - Value assignment with `--key=value` or `-k=value`
  - Boolean negation with `--!flag` or `--no-flag`
  - Quoted value support (`--path="/some/path"`)
  - Returns structured data: `{ options: {}, positionals: [] }`

- **`ColorPrint`** - Fluent API for colored output
  - Method chaining: `ColorPrint.red().bold().println("Error!")`
  - Named colors (red, green, blue, yellow, magenta, cyan, white, black)
  - Bright color variants (brightRed, brightGreen, etc.)
  - ANSI 256-color support by code number
  - Styles: bold, italic, underline, dim, strikethrough
  - Background colors
  - Convenient static methods: `printError()`, `printSuccess()`, `printWarning()`, `printInfo()`, `printDebug()`

### Tab Completion System

Extensible tab completion via provider pattern:

- **`ITabProvider`** - Interface for completion providers
  - `canProvideCompletions(input, cursorPosition)` - determines if provider handles this context
  - `getCompletions(input, cursorPosition)` - returns list of `TabCompletion` objects
  - `getPriority()` - for provider ordering (higher = higher priority)

- **`AbstractTabProvider`** - Base class with common utilities
  - Word boundary detection
  - Character classification for completion contexts
  - Completion sorting and formatting

- **Built-in Providers:**
  - `BifTabProvider` - Completes Built-In Function names
  - `ComponentTabProvider` - Completes `bx:` component names

- **`TabCompletion`** - Represents a single suggestion
  - `text` - the text to insert
  - `displayText` - what to show in menu (can include ANSI colors)
  - `description` - optional documentation string
  - `replaceStart`/`replaceEnd` - precise replacement boundaries

- **`TabCompletionState`** - Manages completion cycling
  - Tracks original input, current selection index
  - Handles forward (TAB) and backward (SHIFT+TAB) cycling
  - Preview of selected completion in input line

### REPL Features

- **Multi-line Input:** Automatic detection of unbalanced braces
  - Prompt changes to `...` for continuation lines
  - Executes when braces are balanced

- **Command History:**
  - UP/DOWN arrows navigate history
  - `!!` repeats last command
  - `!n` repeats command number n
  - `:history` shows all command history

- **Magic Commands:**
  - `:clear` - clears console screen
  - `:dark` - switches to dark color theme
  - `:light` - switches to light color theme
  - `:history` - displays command history

- **Input Controls:**
  - ENTER - executes line (or accepts selected completion)
  - BACKSPACE - deletes character
  - TAB - cycles forward through completions
  - SHIFT+TAB - cycles backward through completions
  - Ctrl+C - exits REPL
  - Ctrl+D - clears current line (or exits on empty line)
  - Ctrl+L - clears screen

### Terminal Compatibility

- **Cross-Platform Input Handling:**
  - Uses `BoxInputStreamReader` for all platforms
  - Handles CSI escape sequences (ESC[...): arrow keys, shift+tab
  - Handles SS3 escape sequences (ESC O...): function keys
  - Proper UTF-8 decoding for international characters

- **ANSI Color Support:**
  - Full 256-color palette via `MiniConsole.CODES` enum
  - Graceful degradation on terminals without color support
  - Helper methods for common formatting patterns

### Development Guidelines

- **Extending Tab Completion:**
  1. Implement `ITabProvider` or extend `AbstractTabProvider`
  2. Register with `console.registerTabProvider(yourProvider)`
  3. Providers checked in priority order
  4. Return empty list if no completions available

- **Adding REPL Magic Commands:**
  1. Check for command prefix in `BoxRepl.start()` method
  2. Add handling before the execution block
  3. Use `continue` to skip normal execution
  4. Update help text/banner as needed

- **Custom Syntax Highlighting:**
  1. Implement `ISyntaxHighlighter` interface
  2. Apply ANSI codes in `highlight()` method
  3. Pass to `MiniConsole` constructor
  4. Called automatically during input redraw

## Examples

- To add a new component: create a class in `runtime/components/`, annotate with `@BoxComponent`, and implement logic.
- To add a new service: implement `IService`, place in `runtime/services/`, and register in `BoxRuntime`.
- To add a new BIF: create a class in `runtime/bifs/global/{category}/`, annotate with `@BoxBIF`, extend `BIF`, and implement `_invoke()`.
  - Example: `CreateObject` BIF demonstrates optional arguments, ClassLoader support, and static helper methods
  - Example: `CreateDynamicProxy` BIF shows how to accept and use custom ClassLoaders for Java interop
- To add a module: create a folder in `modules/` with `ModuleConfig.bx` and `box.json`.
- To add arguments to existing BIFs:
  - Add to `declaredArguments` array in constructor
  - Extract the argument in `_invoke()` using `arguments.get()`, `arguments.getAsString()`, etc.
  - Use `arguments.getAsAttempt(Key, Class)` for optional type-safe conversions with defaults via `.orElse()`
  - Document with `@argument.{name}` Javadoc tags
- For Java interop BIFs:
  - Import `RequestBoxContext` and use `context.getParentOfType(RequestBoxContext.class).getRequestClassLoader()` for default ClassLoader
  - Use `DynamicObject.of(Class<?>, IBoxContext)` to wrap Java classes for BoxLang use
  - Use `ClassLocator.load()` for standard class loading or `loadFromClassPaths()` for custom paths

## References

- [README.md](../../README.md) for high-level project info
- [BoxLang Docs](https://boxlang.ortusbooks.com/) for language and runtime details

---
If any conventions or workflows are unclear, please ask for clarification or examples from the codebase.
