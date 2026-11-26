### Create temp file in temp dir

Returns path of file created

<a href="https://try.boxlang.io/?code=eJxLTy0JSc0tcMvMSdVQSIdwXDKLUpNL8osqNTR1FJRKUotLQNJKCprWXACZ5g%2FZ" target="_blank">Run Example</a>

```java
getTempFile( getTempDirectory(), "testFile" )

```

Result: /private/var/folders/k6/hm9skhxj2dd_901z2f2mkwt00000gn/T/testFile13427324567646329113.tmp

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwTy0JSc0tcMvMSdVQSIdwXDKLUpNL8osqNTR1FJRSUnPzlRQ0FTStuQDQaBBl" target="_blank">Run Example</a>

```java
dump( GetTempFile( getTempDirectory(), "demo" ) )

```

### Use a prefix and suffix in a given directory

```java
dir = createTempDirectory( prefix="docs-" )
file = createTempFile( dir, "report-", ".csv" )
writeOutput( listLast( file, "/" ).startsWith( "report-" ) & " " & file.endsWith( ".csv" ) & " " & fileExists( file ) )
directoryDelete( dir, true )
```

Result: true true true
