### Get a system setting or environment variable

```java
result = getSystemSetting( "java.version" );
writeOutput( isString( result ) );

```

Result: true

### With a default value

```java
result = getSystemSetting( "NONEXISTENT_VAR", "default-value" );
writeOutput( result );

```

Result: default-value
