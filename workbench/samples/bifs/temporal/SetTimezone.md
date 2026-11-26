### Set the current Timezone to CET



<a href="https://try.boxlang.io/?code=eJwrTi0JycxNrcrPS9VQUHJ2DVFS0LTmKi%2FKLEnNLy0pKC3RUEhHKPHMS8vX0NQL8fR1jfL3cwUpBQDe0hVB" target="_blank">Run Example</a>

```java
setTimezone( "CET" )
writeoutput( getTimezoneInfo().TIMEZONE )

```

Result: CET

### Additional Examples


```java
<bx:dump var="#getTimeZone()#">
	<bx:set settimezone( "ART" ) >
	<bx:dump var="#getTimeZone()#">
```


### Set an IANA timezone

```java
setTimezone( "America/New_York" )
writeOutput( getTimezoneInfo().timezone )
```

Result: America/New_York

### Dates created afterwards use the timezone

10:00 New York time is 14:00 UTC (daylight time).

```java
setTimezone( "America/New_York" )
writeOutput( dateConvert( "local2Utc", createDateTime( 2024, 3, 15, 10, 0, 0 ) ) )
```

Result: {ts '2024-03-15 14:00:00'}
