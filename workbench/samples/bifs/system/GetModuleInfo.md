### Get information about a loaded module

The result is the module record as a struct (only when the module is loaded).

```java
info = getModuleInfo( "bx-compat-cfml" )
writeOutput( isStruct( info ) )
```

Result: true

### A module that is not loaded

An empty struct is returned for unknown modules.

```java
info = getModuleInfo( "not-a-real-module" )
writeOutput( structIsEmpty( info ) )
```

Result: true
