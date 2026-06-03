### Test that no list elements satisfy a predicate

```java
result = listNone( "1,2,3,4", ( item ) => item > 10 );
writeOutput( result );

```

Result: true

### Returns false when at least one element matches

```java
result = listNone( "1,2,3,4", ( item ) => item > 2 );
writeOutput( result );

```

Result: false
