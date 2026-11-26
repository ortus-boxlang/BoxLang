### Creates an ODBC time object

Uses the CreateODBCTime function to create an ODBC time object


```java
<bx:set time = createDateTime( 2012, 12, 12, 12, 12, 12 ) >
<bx:set result = createODBCTime( time ) >
<bx:output>#result#</bx:output>
```

Result: {t '12:12:12'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwLkpNLEn1d3FyDsnMTdVQyMsv19BUAEJrLgC6Vgm%2B" target="_blank">Run Example</a>

```java
dump( CreateODBCTime( now() ) )

```


### Create an ODBC datetime

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( createODBCDateTime( dt ) )
```

Result: {ts '2024-03-15 10:30:45'}

### Create an ODBC date

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( createODBCDate( dt ) )
```

Result: {d '2024-03-15'}

### Use the member function form

```java
writeOutput( createDateTime( 2024, 3, 15, 10, 30, 45 ).toODBCTime() )
```

Result: {t '10:30:45'}

### Parse a string and format it as ODBC

```java
writeOutput( createODBCDateTime( "2024-03-15 10:30:45" ) )
```

Result: {ts '2024-03-15 10:30:45'}

### An invalid date throws an error

```java
try {
	createODBCDateTime( "notadate" )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Can't cast [notadate] to a DateTime.
