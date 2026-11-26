### Read options and positionals

Run the script with `boxlang args.bxs --debug --bundles=Spec my/path`. Flags without a value are `true`, and `--key=value` sets a value.

```java
args = cliGetArgs()
writeOutput( args.options.debug & "," & args.options.bundles )
```

Result: true,Spec

### Negated and shorthand options

Run it with `boxlang args.bxs --!verbose -o="/path/to/file" -v`. `--!name` (or `--no-name`) sets an option to false and `-v` is a shorthand boolean option.

```java
args = cliGetArgs()
writeOutput( args.options.verbose & "," & args.options.o & "," & args.options.v )
```

Result: false,/path/to/file,true

### Positional arguments

When running a script with the `boxlang` binary the script path is the first positional. Run it with `boxlang args.bxs my/path/template`.

```java
args = cliGetArgs()
writeOutput( args.positionals.last() )
```

Result: my/path/template

### No arguments

Without any extra arguments the options struct is empty.

```java
writeOutput( cliGetArgs().options.len() )
```

Result: 0
