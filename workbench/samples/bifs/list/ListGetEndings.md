### Simple listLast Example

A very basic listLast example

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvFJLAaylPLzUnVKyvN1SjKKUlN10vJLi5QUNBU0AaahEGc%3D" target="_blank">Run Example</a>

```java
writeOutput( listLast( "one,two,three,four" ) )

```

Result: four

### listLast Example with multiple delimiters

A more advanced listLast example

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvFJLAaylPLzUvVLyvNjSjKKUlP10%2FJLi5R0FJRi9JUUNBU0ARJ8Ebg%3D" target="_blank">Run Example</a>

```java
writeOutput( listLast( "one/two\three/four", "\/" ) )

```

Result: four

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjUsKAjEQRPdziiKrGWjMCVwIKghxlRNkpJFAfiQdneMbUXRVtahXT2tYH0tgnDb3zulZvXDuUrrMCL6JcW00ZXvzIHOwRGvegkt3IhJuorBgmbTGlePKFeeebuJzmppUM3jsoehCFPKD%2F6j6iI49lhnf5e6nG48vesEy4A%3D%3D" target="_blank">Run Example</a>

```java
// Simple Example
writeoutput( listLast( "Susi ,LAS,,boxlang,,,test" ) )
// Member Function
strList = ",I,,love,boxlang,,"
writeDump( strList.listLast() )

```


