### Bind values into a string template

```java
result = stringBind( "Hello, {name}! You have {count} messages.", { name: "Luis", count: 5 } );
writeOutput( result );

```

Result: Hello, Luis! You have 5 messages.

### Using the member function

```java
result = "Welcome, {user}!".stringBind( { user: "World" } );
writeOutput( result );

```

Result: Welcome, World!

### Missing keys remain as placeholders

```java
result = stringBind( "Hello, {name}!", {} );
writeOutput( result );

```

Result: Hello, {name}!
