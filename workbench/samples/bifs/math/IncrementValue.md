### Increment 7



<a href="https://try.boxlang.io/?code=eJzLzEsuSs1NzSsJS8wpTdVQMFfQtOYCAFgVBtA%3D" target="_blank">Run Example</a>

```java
incrementValue( 7 );

```

Result: 8

### Increment 7.5

There is a difference between BL engines. ACF will return the integer incremented removing the decimal part. Boxlang will increment the integer part but return both.

<a href="https://try.boxlang.io/?code=eJzLzEsuSs1NzSsJS8wpTdVQMNczVdC05gIAZrYHMw%3D%3D" target="_blank">Run Example</a>

```java
incrementValue( 7.5 );

```

Result: 8.5

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81VsFUwNLO05iovyixJdSnNLdBQyMxLLkrNTc0rKUvMKU3VUMgDqtJU0LTmygMr1zMlUjkApk4eFQ%3D%3D" target="_blank">Run Example</a>

```java
num = 169;
writeDump( incrementvalue( num ) );
num = .59;
writeDump( incrementvalue( num ) );

```


