### Get a value in a structure using structGet



<a href="https://try.boxlang.io/?code=eJyrULBVqFbg4oxUsFKo5uLkjALSFlyctVy11lzlRZklqS6luQUaCsUlRaXJJe6pJRoKShV6lXpVSgqaCprWXAD06RBu" target="_blank">Run Example</a>

```java
x = { 
	Y : {
		Z : 8
	}
};
writeDump( structGet( "x.y.z" ) );

```

Result: 8

### Accidentally Modifying a Structure

The structGet function will modify the variable x by adding a new structure x.a and also adds a key x.a.b to satisfy the path.

<a href="https://try.boxlang.io/?code=eJyrULBVqFbg4oxUsFKo5uLkjALSFlyctVy11lzlRZklqS6luQUaCsUlRaXJJe6pJRoKShV6iXpJSgqaCpooSipAfAAqMRVN" target="_blank">Run Example</a>

```java
x = { 
	Y : {
		Z : 8
	}
};
writeDump( structGet( "x.a.b" ) );
writeDump( x );

```

Result: 

### Accidentally Overwriting a variable using structGet

The value of x.y.z[2] will be set to an empty struct.

<a href="https://try.boxlang.io/?code=eJyrULBVqFbg4oxUsFKo5uLkjALS0UCa01AHRBqBSWMgEcvFWctVa81VXpRZkupSmlugoVBcUlSaXOKeWqKhoFShV6lXFW0Uq6SgqaCJoqoCxAcAVw8YYA%3D%3D" target="_blank">Run Example</a>

```java
x = { 
	Y : {
		Z : [
			1,
			2,
			3
		]
	}
};
writeDump( structGet( "x.y.z[2]" ) );
writeDump( x );

```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyVT01rhDAUPOuvGHLaBXHvLR5EpUhLKav0nmrUgBoxz23Lsv%2B9WTdV2uKhEAgz77354L3seKsR4AzXicIcdzi7jhNGefqa5mmSWcLJnpLkxQAaJ%2BFdiWReXmB8TJ8fLWHwxTXvcu8eDsga9Q7etuA3MzeeumGHlr%2BJNmDhOmAeTnwMLMJ%2Bvn4QhIITeEHyJEkKDdkvUrWgiFO4zgJkJkFB5mwHZtd8I%2BCvAsxKz8FGoaeWNFSF32I%2Fgx7XxdWCbVmwvW3zJ%2BHNO61AjcDAqUGpDN0rgviQmjwbyXw0jb02XSG6gT6hZ9tpFL5byb6MVb1Rt1T1Vkd7%2BK9qV7mlz7ezkf8Cp3K5ig%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	CAT : {
		ACTIVITIES : {
			SLEEP : true,
			EAT : true,
			DRINK : true
		}
	}
};
// Show all animals
Dump( label="All animals", var=animals );
// Get cat activities in animals
getCatActivities = StructGet( "animals.cat.activities" );
// Show results of getCatActivities
Dump( label="Results of StructGet(""animals.cat.activities"")", var=getCatActivities );
// If the path does not exist, result returns an empty structure.
findDog = StructGet( "animals.dog" );
// Show results of findDog
Dump( label="Results of StructGet(""animals.dog"")", var=findDog );

```


