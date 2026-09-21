### Check if a file is a valid ZIP archive

```java
zipFile = getTempDirectory() & "test.zip";
// Create a simple zip for testing
compress( getTempDirectory(), zipFile );
writeOutput( isZipFile( zipFile ) );

```

Result: true
