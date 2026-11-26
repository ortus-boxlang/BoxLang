### Get the list of ancestor tags

The result is a comma-delimited string, not an array.

```java
tags = getBaseTagList()
writeOutput( isSimpleValue( tags ) )
```

Result: true

### Inspect the ancestors as an array

```java
ancestors = getBaseTagList().listToArray()
writeOutput( isArray( ancestors ) )
```

Result: true
