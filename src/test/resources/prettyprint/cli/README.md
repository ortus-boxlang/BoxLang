# PrettyPrint CLI Test Files

This directory contains test files for manually testing the PrettyPrint CLI options.

## Test Files

- **input/test1.bxs** - Simple BoxLang script with function
- **input/test2.cfm** - CFML file with function
- **input/test3.bx** - BoxLang class file
- **custom-config.json** - Custom BXFormat configuration (indentSize: 2, maxLineLength: 120)
- **test.cfformat.json** - CFFormat configuration file for testing conversion

All input files are intentionally unformatted to test the formatter's behavior.

## Configuration File Support

The formatter supports two configuration file formats:

1. **.bxformat.json** - Native BoxLang format configuration (preferred)
2. **.cfformat.json** - CFFormat configuration (for compatibility with existing projects)

When no explicit config is specified, the formatter looks for configuration in this order:
1. `.bxformat.json` in the working directory
2. `.cfformat.json` in the working directory (auto-converted)
3. Default settings

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

### Test Convert CFFormat Config

Convert a .cfformat.json file to .bxformat.json:
```bash
# Convert in current directory
boxlang format --convertConfig

# Or specify a path
boxlang format --convertConfig --input /path/to/.cfformat.json
```

### Test with CFFormat Config

The formatter can directly use .cfformat.json files:
```bash
# Explicit config path
boxlang format --config /path/to/.cfformat.json -i src/

# Or just put .cfformat.json in the working directory and it will be auto-detected
```

## CLI Options

- `-h, --help` - Show help message and exit
- `-i, --input <path>` - Input file or directory to format (default: current directory)
- `-o, --output <path>` - Output file or directory (default: overwrite input files)
- `-c, --config <path>` - Configuration file path (default: .bxformat.json or .cfformat.json)
- `--check` - Check if files need formatting without modifying them (exit code 1 if changes needed)
- `--initConfig` - Create a default .bxformat.json configuration file
- `--convertConfig` - Convert .cfformat.json to .bxformat.json

## Automated Tests

See `PrettyPrintMainTest.java` for automated tests that verify:
- Test files exist and are valid
- Test files can be parsed successfully
- Test files can be formatted with default and custom configs
- Custom config can be loaded
- Formatting is idempotent (formatting twice produces same result)
- Test files are intentionally unformatted
