### Get the default cache

Calling `cache()` with no arguments returns the `default` cache.

```java
writeOutput( cache().getName() )
```

Result: default

### Store and retrieve a value

`get()` returns an attempt, so call `get()` on it again to read the value.

```java
cache().set( "greeting", "hello" )
writeOutput( cache().get( "greeting" ).get() )
```

Result: hello

### Get a cache by name

```java
writeOutput( cache( "default" ).getName() )
```

Result: default

### Check if a key exists

```java
cache().set( "token", 123 )
writeOutput( cache().lookup( "token" ) & "," & cache().lookup( "missing" ) )
```

Result: true,false

### Cache does not exist

An exception is thrown when no cache is registered with that name.

```java
try {
	cache( "nope" )
} catch ( any e ) {
	writeOutput( e.message.contains( "does not exist" ) )
}
```

Result: true
