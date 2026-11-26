### isinthread Example

Check if the code is running inside a bx:thread.

<a href="https://try.boxlang.io/?code=eJwrSi0sTS0u0fP0C%2FEIcnV0UbBVSEvMKU7lKi%2FKLEn1Ly0pKC3RUMgs9swLyShKTUzR0FTQ5EqqsCoB8xQSk0sy8%2FNslYpK85QU8hJzU22VMqEqlRSquTiLMI1HNourlqs4JzW1QEPB0MDAAGgyiq0YmjUB6Wg44w%3D%3D" target="_blank">Run Example</a>

```java
request.INTHREAD = false
writeOutput( isInThread() )
bx:thread action="run" name="inThread" {
	request.INTHREAD = isInThread()
}
sleep( 1000 )
writeOutput( request.INTHREAD )

```

Result: falsetrue

### Check from inside a thread created with threadNew()

```java
request.flag = false
threadNew( () => {
	request.flag = isInThread()
}, {}, "inThreadCheck" )
threadJoin( "inThreadCheck" )
writeOutput( request.flag )
```

Result: true

### Additional Examples

