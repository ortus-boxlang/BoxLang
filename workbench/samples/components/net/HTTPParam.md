### Script Syntax (CF11+)



<a href="https://try.boxlang.io/?code=eJwtjEEKwjAQRdfmFMOsdGGzlZbs3CvoBdJ20hYyJqYTo4h3t1JX%2F%2FF5vPZZjyIRmGQMvcHz6XJF6EabZhKDWdz%2BgJCTN%2Fjz5lrrUko1hDB4qrrAGiHRnP0ir4vwVpt2zUabLMPNMhm8I8grLuBCYjeR7xEe1ufl6Rx7bNRHlTQJHTPH7T8Ku0Z9Afh2N4k%3D" target="_blank">Run Example</a>

```java
bx:http method="POST" charset="utf-8" url="https://www.google.com/" result="result" {
	bx:httpparam name="q" type="formfield" value="cfml";
}
writeDump( result );

```

Result: 

### Alternate Script Syntax (CF9+). Removed in ColdFusion 2025.




```java
httpService = new http( method="POST", charset="utf-8", url="https://www.google.com/" );
httpService.addParam( name="q", type="formfield", value="cfml" );
result = httpService.send().getPrefix();
writeDump( result );

```

Result: 

### CFHTTP Tag Syntax




```java
<bx:http result="result" method="POST" charset="utf-8" url="https://www.google.com/">
    <bx:httpparam name="q" type="formfield" value="cfml">
</bx:http>
<bx:dump var="#result#">
```

Result: 

