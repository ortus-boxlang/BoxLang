### Converting Local to UTC



<a href="https://try.boxlang.io/?code=eJwrLUmOT0ksSS3JzE1VsFUAMZ3z88pSi0o0FJRy8pMTc4xCS5KVdBTy8ss1NBU0rbkAzC8QaQ%3D%3D" target="_blank">Run Example</a>

```java
utc_datetime = dateConvert( "local2Utc", now() );

```

Result: {ts '2025-05-27 05:12:10'}

### Converting UTC to Local

This example makes sense only if your server time is UTC. now() uses your server settings when creating a datetime object.

<a href="https://try.boxlang.io/?code=eJzLyU9OzIlPSSxJLcnMTVWwVQAxnfPzylKLSjQUlEpLko18QEqUdBTy8ss1NBU0rbkA8ocRKA%3D%3D" target="_blank">Run Example</a>

```java
local_datetime = dateConvert( "utc2Local", now() );

```

Result: {ts '2025-05-26 22:12:10'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUHBJLEl1zs8rSy0q0VBQ8slPTswxKi1JVtJRUKouKVZQNzIwMNM1MNQ1MlMwMLIyMAAi9VolBU0FTWuuclzGAA0wAhsFNCYvv1xDE6IeAIX5Itg%3D" target="_blank">Run Example</a>

```java
writeDump( DateConvert( "Local2utc", "{ts '2006-01-26 02:00:00'}" ) );
writeDump( DateConvert( "utc2Local", now() ) );

```


