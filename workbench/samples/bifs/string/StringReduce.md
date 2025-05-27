### Full function

Reduce the string to a single value.

<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBVUEpMSk5JTVOy5krOyS8uLUoFimkoOOZVKmTmFRjqwFhGCpoKtnYK1VycRaklpUV5YFkFNbCUNVetNVd5UWZJqn9pSUFpiYZCcElRZl56UGpKaXKqhkIOxDIdBagNOgpKVUpA8zStuQCLXirm" target="_blank">Run Example</a>

```java
letters = "abcdef";
closure = ( Any inp1, Any inp2 ) => {
	return inp1 & inp2;
};
writeOutput( StringReduce( letters, closure, "z" ) );

```

Result: zabcdef

### Member function

Reduce the string to a single value.


```java
letters = "abcdef";
closure = ( Any inp1, Any inp2 ) => {
	return inp1 & inp2;
};
writeOutput( letters.reduce( closure, "z" ) );

```

Result: zabcdef

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjcEKwjAQRM%2FuVww5SAq56DVU8AsE%2F6CmqxRClM2uYov%2FbsGAt2HmMS%2BzKktFDzdc0shXFynlezXhtfM4ljeeQzbehX%2Feo0N%2FwEIbYTUpjcC2zZE%2BkV4yKZ9MH6YeVWUqtzOPltgj%2F6QBzRTgZrd%2BdpG%2BNo4uXg%3D%3D" target="_blank">Run Example</a>

```java
letters = "abcdef";
closure = ( Any value1, Any value2 ) => {
	return value1 & value2;
};
writeOutput( stringReduce( letters, closure, "z" ) );

```


