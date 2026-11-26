### Check if date is a date OBJECT, not just a date string

```java
<bx:script>
	date = IsDateObject( now() )
	writeOutput( "Can string be converted to a date/time value : " & date )
</bx:script>
```

Result: Can string be converted to a date/time value : YES

### Date strings and timespans are not date objects

```java
writeOutput( isDateObject( "2021-01-01" ) & " " & isDateObject( createTimespan( 1, 0, 0, 0 ) ) )
```

Result: false false

### Parse a string first to get a date object

```java
writeOutput( isDateObject( parseDateTime( "2021-01-01" ) ) )
```

Result: true
