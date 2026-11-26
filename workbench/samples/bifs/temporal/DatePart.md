### All dateparts

This example shows information available from datePart


```java
<bx:set todayDate = now() >
<h3>datePart Example</h3>
<p>Today's date is <bx:output>#todayDate#</bx:output>.
<p>Using datePart, we extract an integer representing the dateparts from that value <bx:output>
<ul>
<li>year: #datePart( "yyyy", todayDate )#</li>
<li>quarter: #datePart( "q", todayDate )#</li>
<li>month: #datePart( "m", todayDate )#</li>
<li>day of year: #datePart( "y", todayDate )#</li>
<li>day: #datePart( "d", todayDate )#</li>
<li>weekday: #datePart( "w", todayDate )#</li>
<li>week: #datePart( "ww", todayDate )#</li>
<li>hour: #datePart( "h", todayDate )#</li>
<li>minute: #datePart( "n", todayDate )#</li>
<li>second: #datePart( "s", todayDate )#</li>
</ul>
</bx:output>
```


### Additional Examples


```java
writeOutput( "Date for the current date is" & datePart( "d", now() ) )
d1 = CreateDate( 2016, 11, 10 ); // user defined date with member function
writeOutput( "<br>Month of the given date is " & datePart( "m", d1, "Asia/Calcutta" ) )

```


### Extract the year and quarter

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( datePart( "yyyy", dt ) & " " & datePart( "q", dt ) )
```

Result: 2024 1

### Day of year and minute

```java
dt = createDateTime( 2024, 3, 15, 10, 30, 45 )
writeOutput( datePart( "y", dt ) & " " & datePart( "n", dt ) )
```

Result: 75 30

### Apply a timezone first

10:30 UTC is 06:30 in New York (UTC-4), assuming the default timezone is UTC.

```java
writeOutput( datePart( "h", createDateTime( 2024, 3, 15, 10, 30, 45 ), "America/New_York" ) )
```

Result: 6

### An unsupported datepart throws an error

```java
try {
	datePart( "x", createDate( 2024, 3, 15 ) )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: The key [x] is not supported for the DatePart method
