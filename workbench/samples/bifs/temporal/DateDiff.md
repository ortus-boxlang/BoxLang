### dateDiff Example

Find the difference between two dates.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSEksSXXJTEvTUFBKUdJRUDIyMDTWNTDUNTRF5hmZKiloKmgCAATzDo0%3D" target="_blank">Run Example</a>

```java
writeOutput( dateDiff( "d", "2013-01-15", "2013-01-25" ) )

```

Result: 10

### How old are they?

Calculates a persons age based on a variable birthDate which contains a date. Uses the now function to get current date.

<a href="https://try.boxlang.io/?code=eJxLyiwqyXBJLElVsFVILkoFMkAcDQVDS3MjHQVTHQUjAwVNrsR0kHwKSDYzLU1DQakSCJR0FJJgunUU8vLLNTSBSsuLMktS80tLCkpLNBRA%2BjQB51gelg%3D%3D" target="_blank">Run Example</a>

```java
birthDate = createDate( 1972, 5, 20 )
age = dateDiff( "yyyy", birthDate, now() )
writeoutput( age )

```


### dateDiff member function


<a href="https://try.boxlang.io/?code=eJwrSS0ucUksSVWwVcjLL9fQ5ErJTEuDCqQAKceUFA0FpRQlHQVDHYUSmGJNrvKizJJU%2F9KSgtISDbi4HkgzVDncHE0FTQBzYx%2Fe" target="_blank">Run Example</a>

```java
testDate = now()
diffDate = dateAdd( "d", 1, testDate )
writeOutput( testDate.diff( "d", diffDate ) )
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLTc7I11BwSSxJdclMS9NQUEpR0lFQMjIwNNM1MNM1MkfhWSgpaCqoKSjZJBXZAZnWCvr6CoZcqaQYYY7FCAPSjDDDYoQuFmdkoJqhYGBgBUbowoZQYaL8RqyhBjgNxeJbDEMNSTUUm%2F%2FLUwi61Rx3AHCBjDUyAgCaOnOI" target="_blank">Run Example</a>

```java
echo( DateDiff( "d", "2016-06-27", "2016-06-28" ) & "<br>" ); // 1
echo( DateDiff( "d", "2016-06-27", "2016-06-27" ) & "<br>" ); // 0
echo( DateDiff( "d", "2016-06-27", "2016-06-26" ) & "<br>" ); // -1
echo( DateDiff( "h", "2016-06-27 00:00:00", "2016-06-27 01:00:00" ) & "<br>" ); // 1
echo( DateDiff( "h", "2016-06-27 00:00:00", "2016-06-27 00:00:00" ) & "<br>" ); // 0
echo( DateDiff( "h", "2016-06-27 01:00:00", "2016-06-27 00:00:00" ) & "<br>" ); // -1
echo( DateDiff( "wd", "2016-06-27 00:00:00", "2016-07-27 01:00:00" ) & "<br>" )
 // 22

```


### Days between two dates

January has 31 days and February 2024 has 29.

```java
writeOutput( dateDiff( "d", createDate( 2024, 1, 1 ), createDate( 2024, 3, 1 ) ) )
```

Result: 60

### Whole hours between two times

```java
writeOutput( dateDiff( "h", createDateTime( 2024, 1, 1, 8, 0, 0 ), createDateTime( 2024, 1, 1, 17, 30, 0 ) ) )
```

Result: 9

### A later date1 gives a negative result

```java
writeOutput( dateDiff( "d", createDate( 2024, 3, 1 ), createDate( 2024, 1, 1 ) ) )
```

Result: -60

### Use the member function

Both w and ww count whole weeks.

```java
writeOutput( createDate( 2024, 1, 1 ).diff( "ww", createDate( 2024, 2, 1 ) ) )
```

Result: 4

### An unsupported datepart throws an error

```java
try {
	dateDiff( "zz", createDate( 2024, 1, 1 ), createDate( 2024, 3, 1 ) )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: The datepart [zz] is not supported for the method DateDiff or the member function Date.diff
