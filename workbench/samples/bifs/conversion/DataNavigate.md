### Navigate into a data structure using a path

```java
data = { user: { name: "Luis", address: { city: "NYC" } } };
result = dataNavigate( data, "user.address.city" );
writeOutput( result );

```

Result: NYC

### Navigate with array indices

```java
data = { users: [ { name: "Alice" }, { name: "Bob" } ] };
result = dataNavigate( data, "users[2].name" );
writeOutput( result );

```

Result: Bob
