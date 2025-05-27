### Escapes the HTML characters



<a href="https://try.boxlang.io/?code=eJw1ikEOgkAQBO%2B%2BopcD0TdAvMmZgx8YZOJOWHaTnRZ9PmhiUqeqilzTbTYOpa7CM5p7NMeBgOpEC%2F5FLoxa0f8O6odfa%2FkIz6SYqjwWpV8xVt2svBzJsuItjilJXkIIDS7daQf35Ca6" target="_blank">Run Example</a>

```java
htmlEditFormat( "This is a test & this is another <This text is in angle brackets> Previous line was blank!!!" );

```

Result: This is a test &amp; this is another &lt;This text is in angle brackets&gt; Previous line was blank!!!

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrSS0uCS4pysxLV7BVUPJIzcnJV1BUVOSyCcnILFYoSa0oUQDSmXnFmSmpCvlpCol56TmpCklFicnZqSXFdlwoyvJLS7CrAxmpZM1VXpRZkgpUVFBaogHUBLdZE01KySbD1M4jxNfHNSWzxC2%2FKDexxEYfKKSkoKaAKoxqDMggADvORjk%3D" target="_blank">Run Example</a>

```java
testString = "Hello !!!
<This text is inside of angle brackets>
This text is outside of angle brackets !!!";
writeoutput( testString );
writeoutput( "<h5>HTMLEditFormat</h5>" & HTMLEditFormat( testString ) );

```


