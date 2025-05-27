### Numeric 1 is interpreted as true



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUDBU0LTmAgBfpgcc" target="_blank">Run Example</a>

```java
trueFalseFormat( 1 );

```

Result: true

### Numeric 0 is interpreted as false



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUDBQ0LTmAgBfoQcb" target="_blank">Run Example</a>

```java
trueFalseFormat( 0 );

```

Result: false

### String representation of 1 is interpreted as true



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUFAyVFLQtOYCAG4pB2A%3D" target="_blank">Run Example</a>

```java
trueFalseFormat( "1" );

```

Result: true

### String representation of 0 is interpreted as false



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUFAyUFLQtOYCAG4jB18%3D" target="_blank">Run Example</a>

```java
trueFalseFormat( "0" );

```

Result: false

### YES is recognized as synonym for true as well



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUFCKdA1WUtC05gIAgJ4IIA%3D%3D" target="_blank">Run Example</a>

```java
trueFalseFormat( "YES" );

```

Result: true

### And NO as synonym for false



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUFDy81dS0LTmAgB3fgfM" target="_blank">Run Example</a>

```java
trueFalseFormat( "NO" );

```

Result: false

### An empty string results in false again



<a href="https://try.boxlang.io/?code=eJwrKSpNdUvMKU51yy%2FKTSzRUFBSUtC05gIAZoQHLw%3D%3D" target="_blank">Run Example</a>

```java
trueFalseFormat( "" );

```

Result: false

### Additional Examples


```java
<bx:output>
	False: #trueFalseFormat( false )#<br>
	True: #trueFalseFormat( true )#<br>
	0: #trueFalseFormat( 0 )#<br>
	1: #trueFalseFormat( 1 )#<br>
	No: #trueFalseFormat( "No" )#<br>
	Yes: #trueFalseFormat( "Yes" )#
</bx:output>
```


