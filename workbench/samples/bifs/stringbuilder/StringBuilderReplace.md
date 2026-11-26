### Replace a range with stringBuilderReplace()

Replaces characters using 1-based, inclusive start and end positions.

```java
sb = sb{'Hello World'}
stringBuilderReplace( sb, 7, 11, 'BoxLang' )
writeOutput( sb.toString() )
```

Result: Hello BoxLang

### Member usage

```java
sb = sb{'Hello World'}
sb.replace( 7, 11, 'BoxLang' )
writeOutput( sb.toString() )
```

Result: Hello BoxLang

### Invalid range throws an error

```java
sb = sb{'Hello'}
try {
    sb.replace( 4, 2, "x" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: Invalid replace range [4,2]. Valid range is 1..5 with end >= start.
