### Decrement 7



<a href="https://try.boxlang.io/?code=eJxLSU0uSs1NzSsJS8wpTdVQMFfQtOYCAFb4BsI%3D" target="_blank">Run Example</a>

```java
decrementValue( 7 );

```

Result: 6

### Decrement 7.5

There is a difference between CFML engines.  ACF will return the integer decremented removing the decimal part, returns 6.  Lucee will decrement the integer part but return both, returns 6.5.

<a href="https://try.boxlang.io/?code=eJxLSU0uSs1NzSsJS8wpTdVQMNczVdC05gIAZX0HJQ%3D%3D" target="_blank">Run Example</a>

```java
decrementValue( 7.5 );

```

Result: 6.5

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUEhJTS5KzU3NKwlLzClN1VAwU9BU0LRW0NdXMOUqx6NMzxSskAusUs%2BUCwB4Shib" target="_blank">Run Example</a>

```java
writeDump( decrementValue( 6 ) ); // 5
writeDump( decrementValue( 6.5 ) );
 // 5.5

```


