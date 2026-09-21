### Check whether two Sets contain the same elements

```java
a = setOf( 1, 2, 3 );
b = setOf( 3, 2, 1 );
writeOutput( a.equals( b ) );

```

Result: true

### Sets with different elements are not equal

```java
a = setOf( 1, 2, 3 );
b = setOf( 1, 2 );
writeOutput( a.equals( b ) );

```

Result: false
