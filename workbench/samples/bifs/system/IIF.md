### Simple iIf example

Literal strings must be wrapped with de() so they are not evaluated as variables.

```java
writeOutput( iIf( server.os.name == "Bacon", de( "Running Bacon OS" ), de( "Not Running Bacon OS" ) ) )
```

Result: Not Running Bacon OS

### Return a variable value

An expression that is not wrapped in de() is looked up as a variable.

```java
name = "Luis"
writeOutput( iIf( true, "name", de( "anonymous" ) ) )
```

Result: Luis

### Use the ternary operator instead

Prefer the ternary operator: it only evaluates the branch that is used and needs no de().

```java
writeOutput( server.os.name == "Bacon" ? "Running Bacon OS" : "Not Running Bacon OS" )
```

Result: Not Running Bacon OS
