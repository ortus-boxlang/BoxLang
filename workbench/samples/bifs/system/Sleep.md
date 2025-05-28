### Sleep for 5 seconds

Outputs the current date/time, sleeps for 5 seconds, then outputs the current date/time again.


```java
<bx:script>
	writeOutput( now() );
	writeOutput( "<br />" );
	sleep( 5000 );
	writeOutput( now() );
</bx:script>

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLqrAqycxNLVLISUxKzbFVCs5JTS1QSMsvUjBUKE5Nzs9LUVIoqSxItVXKzMvJzEtVUqjm4iwGKdJQMDQwMFDQtOaq5dLXV8DUaAVWkFvMVV6UWZLqX1pSUFqioaBkk1RkpwTSloTTaiM9U6gZxbhtNzKF2a6AYj2SZiuwIqALAK6GRnM%3D" target="_blank">Run Example</a>

```java
bx:timer label="Sleep for 1 second" type="inline" {
	sleep( 1000 );
}
// Sleep for 1 second: 1000ms
writeOutput( "<br>" );
bx:timer label="Sleep for 2.5 seconds" type="inline" {
	sleep( 2500 );
}
 // Sleep for 2.5 seconds: 2500ms

```


