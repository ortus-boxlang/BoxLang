### Simple listLen Example

Get the number of elements in this list

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvFJzdNQUErLz9dJSiwCYSMdIMdISUFTQRMAeYAO2A%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listLen( "foo,bar,bar2,foo2" ) )

```

Result: 4

### listLen Example with Delimiter

Get the number of elements in this list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvFJzdNQUErLz9dJSiyqAWKjGiDHSElHQalGSUFTQRMA0toQhA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listLen( "foo,bar|bar2|foo2", "|" ) )

```

Result: 3

### listLen Example with IncludeEmptyValues

Get the number of elements in this list, including empty values

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLvFJzdNQUErLz9dJSizS0QEyjJR0FJR0QESka7CSgqaCJgDrrg%2Bu" target="_blank">Run Example</a>

```java
writeOutput( listLen( "foo,bar,,foo2", ",", "YES" ) )

```

Result: 4

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMjJLC7xSc3TUFAqLi3O1ClOzNXxcQzWKUktLlFS0FTQtFbQ11cw4SrHqQOsXAehtjyzJEMhNbegpFKhLDGnNLWYoF49sGYukGZjAMs5LpY%3D" target="_blank">Run Example</a>

```java
writeDump( listLen( "susi,sam,LAS,test" ) ); // 4
writeDump( listLen( "susi,,LAS,," ) ); // with empty values
writeDump( listLen( "susi,,LAS,,." ) )
 // 3

```

### Count items, ignoring empty fields

```java
writeOutput( listLen( "a,b,,c" ) )

```

Result: 3

### Member function including empty fields

```java
writeOutput( "a,b,,c".listLen( ",", true ) )

```

Result: 4
