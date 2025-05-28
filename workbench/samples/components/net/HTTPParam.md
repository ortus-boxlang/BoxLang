### Script Syntax



<a href="https://try.boxlang.io/?code=eJwtjEEKwjAQRdfmFMOsdGGzlZbs3CvoBdJ20hYyJqYTo4h3t1JX%2F%2FF5vPZZjyIRmGQMvcHz6XJF6EabZhKDWdz%2BgJCTN%2Fjz5lrrUko1hDB4qrrAGiHRnP0ir4vwVpt2zUabLMPNMhm8I8grLuBCYjeR7xEe1ufl6Rx7bNRHlTQJHTPH7T8Ku0Z9Afh2N4k%3D" target="_blank">Run Example</a>

```java
bx:http method="POST" charset="utf-8" url="https://www.google.com/" result="result" {
	bx:httpparam name="q" type="formfield" value="bx";
}
writeDump( result );

```


### BX:HTTP Tag Syntax




```java
<bx:http result="result" method="POST" charset="utf-8" url="https://www.google.com/">
    <bx:httpparam name="q" type="formfield" value="bx">
</bx:http>
<bx:dump var="#result#">
```


