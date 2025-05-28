### Script Syntax



<a href="https://try.boxlang.io/?code=eJwtjEEOwiAQAM%2Fyis2e9GC5mjbcNH7AD1DcFhMQCotojH8XU08zh8mMz94yR%2FDENlwVnk8XBGN1ysQKC0%2F7A0JJTuEvy72UtdZuDmF21JngJUKiXFyLVyK8xWZcr1En7eGuPSlcEPgVm7QZwkO70txM3uEgPqKmG9Ox%2BLj972A3iC9hPzS%2B" target="_blank">Run Example</a>

```java
bx:http method="GET" charset="utf-8" url="https://www.google.com/" result="result" {
	bx:httpparam name="q" type="url" value="bx";
}
writeDump( result );

```


### Alternate Script Syntax




```java
httpService = new http( method="GET", charset="utf-8", url="https://www.google.com/" );
httpService.addParam( name="q", type="url", value="bx" );
result = httpService.send().getPrefix();
writeDump( result );

```


### BX:HTTP Tag Syntax




```java
<bx:http result="result" method="GET" charset="utf-8" url="https://www.google.com/">
    <bx:httpparam name="q" type="url" value="bx">
</bx:http>
<bx:dump var="#result#">
```


