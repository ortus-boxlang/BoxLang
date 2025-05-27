### Simple listLen Example

Get the number of elements in this list

<a href="https://try.boxlang.io/?code=eJzLySwu8UnN01BQSsvP10lKLAJhIx0gx0hJQdOaCwCyzQnQ" target="_blank">Run Example</a>

```java
listLen( "foo,bar,bar2,foo2" );

```

Result: 4

### listLen Example with Delimiter

Get the number of elements in this list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLySwu8UnN01BQSsvP10lKLKoBYqMaIMdISUdBqUZJQdOaCwDzEwt8" target="_blank">Run Example</a>

```java
listLen( "foo,bar|bar2|foo2", "|" );

```

Result: 3

### listLen Example with IncludeEmptyValues

Get the number of elements in this list, including empty values

<a href="https://try.boxlang.io/?code=eJzLySwu8UnN01BQSsvP10lKLNLRATKMlHQUlHRARKRrsJKCpjUXAPzbCqY%3D" target="_blank">Run Example</a>

```java
listLen( "foo,bar,,foo2", ",", "YES" );

```

Result: 4

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMjJLC7xSc3TUFAqLi3O1ClOzNXxcQzWKUktLlFS0FTQtFbQ11cw4SrHqQOsXAehtjyzJEMhNbegpFKhLDGnNLWYoF49iGYukG5jLgApey7b" target="_blank">Run Example</a>

```java
writeDump( listLen( "susi,sam,LAS,test" ) ); // 4
writeDump( listLen( "susi,,LAS,," ) ); // with empty values
writeDump( listLen( "susi,,LAS,,." ) );
 // 3

```


