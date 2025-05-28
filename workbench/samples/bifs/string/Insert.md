### Simple insert function example

To add substring on prefix of the given string

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pysxLV7BVUFJIzigCCigkFeWXF6cWKVlzFaUWl%2BaUAOUy84ACJRoKSu75%2Bek5qUo6CsVwrToKBgqa1lzlRZklqf6lJQWlQHVQjUBhAKORIUA%3D" target="_blank">Run Example</a>

```java
someString = " chrome browser";
result = insert( "Google", someString, 0 );
writeOutput( result );

```

Result: Google chrome browser

### Simple insert function example with position

To add substring on suffix of the given string

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pysxLV7BVUPJLLVcoKMosSyxJVcjNr8rMyUlUSMssSlWy5spJzUsvyQAqAjI0FIoR2jStucqLMktS%2FUtLCkpLNBQy84pTi4C0Ulp%2BhZIOkkodBagZmiA9ABjVKSI%3D" target="_blank">Run Example</a>

```java
someString = "New private mozilla fire";
length = len( someString );
writeOutput( insert( "fox", someString, length ) );

```

Result: New private mozilla firefox

### Additional Examples

<a href="https://try.boxlang.io?code=eJxVjbsOgkAQRfv5iputICFSWhA7LSz8CMDRbLIPMjMonw%2BY1cT2npxz1QQnuCtCfjFcR2%2Fxxuc5ThV8Uhar4Ia8hD49XQM1aXBEjbpD26JohdO23DgOLHjMaTSfE6nt%2BcvSxynwf17t8HvAmO%2Fsvmna28X5EFoBANkzHQ%3D%3D" target="_blank">Run Example</a>

```java
str = "I love ";
writeDump( insert( "boxlang", str, 7 ) ); // I love boxlang
// Member function
st = "Example";
writeDump( st.insert( " code", 7 ) );
 // Example code

```
