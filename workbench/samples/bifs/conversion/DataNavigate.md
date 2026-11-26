### Navigate into a struct using a path

```java
data = { user: { name: "Luis", address: { city: "NYC" } } }
result = dataNavigate( data ).get( "user.address.city" )
writeOutput( result )

```

Result: NYC

### Navigate with array indices

```java
data = { users: [ { name: "Alice" }, { name: "Bob" } ] }
result = dataNavigate( data ).get( "users[2].name" )
writeOutput( result )

```

Result: Bob

### Chain from() calls and provide a default

```java
data = { app: { settings: { theme: "dark" } } }
settings = dataNavigate( data ).from( "app.settings" )
writeOutput( settings.get( "theme" ) & " " & settings.getOrDefault( "lang", "en" ) )

```

Result: dark en

### Navigate a JSON string

```java
json = '{"server":{"port":8080}}'
writeOutput( dataNavigate( json ).getAsInteger( "server.port" ) )

```

Result: 8080

### Query with a JSONPath expression

```java
data = { items: [ { name: "alpha", active: true }, { name: "beta", active: false } ] }
active = dataNavigate( data ).query( "items[?(@.active == true)]" )
writeOutput( active.len() )

```

Result: 1
