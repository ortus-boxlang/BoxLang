### Group array elements by a computed key

Returns a struct where each key is a value returned by the callback and each value is an array of matching elements.

```java
numbers = [ 1, 2, 3, 4, 5, 6 ];
groups = numbers.groupBy( ( n ) => n % 2 == 0 ? "even" : "odd" );
writeOutput( groups.keyList() );

```

Result: odd,even

### Group objects by a property

```java
users = [
    { name: "Alice", role: "admin" },
    { name: "Bob", role: "user" },
    { name: "Carol", role: "admin" }
];
byRole = users.groupBy( ( u ) => u.role );
writeOutput( byRole.admin.len() );

```

Result: 2

### Access a specific group

```java
numbers = [ 1, 2, 3, 4 ];
groups = numbers.groupBy( ( n ) => n % 2 == 0 ? "even" : "odd" );
writeOutput( groups.even.toString() );

```

Result: [2, 4]
