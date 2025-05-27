### isNull Example

Returns true if the given object is null or the given expressions evaluates to null; Returns false is otherwise. 

<a href="https://try.boxlang.io/?code=eJzLLPYrzcnRUMhKLEt0Tiwu0VBQygMKKOkoKCkpaCpoWnMBAMSRCZs%3D" target="_blank">Run Example</a>

```java
isNull( javaCast( "null", "" ) );

```

Result: true

### Additional Examples


```java
v1 = "test";
writeDump( isnull( v1 ) ); // false
v2; // Defining empty variable or v2=nullValue();
writeDump( isnull( v2 ) );
 // true

```


