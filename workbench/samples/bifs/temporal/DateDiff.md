### dateDiff Example

Find the difference between two dates.

<a href="https://try.boxlang.io/?code=eJxLSSxJdclMS9NQUEpR0lFQMjIwNNY1MNQ1NEXmGZkqKWhacwEA%2FP0JhQ%3D%3D" target="_blank">Run Example</a>

```java
dateDiff( "d", "2013-01-15", "2013-01-25" );

```

Result: 10

### How old are they?

Calculates a persons age based on a variable birthDate which contains a date. Uses the now function to get current date.

<a href="https://try.boxlang.io/?code=eJxLyiwqyXBJLElVsFVILkoFMkAcDQVDS3MjHQVTHQUjAwVNa67EdJCCFJB0ZlqahoJSJRAo6SgkwbTrKOTll2togtSWF2WWpOaXlhSUlmgogDQCxQBb5x9R" target="_blank">Run Example</a>

```java
birthDate = createDate( 1972, 5, 20 );
age = dateDiff( "yyyy", birthDate, now() );
writeoutput( age );

```


### dateDiff member function


<a href="https://try.boxlang.io/?code=eJw9jEsKg0AQRPeeonA1wiTiOrgICa6EnEHSLQ6EnjDTjdf3g7oqePWqlLO%2BB2W0kDi76lFQGMeD0BpPIoeSSo%2FGQ0979eYUlD%2Bmf1N3FfdtffjXUbXpqGvoFDISqyXJaBAFvX2ZPQYh3Hbwij%2FqLIcoxQLZ7DCY" target="_blank">Run Example</a>

```java
testDate = now();
diffDate = dateAdd( "d", 1, testDate );
writeOutput( testDate.diff( "d", diffDate ) );
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLTc7I11BwSSxJdclMS9NQUEpR0lFQMjIwNNM1MNM1MkfhWSgpaCqoKSjZJBXZAZnWCvr6CoZcqaQYYY7FCAPSjDDDYoQuFmdkoJqhYGBgBUbowoZQYaL8RqyhBjgNxeJbDEMNSTUUm%2F%2FLUwi61RxPAHCBzDUy4gIAgmpzzQ%3D%3D" target="_blank">Run Example</a>

```java
echo( DateDiff( "d", "2016-06-27", "2016-06-28" ) & "<br>" ); // 1
echo( DateDiff( "d", "2016-06-27", "2016-06-27" ) & "<br>" ); // 0
echo( DateDiff( "d", "2016-06-27", "2016-06-26" ) & "<br>" ); // -1
echo( DateDiff( "h", "2016-06-27 00:00:00", "2016-06-27 01:00:00" ) & "<br>" ); // 1
echo( DateDiff( "h", "2016-06-27 00:00:00", "2016-06-27 00:00:00" ) & "<br>" ); // 0
echo( DateDiff( "h", "2016-06-27 01:00:00", "2016-06-27 00:00:00" ) & "<br>" ); // -1
echo( DateDiff( "wd", "2016-06-27 00:00:00", "2016-07-27 01:00:00" ) & "<br>" );
 // 22

```


