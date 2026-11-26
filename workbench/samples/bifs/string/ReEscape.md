### Example of reEscape



<a href="https://try.boxlang.io/?code=eJwrSnUtTk4sSNVQUNLSq66Njk2tSMy1L8hJVVLQtOYCAJ2GCXY%3D" target="_blank">Run Example</a>

```java
reEscape( "*.{}[]exam?ple" )

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
