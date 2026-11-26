### Set the day of the month

Results assume the runtime default timezone is UTC.

```java
d = createDate( 2024, 3, 15 )
writeOutput( dateSetDay( d, 1 ) )
```

Result: {ts '2024-03-01 00:00:00'}

### Set the month using the member function

```java
writeOutput( createDate( 2024, 3, 15 ).setMonth( 12 ) )
```

Result: {ts '2024-12-15 00:00:00'}

### Set the time parts

```java
writeOutput( dateSetHour( createDate( 2024, 3, 15 ), 13 ) )
```

Result: {ts '2024-03-15 13:00:00'}

### Set the year

```java
writeOutput( dateSetYear( createDate( 2024, 3, 15 ), 2030 ) )
```

Result: {ts '2030-03-15 00:00:00'}

### Out-of-range values roll over, zero clamps

Setting a day beyond the end of the month advances into the following months. A day of zero or less becomes day 1.

```java
writeOutput( createDate( 2024, 3, 15 ).setDay( 50 ) & " " & createDate( 2024, 3, 15 ).setDay( 0 ) )
```

Result: {ts '2024-04-19 00:00:00'} {ts '2024-03-01 00:00:00'}

### The date object is modified in place

```java
d = createDate( 2024, 3, 15 )
d.setYear( 2000 )
writeOutput( d )
```

Result: {ts '2000-03-15 00:00:00'}
