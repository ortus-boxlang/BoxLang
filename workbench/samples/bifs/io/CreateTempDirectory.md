### Create a temporary directory

```java
dir = createTempDirectory()
writeOutput( directoryExists( dir ) )
directoryDelete( dir )

```

Result: true

### With a prefix

The directory is the first argument, so pass the prefix by name.

```java
dir = createTempDirectory( prefix="myapp-" )
writeOutput( dir.contains( "myapp-" ) )
directoryDelete( dir )

```

Result: true
