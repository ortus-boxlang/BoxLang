### Simple listRest Example

A very basic listRest example

<a href="https://try.boxlang.io/?code=eJzLySwuCUotLtFQUMrPS9UpKc%2FXKckoSk3VScsvLVJS0LTmAgDXDgtp" target="_blank">Run Example</a>

```java
listRest( "one,two,three,four" );

```

Result: two,three,four

### Combining listRest to Shorten the List

Nesting listRest shortens the list by one each time with the first element removed.

<a href="https://try.boxlang.io/?code=eJzLySwuCUotLtFQyIGzlPLzUnVKyvN1SjKKUlN10vJLi5QUNBU0rbkAh1wPVA%3D%3D" target="_blank">Run Example</a>

```java
listRest( listRest( "one,two,three,four" ) );

```

Result: three,four

### Traversing a List with listRest and listFirst

Nesting list functions lets you move through the list in pieces.

<a href="https://try.boxlang.io/?code=eJzLySwuccssKi7RUMgBMoNSUVlK%2BXmpOiXl%2BTolGUWpqTpp%2BaVFSgqaIGjNBQCDahOp" target="_blank">Run Example</a>

```java
listFirst( listRest( listRest( "one,two,three,four" ) ) );

```

Result: three

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglKLQaylIpLijLz0nV8SpNTU3WKS4szdXwcg5UUNBXUFJRskorsQBjItebS11fwTc1NSi1SSCvNSy7JzM%2FjAur1AZqkYKugZKBjqGOkY6xjomTNVQ6yyqU0t0BDAagCZJce3EJNkFEAlXUsRQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listRest( "string,Lucee,susi,LAS" ) & "<br><br>" );
// Member function
strList = "0,1,2,3,4";
writeDump( strlist.listRest() );

```


