### Is the current date in a leap year?



<a href="https://try.boxlang.io/?code=eJxLSSxJVbBVyMsv19Dkyiz2SU0siExNLAIKITgaCpVgMgWkVlNBk6u8KLMk1b%2B0pKC0RANJnYImAPRmGd0%3D" target="_blank">Run Example</a>

```java
date = now()
isLeapYear = isLeapYear( year( date ) )
writeOutput( isLeapYear )

```

Result: true when the current year is a leap year, otherwise false


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVUyMjC0UNJRKEsssvUs9klNLIhMTSzSUAAJK2gqaHKVY9FhZIBVh5EBDh2GWJQbgtQCAEbVKqQ%3D" target="_blank">Run Example</a>

```java
writeDump( label="2018", var=IsLeapYear( 2018 ) )
writeDump( label="2020", var=IsLeapYear( 2020 ) )
writeDump( label="1", var=IsLeapYear( 1 ) )

```

Result: false, true, false (for 2018, 2020 and 1)
