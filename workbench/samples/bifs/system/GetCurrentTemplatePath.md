### Get the current template path

```java
path = getCurrentTemplatePath()
writeOutput( isSimpleValue( path ) )
```

Result: true

### Show the absolute path

```java
writeOutput( getCurrentTemplatePath() )
```

Result: the absolute file system path of the file that contains this call (depends on your environment)

### Extract only the file name

```java
writeOutput( getFileFromPath( getCurrentTemplatePath() ) )
```

Result: the file name of the calling template, for example index.bxm
