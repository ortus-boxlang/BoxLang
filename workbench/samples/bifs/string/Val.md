### Numeric characters at beginning and middle of a string



<a href="https://try.boxlang.io/?code=eJwrS8zRUFAyNDIoSS0uMTQqLi4pysxLV1LQtOYCAHXFCB4%3D" target="_blank">Run Example</a>

```java
val( "120test12sstring" );

```

Result: 120

### Numeric characters only at the end of a string



<a href="https://try.boxlang.io/?code=eJwrS8zRUFAqLinKzEu3tFBS0LTmAgA9cAVm" target="_blank">Run Example</a>

```java
val( "string98" );

```

Result: 0

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FAIS8zRUFAyNDI2UfBNzMxTCC7RU1LQVNC0VtDXVwAJc6UgqYMp0QFLIRQaoKgCyumZmJqhmAMSQFGEUzOSNhSJ%2FLxUPBaGwC3kgtrIBQBiHjMQ" target="_blank">Run Example</a>

```java
dump( Val( "1234 Main St." ) ); // 1234
dump( Val( "Main St., 1234" ) ); // 0
dump( Val( "123.456" ) ); // 123.456
dump( Val( "" ) ); // 0
dump( Val( "1" ) ); // 1
dump( Val( "one" ) ); // 0
dump( Val( "123T456" ) );
 // 123

```


