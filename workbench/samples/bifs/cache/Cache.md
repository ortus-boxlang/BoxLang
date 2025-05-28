### Adding a page to the cache

Puts HTML page into the cache and uses the cached version on subsequent calls to the page.


```java
<bx:cache action="optimal" directory="/path/to/directory" timespan="#createTimeSpan( 1, 0, 0, 0 )#" idletime="#createTimeSpan( 0, 12, 0, 0 )#">
	<div id="some-id">Hello World!</div>
</bx:cache>
```

Result: <div id="some-id">Hello World!</div>

### Flushing a page from the cache

Flushes the 'hello-world.bxm' page from the cache.


```java
<bx:cache action="flush" directory="/path/to/directory" expireURL="/hello-world.bxm"/>
```


