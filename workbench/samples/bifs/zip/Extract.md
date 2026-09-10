### Extract a zip-file

Extract a zip-file and save the data in the "output-directory".


```java
extract( "zip", "test.zip", "output-directory" );

```


### Extract a multiple zip-files via a directory

Extract all zip-files, which are stored in the "multiple-directory" and save the data in the "output-directory".


```java
extract( "zip", "multiple-directory", "output-directory" );

```


### Additional Examples


```java
extract( "zip", "D:\test.zip", "D:\zipresult" );

```

### Extract a TAR or TGZ archive

Use `tar` for a raw TAR archive and `tgz` for a gzip-compressed TAR archive.

```java
extract( format="tar", source="archive.tar", destination="output-directory" );
extract( format="tgz", source="archive.tgz", destination="output-directory" );
```

When `format` is omitted, it is detected from the source extension. An unrecognized extension requires an explicit `format`.


