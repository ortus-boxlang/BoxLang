### Find the first element matching a predicate

Returns the first element for which the callback returns `true`.

```java
users = [ { name: "Ada" }, { name: "Grace" }, { name: "Linus" } ];
found = users.findFirst( ( user ) => user.name == "Grace" );
writeOutput( found.name );

```

Result: Grace

### With a default value when nothing matches

```java
users = [ { name: "Ada" } ];
found = users.findFirst( ( user ) => user.name == "Nobody", "Unknown" );
writeOutput( found.name );

```

Result: Unknown

### Find the first even number

```java
numbers = [ 1, 3, 5, 6, 7, 8 ];
found = numbers.findFirst( ( n ) => n % 2 == 0 );
writeOutput( found );

```

Result: 6
