### Simple listIndexExists

Check whether the index is exists or not in list


```java
<bx:set list = "Apple,Orange,Banana,Graphs" >
<bx:if listIndexExists( list, 2 ) >
	<bx:set list = listsetAt( list, 2, "Goa" ) >
</bx:if>
<bx:output>#list#</bx:output>
```

Result: Apple,Goa,Banana,Graphs

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljbEKwjAYhPc8xZGphR8zCC7FQbBCobr0CWr9hUCblOSP9vGNVienO4777oxBZ6d5ZNRL%2F1b1DFbYJ5mTFBhtlMbdeKmX7GIB3aVoqT10RGMamIlIOIombFGirJQxOPN05YBTcoNY71SU0GYae2hqMucf%2FIN1tf4d0zQX%2BBY3f6%2B7dRt5%2FOIF%2FIkJgSUFF3Hvx8jqBcsbQqE%3D" target="_blank">Run Example</a>

```java
// Simple Example
writeoutput( listIndexExists( "Susi,LAS,,lucee,,,test", 3 ) );
// Member Function
strList = ",I,,love,lucee,,";
writeDump( strList.listIndexExists( 6 ) );
 // Not exists, returns false

```


