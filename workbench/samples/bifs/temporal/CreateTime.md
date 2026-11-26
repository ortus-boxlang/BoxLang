### Tag Syntax




```java
<bx:set yourTime = createTime( "5", "24", "56" ) >  
 <bx:dump var="#yourTime#"/>   
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwLkpNLEkNycxN1VBQMjRS0lFQMjUHkcYGSgqaCprWXADtXgmX" target="_blank">Run Example</a>

```java
dump( CreateTime( "12", "57", "30" ) )

```


### Create a time

The date part is always the Unix epoch date, 1970-01-01.

```java
writeOutput( createTime( 14, 5, 9 ) )
```

Result: {ts '1970-01-01 14:05:09'}

### Minutes, seconds and milliseconds are optional

```java
writeOutput( dateTimeFormat( createTime( 9 ), "HH:mm:ss" ) )
```

Result: 09:00:00
