### Simple Example

Replaces the 2nd list element with 'foo'

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglOLXEEMpWSEot0cvKLUnN1MguKS3OVdBSMdBSU0vLzlRQ0FTQBLQkR7A%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listSetAt( "bar,lorem,ipsum", 2, "foo" ) )

```

Result: bar,foo,ipsum

### Example with Custom Delimiter

Inserts 'foo' into the list with a custom delimiter

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglOLXEEMpWSEotqcvKLUnN1MguKS3NrclNrivNzU0syMvPSlXQUjHQUlNLy84EspRolBU0FTQDyWRjg" target="_blank">Run Example</a>

```java
writeOutput( listSetAt( "bar|lorem,ipsum|me|something", 2, "foo", "|" ) )

```

Result: bar|foo|me|something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNjj0LwjAQhvf8iiOThcPS1k0cFHFzcmq3NjkhkH7QJoJwP96EqtVBcHk57u6556yZXAY7kFhjgwo1khRH3w4rsGF0Ibd3c5khyFyGqCQkkIjYy2eSuWFWzJr5F50vdAjm5Ubxl7344jGEGz3FI1tIUziQqv1EYByYTlmvaQJqB3eHqyGr4VbbsI3VyyICc6a2ofHkO%2BVM3wn91mbrD7ncRGE5%2FxtVz1dLpAdgvlsQ" target="_blank">Run Example</a>

```java
list1 = ",a,b,c,d,e"
Dump( listSetAt( list1, "2", "Z" ) )
list2 = ",a||b||c||d||e"
Dump( listSetAt( list2, "2", "Z", "||" ) )
list3 = ",a,b,c,d,e"
Dump( listSetAt( list3, "2", "Z", ",", true ) ); // Because it includes empty field value ,Z,b,c,d,e
// MemberFunction
dump( list1.listSetAt( "4", "Y" ) )
 // ,a,b,c,Y,e

```


