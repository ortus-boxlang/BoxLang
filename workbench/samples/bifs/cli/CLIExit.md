### Exit the CLI with an exit code

The process terminates immediately, so the last line is never executed. Run it with `boxlang exit.bxs`; the exit code is available to the shell in `$?`.

```java
println( "before exit" )
cliExit( 3 )
println( "not reached" )
```

Result: before exit (the process ends with exit code 3)

### Exit with the default code

When no exit code is passed, 0 (success) is used.

```java
println( "done" )
cliExit()
```

Result: done (the process ends with exit code 0)
