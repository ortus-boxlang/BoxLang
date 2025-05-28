### Tag Syntax




```java
<bx:set MyArray = arrayNew( 1 ) > 
 <!--- Resize that array to the number of records in the query. ---> 
 <bx:set temp = <!--- Transpiler workaround for BIF return type --->(( <bx:argument>, <bx:argument> ) => <bx:set arrayResize( arg1, arg2 ) ><bx:return true>)( MyArray, 8 ) > 
  <bx:dump var="#MyArray#"/>  
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhirXmSiwqSqwMSi3OrErVUMiDqNVRMDRQ0LTmcinNLYALggT09RVyU0E8hbTSvOSSzPw8LqisXhHUCENTrDoB9AgnmQ%3D%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
arrayResize( numbers, 10 );
Dump( numbers );
// member function
numbers.resize( 15 );
Dump( numbers );

```


