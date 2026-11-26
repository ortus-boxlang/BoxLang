### Write a string to the console

```java
systemOutput( "Hello World" )
```

Result: Hello World is written to the console (or request output)

### Write with a trailing line break

```java
systemOutput( "first", true )
systemOutput( "second", true )
```

Result: first and second, each on its own line

### Write to the error stream

```java
systemOutput( "something went wrong", true, true )
```

Result: the message is written to standard error with a line break

### Write an array

```java
systemOutput( [ "foo", "bar" ], true )
```

Result: the string representation of the array
