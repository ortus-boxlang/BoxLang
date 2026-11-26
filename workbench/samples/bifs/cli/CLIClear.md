### Clear the console screen

Clears the terminal before the next output is written. It uses an ANSI escape sequence on Linux and macOS and `cls` on Windows.

```java
cliClear()
writeOutput( "The screen was cleared" )
```

Result: The screen was cleared (printed on a cleared screen)

### Redraw a screen in a loop

Clearing the screen before each draw is useful for simple text user interfaces.

```java
for ( i = 1; i <= 3; i++ ) {
	cliClear()
	writeOutput( "Step #i#" )
	sleep( 100 )
}
```

Result: Step 3 (each step is drawn on a cleared screen)
