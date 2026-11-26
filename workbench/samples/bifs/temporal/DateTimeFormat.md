### Omitting the Mask

Should default mask to `hh:mm tt`

<a href="https://try.boxlang.io/?code=eJwrycxNdcsvyk0s0VBILkpNLEl1AeIQoKiGgpGBobmOgoWOgrGhjoKhqY6CERAbGipoKmhacwEADccPEg%3D%3D" target="_blank">Run Example</a>

```java
timeFormat( createDateTime( 2017, 8, 31, 15, 25, 11 ) )

```

Result: 03:25 pm

### Additional Examples

### Format with an explicit mask

Results assume the runtime default timezone is UTC.

```java
writeOutput( dateTimeFormat( createDateTime( 2024, 3, 15, 10, 30, 45 ), "yyyy-MM-dd HH:mm" ) )
```

Result: 2024-03-15 10:30

### Default dateTimeFormat and dateFormat masks

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( dateTimeFormat( dt ) & " | " & dateFormat( dt ) )
```

Result: 15-Mar-2024 10:30:45 | 15-Mar-24

### ISO and epoch masks

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( dateTimeFormat( dt, "ISO" ) & " " & dateTimeFormat( dt, "epoch" ) )
```

Result: 2024-03-15T10:30:45Z 1710498645

### Convert to a timezone while formatting

```java
writeOutput( dateTimeFormat( createDateTime( 2024, 3, 15, 10, 30, 45 ), "yyyy-MM-dd HH:mm", "America/New_York" ) )
```

Result: 2024-03-15 06:30

### Localize the output and use the member function

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( dt.format( "MMMM d, yyyy" ) & " | " & dateTimeFormat( dt, "MMMM d, yyyy", "UTC", "es-ES" ) )
```

Result: March 15, 2024 | marzo 15, 2024

### An unparseable date throws an error

```java
try {
	dateTimeFormat( "notadate" )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Can't cast [notadate] to a DateTime.
