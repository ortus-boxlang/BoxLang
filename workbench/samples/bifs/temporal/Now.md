### Using now() in Script

Let's display the current server datetime using script.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQUArJSFVILi0qSs0rUUhJLElVSMxLUSjJzE1VyCy2UlBSUFPIyy%2FX0FTQtOYCAE2uElw%3D" target="_blank">Run Example</a>

```java
writeOutput( "The current date and time is: " & now() );

```

Result: The current date and time is: {ts '2014-03-19 15:27:42'}

### Using now() in Tagged BL

Let's display the current server datetime using tagged BL.


```java
<p>The current date and time is: <bx:output>#now()#</bx:output></p>
```

Result: The current date and time is: {ts '2014-03-19 15:27:42'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQUEouLSpKzStRcEksSS3JzE1VyCxWUFJQU8jLL9fQVNC05gIAm9EPTg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( "current Datetime is " & now() );

```


