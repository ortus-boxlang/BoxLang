### Example using a simple numeric comparison

Take list and use List Filter to return items that are 3 and higher.

<a href="https://try.boxlang.io/?code=eJzLK81NSi3yySwuUbBVUDLUMdIx1jHRMdUxU%2BIqyShKTfUv8s0vSgXK5QCVuGXmlKQWaSjkwTXpKGgoOOZVKmSWpOYqaCrY2ilUc3EWpZaUFuVBxOxsFYy5ahU0ucqLgPyU0twCDQVkgzUBMSsorA%3D%3D" target="_blank">Run Example</a>

```java
numberList = "1,2,3,4,5,6"
threeOrMore = listFilter( numberList, ( Any item ) => {
	return item >= 3
} )
writedump( threeOrMore )

```

Result: A List with the values '3,4,5,6'

### Example using a member function

This is the same example as above, but using a member function on the list instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJzLK81NSi3yySwuUbBVUDLUMdIx1jHRMdUxU%2BIqyShKTfUv8s0vSgXK5cEV6uUACbfMnJLUIg0FDQXHvEqFzJLUXAVNBVs7hWouzqLUktKiPIiYna2CMVetgiZXeRGQn1KaW6ChgGyuJgAYPSiO" target="_blank">Run Example</a>

```java
numberList = "1,2,3,4,5,6"
threeOrMore = numberList.listFilter( ( Any item ) => {
	return item >= 3
} )
writedump( threeOrMore )

```

Result: A List with the values '3,4,5,6'

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNj8FqAzEMRM%2Brr1B92gXTfEEKhZJboLTQnN1GTg22tmjlbEM2%2F157SWhDLzmOZqTRS4cYBsUlmle3p2njlGR6jo512gkRG%2FAhltkLDTnWXI2v5lGLaV622OIjH5AiJeIiqwjbb%2Bxw%2BYBHaIJvLybelaaxlphiF68R0iyMKpmgOcFFexcHgpNFM5UkjBKUnnL6avHqnw4WC1xTeifBVeYPDT1DferNxQrVM1kde6ufBcb6Pov1YU8GhIYzTEne%2F4X6hTmT8PYfyYwx37wN45qgdnc%2FPqF6Ig%3D%3D" target="_blank">Run Example</a>

```java
mylist = "Save|Water|Plant|green"
filterResult = listFilter( mylist, ( Any element, Any idx ) => {
	if( element != "water" ) {
		return true
	}
	return false
}, "|" )
writeDump( filterResult )
// Member Function
listVal = "one,two,three,four,five"
res = listVal.listFilter( ( Any elem, Any ind ) => {
	if( elem != "three" ) {
		return true
	}
	return false
} )
writeDump( res )

```


