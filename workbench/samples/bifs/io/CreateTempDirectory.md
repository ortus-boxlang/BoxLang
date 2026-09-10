### Create a temporary directory

```java
dir = createTempDirectory();
writeOutput( directoryExists( dir ) );

```

Result: true

### With a prefix

```java
dir = createTempDirectory( "myapp-" );
writeOutput( dir.contains( "myapp-" ) );

```

Result: true
