### Clear the timezone

Set the timezone and then clear it.

<a href="https://try.boxlang.io/?code=eJwrTi0JycxNjcrPS9VQUHJ2DVFS0LTmKi%2FKLEn1Ly0pKC3RUEiHKKkCKvHMS8vX0NQL8fR1jfL3c1VQU1B61DZJAawnOSc1sQhuFgmGAJUCAM45LCA%3D" target="_blank">Run Example</a>

```java
setTimeZone( "CET" );
writeOutput( getTimezoneInfo().TIMEZONE & "→ " );
clearTimeZone();
writeOutput( getTimezoneInfo().TIMEZONE );

```

Result: CET→ Etc/UTC

