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
	var tz = getTimeZoneInfo( "US/Hawaii", "de-DE" )
</bx:script>

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwTy0JycxNjcrPS%2FXMS8vX0FTQBAB0zAhw" target="_blank">Run Example</a>

```java
dump( GetTimeZoneInfo() )

```


### Inspect a specific timezone

Names are rendered with the JVM time zone names for the requested locale.

```java
info = getTimezoneInfo( "America/New_York", "en-US" )
writeOutput( info.id & " " & info.utcHourOffset & " " & info.offset & " " & info.shortName & " " & info.shortNameDST )
```

Result: America/New_York -5 -18000 EST EDT

### Offsets that are not whole hours

```java
info = getTimezoneInfo( "Asia/Kolkata", "en-US" )
writeOutput( info.utcHourOffset & ":" & info.utcMinuteOffset & " " & info.name )
```

Result: 5:30 India Standard Time

### Localized timezone name

```java
writeOutput( getTimezoneInfo( "US/Hawaii", "de-DE" ).name )
```

Result: Hawaii-Aleuten-Normalzeit
