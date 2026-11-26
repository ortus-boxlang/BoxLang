### Converting Local to UTC



<a href="https://try.boxlang.io/?code=eJwrLUmOT0ksSS3JzE1VsFUAMZ3z88pSi0o0FJRy8pMTc4xCS5KVdBTy8ss1NBU0AatnECQ%3D" target="_blank">Run Example</a>

```java
utc_datetime = dateConvert( "local2Utc", now() )

```

Result: {ts '2025-05-27 05:12:10'}

### Converting UTC to Local

This example makes sense only if your server time is UTC. now() uses your server settings when creating a datetime object.

<a href="https://try.boxlang.io/?code=eJzLyU9OzIlPSSxJLcnMTVWwVQAxnfPzylKLSjQUlEpLko18QEqUdBTy8ss1NBU0AdBBEOM%3D" target="_blank">Run Example</a>

```java
local_datetime = dateConvert( "utc2Local", now() )

```

Result: {ts '2025-05-26 22:12:10'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUHBJLEl1zs8rSy0q0VBQ8slPTswxKi1JVtJRUKouKVZQNzIwMNM1MNQ1MlMwMLIyMAAi9VolBU0FTa5yXKYA9RuBTQKakpdfrqEJUg4AIdUiWA%3D%3D" target="_blank">Run Example</a>

```java
writeDump( DateConvert( "Local2utc", "{ts '2006-01-26 02:00:00'}" ) )
writeDump( DateConvert( "utc2Local", now() ) )

```


### Convert local time to UTC

10:00 in New York is 14:00 UTC on this date (daylight time, UTC-4).

```java
setTimezone( "America/New_York" )
writeOutput( dateConvert( "local2Utc", createDateTime( 2024, 3, 15, 10, 0, 0 ) ) )
```

Result: {ts '2024-03-15 14:00:00'}

### Convert a UTC string to local time

```java
setTimezone( "America/New_York" )
writeOutput( dateConvert( "utc2Local", "2024-03-15 10:00:00" ) )
```

Result: {ts '2024-03-15 06:00:00'}

### An invalid conversion type throws an error

```java
try {
	dateConvert( "bad", createDateTime( 2024, 3, 15, 10, 0, 0 ) )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Input [conversionType] for component [dateConvert] must be one of the following values: local2utc, utc2Local
