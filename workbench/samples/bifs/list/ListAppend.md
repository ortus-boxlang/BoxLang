### Simple listAppend Example

Add 'foo' to the end of this list

<a href="https://try.boxlang.io/?code=eJzLz0nxySwuUbBVUEpKLNIBYiMlrrzUcqhgDpByLChIzUvRUMiHKNVRUErLz1dS0OQqL8osSfUvLSkoLYHLKqgpKOnq2ikBaZgpmgB6cR%2Fm" target="_blank">Run Example</a>

```java
oldList = "bar,bar2"
newList = listAppend( oldList, "foo" )
writeOutput( oldList & "-->" & newList )

```

Result: bar,bar2,foo

### Simple listAppend Example with Delimiter

Add 'foo' to the end of this list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLz0nxySwuUbBVUEpKLNIBYiMlrrzUcqhgDpByLChIzUvRUMiHKNVRUErLz1cCUjVKCppc5UWZJan%2BpSUFpSVwJQpqCkq6unZKQBpmlCYACLEg8g%3D%3D" target="_blank">Run Example</a>

```java
oldList = "bar,bar2"
newList = listAppend( oldList, "foo", "|" )
writeOutput( oldList & "-->" & newList )

```

Result: bar,bar2|foo

### Simple listAppend Example with Empty Fields On

Add 'foo,,' to the end of this list using includeEmptyFields as true

<a href="https://try.boxlang.io/?code=eJzLz0nxySwuUbBVUEpKLNIBYiMlrrzUcqhgDpByLChIzUvRUMiHKNVRUErLz9fRUQIyQERJUWmqgiZXeVFmSap%2FaUlBaQlcqYKagpKurp0SkIYZqQkAB0kjBg%3D%3D" target="_blank">Run Example</a>

```java
oldList = "bar,bar2"
newList = listAppend( oldList, "foo,,", ",", true )
writeOutput( oldList & "-->" & newList )

```

Result: bar,bar2,foo,,

### Simple listAppend Example with Empty Fields Off

Add 'foo' to the end of this list using includeEmptyFields as false

<a href="https://try.boxlang.io/?code=eJzLz0nxySwuUbBVUEpKLNIBYiMlrrzUcqhgDpByLChIzUvRUMiHKNVRUErLz9fRUQIyQERaYk5xqoImV3lRZkmqf2lJQWkJXK2CmoKSrq6dEpCGmakJACm7I1E%3D" target="_blank">Run Example</a>

```java
oldList = "bar,bar2"
newList = listAppend( oldList, "foo,,", ",", false )
writeOutput( oldList & "-->" & newList )

```

Result: bar,bar2,foo

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjssKwjAURPf9iiEbLRS7F1wIbtz4D8GMNmAepDek%2FXtjsKCrgXvmcd36srPgBBU8BylhkCmRqnOrZ%2Fmyj5xjpDd7uBYYoB4hJ4W%2BG0fcghAyaflx4p6ohTM0alEDB1wFJnD2O0GORrcUwaVC65%2FNdOxKssJLdnEbqyN%2Ft%2B2x%2Fg1dGkIx" target="_blank">Run Example</a>

```java
mylist = "one,two,three"
mynewlist = listAppend( mylist, "four" )
// Note that listAppend creates a new list. It doesn't update the existing list:
writeDump( mylist )
writeDump( mynewlist )

```


<a href="https://try.boxlang.io/?code=eJzLrczJLC5RsFVQSsvPV%2BLKrcxLLYeKgCjHgoLUvBQNhVywMh0FpaTEIiUgVaOkoMlVXpRZkupSmlsAkofp0wQAgWQbew%3D%3D" target="_blank">Run Example</a>

```java
mylist = "foo"
mynewlist = listAppend( mylist, "bar", "|" )
writeDump( mynewlist )

```


