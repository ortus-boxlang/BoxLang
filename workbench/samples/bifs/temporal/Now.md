### Using now() in Script

Let's display the current server datetime using script.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQUArJSFVILi0qSs0rUUhJLElVSMxLUSjJzE1VyCy2UlBSUFPIyy%2FX0FTQBAApABIX" target="_blank">Run Example</a>

```java
writeOutput( "The current date and time is: " & now() )

```

Result: The current date and time is: {ts '2014-03-19 15:27:42'}

### Using now() in Tagged BL

Let's display the current server datetime using tagged BL.


```java
<p>The current date and time is: <bx:output>#now()#</bx:output></p>
```

Result: The current date and time is: {ts '2014-03-19 15:27:42'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQUEouLSpKzStRcEksSS3JzE1VyCxWUFJQU8jLL9fQVNAEAH0%2FDwk%3D" target="_blank">Run Example</a>

```java
writeOutput( "current Datetime is " & now() )

```


### Now in a specific timezone

now() returns a DateTime representing the current instant, so these examples show deterministic properties rather than the changing value.

```java
writeOutput( offset( now( "Asia/Tokyo" ) ) )
```

Result: +0900

### The result is a DateTime

```java
writeOutput( now().getClass().getSimpleName() )
```

Result: DateTime
