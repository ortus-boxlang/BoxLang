### Is the current date in a leap year?



<a href="https://try.boxlang.io/?code=eJxLSSxJVbBVyMsv19C05sos9klNLIhMTSwCiiE4GgqVYDIFpFhTAaiwvCizJNW%2FtKSgtEQDSSFICgBT2BqY" target="_blank">Run Example</a>

```java
date = now();
isLeapYear = isLeapYear( year( date ) );
writeOutput( isLeapYear );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVUyMjC0UNJRKEsssvUs9klNLIhMTSzSUAAJK2gqaFpzlWPRYmSAVYuRAS4thljUG0IUAwDp1ytf" target="_blank">Run Example</a>

```java
writeDump( label="2018", var=IsLeapYear( 2018 ) );
writeDump( label="2020", var=IsLeapYear( 2020 ) );
writeDump( label="1", var=IsLeapYear( 1 ) );

```


