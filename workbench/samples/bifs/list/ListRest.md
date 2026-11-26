### Simple listRest Example

A very basic listRest example

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglKLQaylPLzUnVKyvN1SjKKUlN10vJLi5QUNBU0AafJEHE%3D" target="_blank">Run Example</a>

```java
writeOutput( listRest( "one,two,three,four" ) )

```

Result: two,three,four

### Combining listRest to Shorten the List

Nesting listRest shortens the list by one each time with the first element removed.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglKLUZhKeXnpeqUlOfrlGQUpabqpOWXFikpaIIgAJRHFFw%3D" target="_blank">Run Example</a>

```java
writeOutput( listRest( listRest( "one,two,three,four" ) ) )

```

Result: three,four

### Traversing a List with listRest and listFirst

Nesting list functions lets you move through the list in pieces.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnHLLCqGMoNSUVlK%2BXmpOiXl%2BTolGUWpqTpp%2BaVFSgqaEAgA0YkYsQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listFirst( listRest( listRest( "one,two,three,four" ) ) ) )

```

Result: three

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglKLQaylIpLijLz0nWc8ityEoF0cWlxpo6PY7CSgqaCmoKSTVKRHQgDuVz6%2Bgq%2BqblJqUUKaaV5ySWZ%2BXlcQM0%2BQKMUbBWUDHQMdYx0jHVMlLjKQVa5lOYWaCgAFYDs0oNbqKmgCQBh%2Byxn" target="_blank">Run Example</a>

```java
writeOutput( listRest( "string,Boxlang,susi,LAS" ) & "<br><br>" )
// Member function
strList = "0,1,2,3,4"
writeDump( strlist.listRest() )

```


