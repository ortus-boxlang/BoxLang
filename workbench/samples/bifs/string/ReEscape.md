### Example of reEscape



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKEp1LU5OLEjVUFDS0quujY5NrUjMtS%2FISVVS0FTQBABaMQ5%2B" target="_blank">Run Example</a>

```java
writeOutput( reEscape( "*.{}[]exam?ple" ) )

```

Result: \*\.\{\}\[\]exam\?ple

### Escape a literal for use in a regular expression

```java
writeOutput( reEscape( "foo.bar" ) )

```

Result: foo\.bar

### Match a literal dot

Without escaping, `a.b` would match `axb` at position 1.

```java
writeOutput( reFind( reEscape( "a.b" ), "axb a.b" ) )

```

Result: 5
