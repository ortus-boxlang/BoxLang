### Output timezone information

This example shows the use of GetTimeZoneInfo


```java
<bx:output>
The local date and time are #now()#.
</bx:output>
<bx:set info = GetTimeZoneInfo() >
<bx:output>
<p>Total offset in seconds is #info.UTCTOTALOFFSET#.</p>
<p>Offset in hours is #info.UTCHOUROFFSET#.</p>
<p>Offset in minutes minus the offset in hours is #info.UTCMINUTEOFFSET#.</p>
<p>Is Daylight Savings Time in effect? #info.ISDSTON#.</p>
</bx:output>
```


### Get Hawaii timezone information in German

Shows the use of getTimeZoneInfo for a known / specific timezone with German locale. 


```java
<bx:script>
	var tz = getTimeZoneInfo( "US/Hawaii", "de-DE" );
</bx:script>

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwTy0JycxNjcrPS%2FXMS8vX0FTQtOYCAIYsCLU%3D" target="_blank">Run Example</a>

```java
dump( GetTimeZoneInfo() );

```


