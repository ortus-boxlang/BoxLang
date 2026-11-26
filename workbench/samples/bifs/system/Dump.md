### Dump the server scope

```java
writeDump( server )
```

Result: an HTML dump of the server scope structure

### Dump with a label

```java
writeDump( var = getTimeZoneInfo(), label = "Time zone" )
```

Result: an HTML dump of the time zone struct headed by the label Time zone

### Limit recursion depth

depth is 1-based: -1 (the default) is unlimited, 0 shows nothing, 1 shows the top level with no recursion, 2 recurses once, and so on.

```java
complexObject = { a : { b : { c : 1 } } }
writeDump( var = complexObject, depth = 2 )
```

Result: a dump showing key a and its key b, without expanding the contents of b

### Limit the number of rows/items shown

maxRows limits how many keys, array elements or query rows are shown per level, independently of depth. It has the same 1-based semantics as depth.

```java
bigArray = [ 1, 2, 3, 4, 5, 6 ]
writeDump( var = bigArray, maxRows = 3 )
```

Result: a dump showing only the first 3 array items

### Dump to the console as text

```java
writeDump( var = { a : 1 }, output = "console" )
```

Result: the struct is printed as plain text on the console and nothing is added to the page output

### Deprecated: top

top is deprecated in favor of maxRows. For backwards compatibility it is still accepted and, when maxRows is not also passed, its value is used as maxRows. A deprecation warning is logged when it is used.

```java
bigArray = [ 1, 2, 3, 4, 5, 6 ]
writeDump( var = bigArray, top = 3 )
```

Result: a dump showing only the first 3 array items

### Use the bxDump member function

Any value can be dumped with the bxDump() member function.

```java
data = [ 1, 2, 3 ]
data.bxDump( label = "My array" )
```

Result: an HTML dump of the array headed by the label My array
