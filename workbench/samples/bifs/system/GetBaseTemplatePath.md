### Get the base template path

```java
path = getBaseTemplatePath()
writeOutput( isSimpleValue( path ) )
```

Result: true

### Show the path

```java
writeOutput( getBaseTemplatePath() )
```

Result: the absolute file system path of the requested template, or an empty string when there is no request context (depends on your environment)
