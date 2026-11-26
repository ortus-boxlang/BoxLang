### Convert a string to camelCase

```java
result = camelCase( "hello_world" )
writeOutput( result )

```

Result: helloWorld

### Convert kebab-case to camelCase

```java
result = camelCase( "my-variable-name" )
writeOutput( result )

```

Result: myVariableName

### Using the member function

```java
result = "foo_bar_baz".camelCase()
writeOutput( result )

```

Result: fooBarBaz

### Words are split on spaces, hyphens and underscores

```java
writeOutput( camelCase( "HELLO big_WORLD-wide" ) )

```

Result: helloBigWorldWide

### Existing camelCase words are not split

Only whitespace, hyphens and underscores are treated as word separators, and every word is lowercased first.

```java
writeOutput( camelCase( "helloWorld" ) )

```

Result: helloworld
