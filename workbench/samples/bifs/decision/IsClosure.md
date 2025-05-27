### Returns true if the object is a closure



<a href="https://try.boxlang.io/?code=eJwrLixNLEpVsFXQUHDMq1SoUNBUsLVTqObiLEotKS3KAwpoKVRYc9Vac5UXZZakupTmFmgoZBY75%2BQXlxalaigUQ%2FRrKmhacwEAeU4XPA%3D%3D" target="_blank">Run Example</a>

```java
square = ( Any x ) => {
	return x * x;
};
writeDump( isClosure( square ) );

```

Result: TRUE

### Returns false if the object is not a closure



<a href="https://try.boxlang.io/?code=eJwrLixNLEpVsFXQUHDMq1SoUNBUsLVTqObiLEotKS3KAwpoKVRYc9VacxWDVaYAlUJYGgqmCprWXOVFmSWpLqW5BRoKmcXOOfnFpSApmGJNkBIA%2BtYerg%3D%3D" target="_blank">Run Example</a>

```java
square = ( Any x ) => {
	return x * x;
};
squared = square( 5 );
writeDump( isClosure( squared ) );

```

Result: FALSE

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjkEKAjEQBM%2FOK5o9JSDkASFeFH%2FgA9ZkFgIxK5PMXsS%2FKyoiuMeG6u5yDrHMTYUpIsBYhB1utBHuKhVdlD3dPZFz0MaCxFOunDBpjT3PFeZ0OFr6Rn1O%2FPUp6eVqsIwSctu%2F%2Fwwi7BZlPHMJw0digPXrsP7AqyKv6gNk50I%2B" target="_blank">Run Example</a>

```java
// closure
c = () => {
	return true;
};

// user defined function (UDF)
function u() {
	return true;
}
dump( var=isClosure( c ), label="closure" );
dump( var=isClosure( u ), label="user defined function" );

```


