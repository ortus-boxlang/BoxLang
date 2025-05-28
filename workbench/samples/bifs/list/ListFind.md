### Basic Example

Find item in a list and return the index.

<a href="https://try.boxlang.io/?code=eJzLySwuccvMS%2FHLd04sTtVQUEosKMhJ1ckvSsxLT9VJSswDQiUdBSWIgJKCpjUXANhmEOQ%3D" target="_blank">Run Example</a>

```java
listFindNoCase( "apple,orange,banana", "orange" );

```

Result: 2

### Different Delimiters

Find item in a pipe delimited list and return the index.

<a href="https://try.boxlang.io/?code=eJzLySwuccvMS%2FHLd04sTtVQUEosKMhJrckvSsxLT61JSswDQiUdBSWIAIhVo6Sgac0FAEQ%2FEpA%3D" target="_blank">Run Example</a>

```java
listFindNoCase( "apple|orange|banana", "orange", "|" );

```

Result: 2

### Member Syntax

listFindNoCase as a member function

<a href="https://try.boxlang.io/?code=eJxLKyrNLClWsFVQSiwoyEmtyS9KzEtPrUlKzANCJWuu8qLMklT%2F0pKC0hINhTSwYr2czOISt8y8FL9858TiVA0FJf8gRz93VyUdBaUaJQVNBU1rLgBz2hz7" target="_blank">Run Example</a>

```java
fruits = "apple|orange|banana";
writeOutput( fruits.listFindNoCase( "ORANGE", "|" ) );

```

Result: 2

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjsEKgkAYhO%2F7FMOeFJY81E2CBVMIrE49gOkPLayurP9qj99q3eo6M8z3Ld4w3QKPgRNYM3Flhu7qimaiBPKsrJtJ2dASKaa1tSQVZH0vylIiRZojy1C%2BRmqZOrjtCXsRwwv1D%2FKowtCycQMWw09odGRNH6FeTOzrSMQxgnTT6w2jO5plLpbV6xT6McF3tvuxO8VldNEfD%2FFP5CDeDk9JFA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listFindNoCase( "I,love,boxlang,testFile", "BOXLANG" ) ); // Expected output 3
// Member Function with @ delimiter
strList = "I@am@boxlang@dev";
writeDump( strList.listFindNoCase( "Dev", "@" ) );
 // Expected output 4

```


