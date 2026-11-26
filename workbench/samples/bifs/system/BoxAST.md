### Get the AST for BoxLang source code

The default return type is a struct. Every node has an ASTType key.

```java
ast = boxAST( "x = 1 + 2" )
writeOutput( ast.containsKey( "ASTType" ) )
```

Result: true

### Get the AST as JSON text

```java
json = boxAST( source = "x = 1 + 2", returnType = "json" )
writeOutput( isJSON( json ) )
```

Result: true

### Use the member function on a string

```java
ast = "a = [ 1, 2, 3 ]".toAST()
writeOutput( isStruct( ast ) )
```

Result: true

### Missing source and filepath

```java
try {
    boxAST()
} catch ( any e ) {
    writeOutput( e.message contains "Either 'source' or 'filepath' argument must be provided" )
}
```

Result: true
