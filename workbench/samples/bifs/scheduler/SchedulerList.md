### List all registered scheduler names

```java
schedulerNew( "listSched" )
names = schedulerList()
writeOutput( isArray( names ) & " " & names.contains( "listSched" ) )

```

Result: true true

### Check if a scheduler is registered

```java
schedulerNew( "checkSched" )
writeOutput( schedulerList().contains( "checkSched" ) & " " & schedulerList().contains( "nope" ) )

```

Result: true false
