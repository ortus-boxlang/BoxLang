### Output a literal string

```java
writeOutput( "Hello World" )
```

Result: Hello World

### Output a variable

```java
greeting = "Hello World"
writeOutput( greeting )
```

Result: Hello World

### Output HTML-safe text

Wrap untrusted values with encodeForHTML().

```java
name = "<b>Pete</b>"
writeOutput( "Hello " & encodeForHTML( name ) )
```

Result: Hello &lt;b&gt;Pete&lt;/b&gt;

### Use the echo alias

```java
echo( "Hello " )
echo( "echo" )
```

Result: Hello echo
