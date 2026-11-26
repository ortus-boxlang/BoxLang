### Tag Example

 


```java
<bx:set dateTimeVar = dateTimeFormat( now(), "yyyy.MM.dd HH:nn:ss " ) > 
 <bx:output> 
 #parseDateTime( dateTimeVar )# 
 </bx:output> 
```


### Additional Examples


```java
datetime = dateTimeFormat( now(), "yyyy.MM.dd HH:nn:ss" )
dump( ParseDateTime( datetime ) )

```


### Parse a common date string

Results assume the runtime default timezone is UTC.

```java
writeOutput( parseDateTime( "2024-03-15 10:30:45" ) )
```

Result: {ts '2024-03-15 10:30:45'}

### Parse with an explicit mask

```java
writeOutput( parseDateTime( "03/15/2024 10:30 AM", "MM/dd/yyyy hh:mm a" ) )
```

Result: {ts '2024-03-15 10:30:00'}

### Use the string member function

```java
writeOutput( "15.03.2024".toDateTime( "dd.MM.yyyy" ) )
```

Result: {ts '2024-03-15 00:00:00'}

### Parse in a specific timezone

```java
dt = parseDateTime( "2024-03-15 10:30:45", "yyyy-MM-dd HH:mm:ss", "America/New_York" )
writeOutput( dateTimeFormat( dt, "epoch" ) )
```

Result: 1710513045

### A value that cannot be parsed throws an error

```java
try {
	parseDateTime( "garbage" )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Can't cast [garbage] to a DateTime.
