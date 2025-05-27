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

Result: 

### Additional Examples


```java
writeOutput( "Date for the current date is" & datePart( "d", now() ) );
d1 = CreateDate( 2016, 11, 10 ); // user defined date with member function
writeOutput( "<br>Month of the given date is " & d1.Part( "m", "Asia/Calcutta" ) );

```


