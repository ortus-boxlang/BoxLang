### Returns true if the object is a closure



<a href="https://try.boxlang.io/?code=eJwrLixNLEpVsFXQUHDMq1SoUNBUsLVTqObiLEotKS3KAwpoKVRw1XKVF2WWpLqU5hZoKGQWO%2BfkF5cWpWooFEN0aypoAgAkdhaB" target="_blank">Run Example</a>

```java
square = ( Any x ) => {
	return x * x
}
writeDump( isClosure( square ) )

```

Result: TRUE

### Returns false if the object is not a closure



<a href="https://try.boxlang.io/?code=eJwrLixNLEpVsFXQUHDMq1SoUNBUsLVTqObiLEotKS3KAwpoKVRw1XIVg9WlABVCWBoKpgqaXOVFmSWpLqW5BRoKmcXOOfnFpSAZmFpNBU0AcdwduA%3D%3D" target="_blank">Run Example</a>

```java
square = ( Any x ) => {
	return x * x
}
squared = square( 5 )
writeDump( isClosure( squared ) )

```

Result: FALSE

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjkEKwjAURNf%2BUwxdJSDkBHHT4g08QE1%2BIRBT%2Bcl3U7y7BUUK7XLgzcxzDiHPVYUpwMNY%2BAsWOgk3lYImyvQmcg5aWRB5SoUjJi2hpbnA3IarpX%2FUdWDXjvp4GrxG8an23zODAHtGHu%2Bcffcz6GCPWd2whxpr8wMLFkEN" target="_blank">Run Example</a>

```java
// closure
c = () => {
	return true
}

// user defined function (UDF)
function u() {
	return true
}
dump( var=isClosure( c ), label="closure" )
dump( var=isClosure( u ), label="user defined function" )

```


