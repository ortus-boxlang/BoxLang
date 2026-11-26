### Script Syntax

Checking if a directory called 'icons' exists and then creating the directory if it does not exist.

<a href="https://try.boxlang.io/?code=eJzLTNNQUEzJLEpNLskvqnStyCwuKdZQSK0oSMxLCUgsydBQUNJPLC5OLSnWz8xN189Mzs8rVlLQBMNqLk64Tuei1MSSVKBqLIq5agEH6CJA" target="_blank">Run Example</a>

```java
if( !directoryExists( expandPath( "/assets/img/icons" ) ) ) {
	directoryCreate( "assets/img/icons" )
}

```

Result: The directory 'icons' will be created under the img folder.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJw9ikEKgDAMBO%2B%2BYvGkIPQRfsCDHwg22EBtSxpRf68oeJnDzDiHOUjFITFiUSZjFFJOBi%2FKi2W9cAROSNnAp1Rr%2FjC%2Bf%2FfoQslPZKFDKxutXN1eYiZfW%2FQDTHf%2BiP4GE%2BEoDQ%3D%3D" target="_blank">Run Example</a>

```java
// This will create parent directory when not exist
directoryCreate( expandPath( "images/uploads" ), true, true )

```

### Create nested directories in one call

```java
dir = getTempDirectory() & "docs-nested"
directoryCreate( dir & "/a/b/c" )
writeOutput( directoryExists( dir & "/a/b/c" ) )
directoryDelete( dir, true )
```

Result: true

### An existing directory throws unless ignoreExists is true

```java
dir = createTempDirectory( prefix="docs-" )
try {
	directoryCreate( dir )
} catch ( any e ) {
	writeOutput( e.message.replace( dir, "<dir>" ) )
}
directoryCreate( dir, true, true )
directoryDelete( dir, true )
```

Result: The directory [<dir>] already exists. Set the boolean argument ignoreExists to true to prevent this error
