### Set the current Timezone to CET



<a href="https://try.boxlang.io/?code=eJwrTi0JycxNrcrPS9VQUHJ2DVFS0LTmKi%2FKLEnNLy0pKC3RUEhHKPHMS8vX0NQL8fR1jfL3cwUpBQDe0hVB" target="_blank">Run Example</a>

```java
setTimezone( "CET" );
writeoutput( getTimezoneInfo().TIMEZONE );

```

Result: CET

### Additional Examples


```java
<bx:dump var="#getTimeZone()#">
	<bx:set settimezone( "ART" ) >
	<bx:dump var="#getTimeZone()#">
```


