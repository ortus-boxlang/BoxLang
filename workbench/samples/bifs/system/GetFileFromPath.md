### Get the file name from a path

```java
writeOutput( getFileFromPath( "/var/www/app/file.txt" ) )
```

Result: file.txt

### Windows style paths

Both separators are understood.

```java
writeOutput( getFileFromPath( "C:\temp\report.pdf" ) )
```

Result: report.pdf
