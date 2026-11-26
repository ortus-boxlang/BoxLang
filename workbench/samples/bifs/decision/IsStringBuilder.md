### Check a StringBuilder created with stringBuilderNew()

```java
sb = stringBuilderNew( "Hello" )
writeOutput( isStringBuilder( sb ) )
```

Result: true

### A plain string is not a StringBuilder

```java
writeOutput( isStringBuilder( "Hello" ) )
```

Result: false

### Other values return false

```java
writeOutput( isStringBuilder( [ "a", "b" ] ) & " " & isStringBuilder( 42 ) )
```

Result: false false
