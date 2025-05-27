### Format 10 as dual number



<a href="https://try.boxlang.io/?code=eJxLyy%2FKTSxxSixO9dNQMDTQUTBS0LTmAgBU9gYI" target="_blank">Run Example</a>

```java
formatBaseN( 10, 2 );

```

Result: 1010

### Format 1024 as hexadecimal number



<a href="https://try.boxlang.io/?code=eJxLyy%2FKTSxxSixO9dNQMDQwMtFRMDRT0LTmAgBpOAaj" target="_blank">Run Example</a>

```java
formatBaseN( 1024, 16 );

```

Result: 400

### Format 125 as decimal number



<a href="https://try.boxlang.io/?code=eJxLyy%2FKTSxxSixO9dNQMDQy1VEwNFDQtOYCAGJHBm4%3D" target="_blank">Run Example</a>

```java
formatBaseN( 125, 10 );

```

Result: 125

### Format a float

Floors float to integer then formats with radix given

<a href="https://try.boxlang.io/?code=eJxLyy%2FKTSxxSixO9dNQMDTQMzfVUTBS0LTmAgBpTQai" target="_blank">Run Example</a>

```java
formatBaseN( 10.75, 2 );

```

Result: 1010

### Additional Examples


```java
<bx:output>
  #formatBaseN( 15, 2 )# <!--- 1111 (binary) ---> 
  #formatBaseN( 15, 16 )# <!--- f (hexadecimal) ---> 
  #formatBaseN( 15, 8 )# <!--- 17 (octal) ---> 
</bx:output>
```



```java
<bx:set max = CreateObject( "java", "java.lang.Integer" ).MAX_VALUE >
<bx:output>
  #formatBaseN( max, 16 )# <!--- 7fffffff (correct) ---> 
  #formatBaseN( max + 1, 16 )# <!--- 7fffffff (incorrect) ---> 
</bx:output>
```


