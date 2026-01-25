# PrettyPrint CLI Test Files

This directory contains test files for manually testing the PrettyPrint CLI options.

## Test Files

- **input/test1.bxs** - Simple BoxLang script with function
- **input/test2.cfm** - CFML file with function
- **input/test3.bx** - BoxLang class file
- **custom-config.json** - Custom configuration (indentSize: 2, maxLineLength: 120)

All input files are intentionally unformatted to test the formatter's behavior.

## Manual Testing

Since the PrettyPrint.main() method calls System.exit() in several code paths (--help, --check, --initConfig, error handling), automated testing of the main() method would terminate the test JVM. Use these commands for manual CLI testing:

### Test Single File Formatting

Format a single file and output to a specific location:
```bash
./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -i src/test/resources/prettyprint/cli/input/test1.bxs -o /tmp/test1-out.bxs"
```

### Test Directory Formatting

Format all files in a directory:
```bash
./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -i src/test/resources/prettyprint/cli/input -o /tmp/output"
```

### Test Custom Config

Format using a custom configuration file:
```bash
./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -c src/test/resources/prettyprint/cli/custom-config.json -i src/test/resources/prettyprint/cli/input/test1.bxs -o /tmp/test1-custom.bxs"
```

### Test Check Mode

Check if files need formatting (exits with code 1 if formatting needed):
```bash
./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint --check -i src/test/resources/prettyprint/cli/input/test1.bxs"
```

### Test In-Place Formatting

Format a file in place (overwrites the original):
```bash
cp src/test/resources/prettyprint/cli/input/test1.bxs /tmp/test1-copy.bxs
./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint -i /tmp/test1-copy.bxs"
cat /tmp/test1-copy.bxs  # View the formatted result
```

### Test Help Option

Display help message:
```bash
./gradlew run --args="ortus.boxlang.compiler.prettyprint.PrettyPrint --help"
```

### Test Init Config

Initialize a default config file:
```bash
cd /tmp && java -jar /path/to/boxlang.jar ortus.boxlang.compiler.prettyprint.PrettyPrint --initConfig
cat .bxformat.json
```

## CLI Options

- `-h, --help` - Show help message and exit
- `-i, --input <path>` - Input file or directory to format (default: current directory)
- `-o, --output <path>` - Output file or directory (default: overwrite input files)
- `-c, --config <path>` - Configuration file path (default: .bxformat.json in current directory)
- `--check` - Check if files need formatting without modifying them (exit code 1 if changes needed)
- `--initConfig` - Create a default .bxformat.json configuration file

## Automated Tests

See `PrettyPrintMainTest.java` for automated tests that verify:
- Test files exist and are valid
- Test files can be parsed successfully
- Test files can be formatted with default and custom configs
- Custom config can be loaded
- Formatting is idempotent (formatting twice produces same result)
- Test files are intentionally unformatted
