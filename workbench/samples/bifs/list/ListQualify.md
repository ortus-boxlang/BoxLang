### Simple example for listQualify function with delimiter

To insert a string or character before and after the list elements.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErOz0lJKy3OzM%2BzKkrMzMm3MjQxscopTU5NtTIxU7LmKi%2FKLEn1Ly0pKC3RUMgBagosTczJTKuEcHQUlGqUgISVkoKmgqY1FwAAjRtV" target="_blank">Run Example</a>

```java
list = "coldfusion:railo:144:lucee:46";
writeOutput( listQualify( list, "|", ":" ) );

```

Result: |coldfusion|:|railo|:|144|:|lucee|:|46|

### Example for listQualify function with elements

To insert a string or character before and after the alphabet list elements only.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErOz0lJKy3OzM%2BzKkrMzMm3MjQxscopTU5NtTIxU7LmKi%2FKLEn1Ly0pKC3RUMgBagosTczJTKuEcHQUlGqUgIQViHD2cAxSUtBU0LTmAgDllR0D" target="_blank">Run Example</a>

```java
list = "coldfusion:railo:144:lucee:46";
writeOutput( listQualify( list, "|", ":", "CHAR" ) );

```

Result: |coldfusion|:|railo|:144:|lucee|:46

### Example for listQualify function with includeEmptyFields

If includeEmptyFields is true, empty value add in list elements.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErOz0lJKy3OzM%2BzKkrMzMm3MjQxsbLKKU1OTbWyMjFTsuYqL8osSfUvLSkoLdFQyAFqCyxNzMlMq4RwdBSUapSAhBWIcPZwDALSJUWlqQqaCprWXADWtx%2BD" target="_blank">Run Example</a>

```java
list = "coldfusion:railo:144::lucee::46";
writeOutput( listQualify( list, "|", ":", "CHAR", true ) );

```

Result: |coldfusion|:|railo|:144:||:|lucee|:||:46

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjsEKwjAMhu97itCDdDDMA0wFUQRhIlK8Cs5lGmi70bUMwYc3UxA8JOQ%2F5Pt%2BRDDsekvQJn%2BL3HkYOT6g4balQD5CQ5YdRwoa82wMch1T7FPUYHmIp3S13D41qCEG9nes0o0IhzQwVmujClCXaaGCHGagFnVYTSOxzBDhQK6m8JNnQqkEC0tQH1Kx6WyzE1rnC%2BEV5mz2qvz22CbXa5CPqcj8v81r8oniDeOFSOE%3D" target="_blank">Run Example</a>

```java
// Simple function with different delimiter(/)
writeOutput( listQualify( "string/Lucee/susi/LAS", "^", "/" ) & "<br><br>" );
// Member function
strList = "Lucee,ColdFusion,LAS,SUSI";
writeDump( strlist.listQualify( "|" ) );

```


