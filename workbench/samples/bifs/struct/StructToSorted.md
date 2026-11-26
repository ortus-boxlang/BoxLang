### Sort by key with the default directive

The default directive is text ascending, which is case-sensitive: uppercase letters sort before lowercase.

```java
s = { banana : 2, Apple : 1, cherry : 3 }
sorted = structToSorted( s )
writeOutput( structKeyList( sorted ) )
```

Result: Apple,banana,cherry

### Descending, case-insensitive using the member function

```java
s = { Banana : 2, apple : 1, Cherry : 3 }
sorted = s.toSorted( "textNoCase", "desc" )
writeOutput( structKeyList( sorted ) )
```

Result: Cherry,Banana,apple

### Sort with a comparator function

The comparator receives two key names.

```java
s = { apple : 1, banana : 2, cherry : 3 }
sorted = s.toSorted( ( k1, k2 ) -> compare( k2, k1 ) )
writeOutput( structKeyList( sorted ) )
```

Result: cherry,banana,apple
