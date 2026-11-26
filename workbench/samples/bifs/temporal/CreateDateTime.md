### Tag Syntax




```java
<bx:set yourDate = createDateTime( "2015", "12", "09", "6", "20", "34" ) >
<bx:dump var="#yourDate#"/>     
```


### Additional Examples

### Create a datetime from its parts

Results assume the runtime default timezone is UTC.

```java
writeOutput( createDateTime( 2024, 3, 15, 10, 30, 45 ) )
```

Result: {ts '2024-03-15 10:30:45'}

### Use the createDate alias with default time parts

```java
writeOutput( createDate( 2024, 3, 15 ) )
```

Result: {ts '2024-03-15 00:00:00'}

### Create a datetime in a specific timezone

10:30 in New York (UTC-4 on this date) is 14:30 UTC, which is epoch second 1710513000.

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 0, 0, "America/New_York" )
writeOutput( dateTimeFormat( dt, "epoch" ) )
```

Result: 1710513000

### A zero year, month or day is rejected

```java
try {
	createDateTime( 0, 1, 1 )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: The year argument passed to this method must be greater than zero
