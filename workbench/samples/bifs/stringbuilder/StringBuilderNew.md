### Create a StringBuilder with stringBuilderNew()

Creates a new StringBuilder instance.

```java
result = stringBuilderNew();
writeOutput( result.toString() );
```

Result: (empty string)

### Create a seeded StringBuilder

```java
result = stringBuilderNew( 'hello' );
writeOutput( result.toString() );
```

Result: hello

### Create an empty StringBuilder with explicit capacity

```java
result = stringBuilderNew( capacity = 64 );
writeOutput( result.getBuffer().capacity() );
```

Result: at least 64

### Create a seeded StringBuilder with explicit capacity

```java
result = stringBuilderNew( 'hello', 64 );
writeOutput( result.toString() );
```

Result: hello
