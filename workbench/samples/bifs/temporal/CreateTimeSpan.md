### Use of createTimespan in a bx:query

The createTimespan function is useful in the cachedwithin attribute of bx:query.


```java
<bx:query name="GetParks" datasource="cfdocexamples" cachedWithin="#createTimespan( 0, 6, 0, 0 )#"> 
 SELECT PARKNAME, REGION, STATE 
 FROM Parks 
 ORDER by ParkName, State 
 </bx:query>
```


### The createTimespan function returns a numeric value

Passing 6 hours, or a quarter of a day returns a double representing 1/4

<a href="https://try.boxlang.io/?code=eJxLLkpNLEkNycxNLS5IzNNQMNBRMNMBkQYKmtZcAJu1CDY%3D" target="_blank">Run Example</a>

```java
createTimespan( 0, 6, 0, 0 )

```

Result: PT6H

### Adding a date and a timestamp

Instead of using dateAdd you could add a timestamp to a date object


```java
dateFormat( dateAdd( "s", createTimespan( 2, 0, 0, 0 ).toSeconds(), createDate( 2017, 1, 1 ) ) )

```

Result: 03-Jan-17

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBILkpNLEkNycxNDS5IzNNQMNABIWMQpaCpoGnNpaCvD%2BTq5mbmlZakKpQAFRYDFXIBAD0DEdU%3D" target="_blank">Run Example</a>

```java
dump( createTimeSpan( 0, 0, 30, 0 ) )
 // 30-minute timespan

```


### A timespan is a Duration

```java
writeOutput( createTimeSpan( 0, 6, 30, 0 ).toMinutes() )
```

Result: 390

### Convert a timespan to seconds

```java
writeOutput( createTimeSpan( 0, 0, 2, 30 ).toSeconds() )
```

Result: 150

### Fractional days are converted to smaller units

```java
writeOutput( createTimeSpan( 0.5, 0, 0, 0 ).toHours() )
```

Result: 12
