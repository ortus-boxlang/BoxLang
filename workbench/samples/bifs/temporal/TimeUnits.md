### Year of a datetime object



<a href="https://try.boxlang.io/?code=eJxLKVGwVUguSk0sSU0B4pLM3FQNBSMDQzMdBUMwMtVRMDbQUTAyVdC05qoEKq5MTSzSUEgpAfHLizJLUvNLSwpKSzQUKkEiAIaoFhM%3D" target="_blank">Run Example</a>

```java
dt = createdatetime( 2016, 1, 1, 5, 30, 25 )
y = year( dt )
writeoutput( y )

```

Result: 2016

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrycgsjkxNLFKwVagEUhoKefnlGpoKmtZcKaW5BRoKJTB5oAgARtsNyA%3D%3D" target="_blank">Run Example</a>

```java
thisYear = year( now() )
dump( thisYear )

```


### Month and day names

Names use the request locale; results assume the default en-US locale and UTC timezone.

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( monthAsString( dt ) & " " & monthShortAsString( dt ) & " " & dayOfWeekAsString( dt ) & " " & dayOfWeekShortAsString( dt ) )
```

Result: March Mar Friday Fri

### Calendar calculations

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( quarter( dt ) & " " & dayOfYear( dt ) & " " & daysInMonth( dt ) & " " & daysInYear( dt ) )
```

Result: 1 75 31 366

### Use the member functions

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( dt.month() & " " & dt.hour() & " " & dt.minute() & " " & dt.second() )
```

Result: 3 10 30 45

### Apply a timezone before extracting the hour

10:30 UTC is 06:30 in New York (UTC-4).

```java
writeOutput( hour( createDateTime( 2024, 3, 15, 10, 30, 45 ), "America/New_York" ) )
```

Result: 6

### Locale-specific month name

```java
writeOutput( monthAsString( createDate( 2024, 3, 15 ), "UTC", "fr-FR" ) )
```

Result: mars

### Epoch based values

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( getTime( dt ) & " " & offset( dt ) )
```

Result: 1710498645000 +0000
