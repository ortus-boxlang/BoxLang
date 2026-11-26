### Clear keys using a wildcard filter

A wildcard filter matches the whole key. Here every key starting with `foo` is cleared and `bar` is left.

```java
cache().set( "foo1", 1 )
cache().set( "foo2", 2 )
cache().set( "bar", 3 )
cache().clearAll( cacheFilter( "foo*" ) )
writeOutput( cache().getKeys().toList() )
```

Result: bar

### Get keys using a regular expression filter

Pass `true` to use a regular expression, which must match the whole key.

```java
cache().set( "userFoo", 1 )
cache().set( "fooBar", 2 )
cache().set( "other", 3 )
keys = cache().getKeys( cacheFilter( ".*foo.*", true ) )
writeOutput( keys.sort().toList() )
```

Result: fooBar,userFoo

### Single character wildcard

`?` matches exactly one character. Filters are case insensitive.

```java
cache().set( "item1", 1 )
cache().set( "item22", 2 )
writeOutput( cache().getKeys( cacheFilter( "ITEM?" ) ).toList() )
```

Result: item1

### Use a lambda as a custom filter

A closure or lambda that receives a key and returns a boolean can be used instead of `cacheFilter()`.

```java
cache().set( "xray", 1 )
cache().set( "yankee", 2 )
keys = cache().getKeys( ( key ) -> key.getName().startsWith( "x" ) )
writeOutput( keys.toList() )
```

Result: xray
