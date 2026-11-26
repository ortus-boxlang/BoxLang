### Retrieve the parent tag data

Inside a custom tag, find the ancestor tag cf_mapper and read its thisTag data.

```java
tagData = getBaseTagData( "cf_mapper" )
writeOutput( tagData.thisTag.executionMode )
```

Result: the execution mode of the ancestor cf_mapper tag (for example start or end)

### Retrieve the parent tag attributes

```java
parentAttributes = getBaseTagData( "cf_iframe" ).attributes
writeOutput( parentAttributes.keyList() )
```

Result: the attribute names passed to the ancestor cf_iframe tag

### Ancestor not found

Called outside of the named custom tag, a validation exception is thrown.

```java
try {
    getBaseTagData( "cf_mapper" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: mapper not found
