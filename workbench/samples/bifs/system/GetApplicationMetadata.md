### Read application settings

Prints values using the application metadata.

```java
data = getApplicationMetadata()
writeOutput( "Application name is " & ( len( data.name ) ? data.name : "unspecified" ) )
```

Result: Application name is followed by the application name, or unspecified when the application has no name

### The result is a struct of settings

invokeImplicitAccessor is always present in the result.

```java
meta = getApplicationMetadata()
writeOutput( isStruct( meta ) & " " & structKeyExists( meta, "invokeImplicitAccessor" ) )
```

Result: true true
