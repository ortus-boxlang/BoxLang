### Delete a registered executor by name

```java
executorNew( "tempPool", "fixed", 4 )
executorDelete( "tempPool" )
writeOutput( executorHas( "tempPool" ) )
```

Result: false

### Deleting always returns true

Names that are not registered are ignored.

```java
writeOutput( executorDelete( "unknownPool" ) )
```

Result: true
