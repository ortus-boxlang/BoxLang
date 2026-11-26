### Simple insert function example

To add substring on prefix of the given string

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pysxLV7BVUFJIzigCCigkFeWXF6cWKXEVpRaX5pQApTLzgPwSDQUl9%2Fz89JxUJR2FYrhOHQUDBU2u8qLMklT%2F0pKCUqAyqD5NACkfIIU%3D" target="_blank">Run Example</a>

```java
someString = " chrome browser"
result = insert( "Google", someString, 0 )
writeOutput( result )

```

Result: Google chrome browser

### Simple insert function example with position

To add substring on suffix of the given string

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pysxLV7BVUPJLLVcoKMosSyxJVcjNr8rMyUlUSMssSlXiyknNSy%2FJAKoBMjQUihG6NLnKizJLUv1LSwpKSzQUMvOKU4uAtFJafoWSDpJCHQWoEZoKmgCC%2FChn" target="_blank">Run Example</a>

```java
someString = "New private mozilla fire"
length = len( someString )
writeOutput( insert( "fox", someString, length ) )

```

Result: New private mozilla firefox

### Insert in the middle of a string

```java
writeOutput( insert( "boxlang", "I love ", 7 ) )

```

Result: I love boxlang

### Member function

```java
writeOutput( "Example".insert( " code", 7 ) )

```

Result: Example code

### Error: position out of range

```java
try {
	insert( "x", "abc", 10 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Position must be within the range [0, 3]
