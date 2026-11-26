### Insert text with stringBuilderInsert()

Inserts text at a 1-based position.

```java
sb = sb{'Hello World'}
stringBuilderInsert( sb, 7, 'Beautiful ' )
writeOutput( sb.toString() )
```

Result: Hello Beautiful World

### Member usage

```java
sb = sb{'HelloWorld'}
sb.insert( 6, ' ' )
writeOutput( sb.toString() )
```

Result: Hello World

### Out-of-range position throws an error

```java
sb = sb{'Hello'}
try {
    sb.insert( 9, "x" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: Position [9] is out of range. Valid range is 1..6.
