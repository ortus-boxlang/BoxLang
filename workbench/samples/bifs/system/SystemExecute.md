### Execute a system command

```java
result = systemExecute( "echo hello" );
writeOutput( result.output.contains( "hello" ) );

```

Result: true

### Execute with working directory

```java
result = systemExecute( "pwd", getCanonicalPath( "." ) );
writeOutput( isStruct( result ) );

```

Result: true
