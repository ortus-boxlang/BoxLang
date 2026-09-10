### Convert to a modifiable copy

```java
data = { name: "test" };
frozen = toUnmodifiable( data );
modifiable = toModifiable( frozen );
modifiable.name = "changed";
writeOutput( modifiable.name );

```

Result: changed
