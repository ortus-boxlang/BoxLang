### Cast a string to a number.



<a href="https://try.boxlang.io/?code=eJwryfcrzU0tykzWUFAystQzVVLQtOYCAEwMBZ8%3D" target="_blank">Run Example</a>

```java
toNumeric( "29.5" );

```

Result: 29.5

### Cast a hex-value to a number.



<a href="https://try.boxlang.io/?code=eJwryfcrzU0tykzWUFByczMwMDRU0lFQykitUFLQtOYCAI55B%2FQ%3D" target="_blank">Run Example</a>

```java
toNumeric( "FF0011", "hex" );

```

Result: 16711697

### Cast a binary-value to a number.



<a href="https://try.boxlang.io/?code=eJwryfcrzU0tykzWUFAyNDA0UNJRUErKzFNS0LTmAgB8WAdc" target="_blank">Run Example</a>

```java
toNumeric( "1010", "bin" );

```

Result: 10

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUCjJ9yvNTS3KTNZQUDI0MtYzMVVS0FTQtOYqx67GwNDQQElHQSkpMw%2BiUEFfX8EMp2oDA2eQ6ozUCoRqQyNcyo1MQIrzk0ugbgCpNjbgAgB8ADAF" target="_blank">Run Example</a>

```java
writeDump( toNumeric( "123.45" ) );
writeDump( toNumeric( "0110", "bin" ) ); // 6
writeDump( toNumeric( "000C", "hex" ) ); // 12
writeDump( toNumeric( "24", "oct" ) );
 // 30

```


