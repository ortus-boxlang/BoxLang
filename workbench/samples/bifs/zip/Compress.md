### Compress a file

Compress the file "example.txt" to a zip-file.


```java
compress( "zip", "example.txt", "output.zip" );

```


### Compress a directory

Compress the "example-directory" to a zip-file.


```java
compress( "zip", "example-directory", "output.zip" );

```


### Additional Examples

### Compress a TAR or TGZ archive

Use `tar` for a raw TAR archive and `tgz` for a gzip-compressed TAR archive.

```java
compress( format="tar", source="example-directory", destination="output.tar" );
compress( format="tgz", source="example-directory", destination="output.tgz" );
```

When `format` is omitted, it is detected from the destination extension. An unrecognized extension requires an explicit `format`.

