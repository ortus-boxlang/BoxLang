### Get information about a loaded module

```java
info = getModuleInfo( "bx-compat-cfml" );
writeOutput( isStruct( info ) ? "found" : "not found" );

```

Result: found
