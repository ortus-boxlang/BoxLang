### Contract a path relative to a base

```java
base = getTempDirectory();
full = base & "subdir/file.txt";
result = contractPath( full, base );
writeOutput( result );

```

Result: subdir/file.txt
