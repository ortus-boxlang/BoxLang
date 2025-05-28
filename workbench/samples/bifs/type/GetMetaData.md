### Dump Metadata of CFC Instance

CF9+


```java
writeDump( getMetadata( new Query() ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrVLBVKCxNLar0Sy3XUFDKTNHJS8xNVdJRUMorzU0tykzWKUssSs5ILFJS0LTmSinNLdBQcE8t8U0tSUxJLEnUUChU0ATJAABTTRZo" target="_blank">Run Example</a>

```java
q = queryNew( "id,name", "numeric,varchar" );
dump( GetMetadata( q ) );

```


