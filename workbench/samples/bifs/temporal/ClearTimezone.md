### Clear the timezone

Set the timezone and then clear it.

<a href="https://try.boxlang.io/?code=eJwrTi0JycxNjcrPS9VQUHJ2DVFS0OQqL8osSfUvLSkoLdFQSIeoqAKq8MxLy9fQ1Avx9HWN8vdzVVBTUHrUNkkBpCU5JzWxCG4S8UZoAgAKNisq" target="_blank">Run Example</a>

```java
setTimeZone( "CET" )
writeOutput( getTimezoneInfo().TIMEZONE & "→ " )
clearTimeZone()
writeOutput( getTimezoneInfo().TIMEZONE )

```

Result: CET→ Etc/UTC

### Set a timezone for the request

Timezone examples assume the runtime default timezone is UTC.

```java
setTimezone( "America/New_York" )
writeOutput( getTimezoneInfo().timezone )
```

Result: America/New_York

### Clear it to return to the default

```java
setTimezone( "America/New_York" )
clearTimezone()
writeOutput( getTimezoneInfo().timezone == "America/New_York" )
```

Result: false
