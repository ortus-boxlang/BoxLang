### Get a system property

System properties are checked first, then environment variables.

```java
result = getSystemSetting( "java.version" )
writeOutput( isSimpleValue( result ) )
```

Result: true

### With a default value

```java
result = getSystemSetting( "NONEXISTENT_VAR", "default-value" )
writeOutput( result )
```

Result: default-value

### Missing setting without a default

```java
try {
    getSystemSetting( "NONEXISTENT_VAR" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: System property or environment variable not found: NONEXISTENT_VAR
