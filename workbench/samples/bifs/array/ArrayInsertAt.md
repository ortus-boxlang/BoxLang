### Insert an Item in an Array at Position 2

Inserts the number 4 at position 2

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWGuuRJCEZ15xalGJY4mGQjFMrY6CkY6CiYKmNVd5UWZJqn9pSUEpUN4r2N8vOLUoMzEnsyoVSbmCJkgpAHpvIAc%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
];
arrayInsertAt( someArray, 2, 4 );
writeOutput( JSONSerialize( someArray ) );

```

Result: [1,4,2,3]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFjTsOwjAMQGdyCisTSBUdGCuGjpwBMbjEQRbkI5Oo4vYkAbVsyMuT%2FfRsJXMaRfAFRziD2miM8UG6K3TnmRtM6Ms0DIL%2B9jm7QmEV1WVQWEMn%2FyRJY9qCXeIdHDrQnmbgdiUDnMhp2A3KZBd%2F3brre3DkJhKw2V8TB69WYc%2FLh1r9imjMv%2Bobm5pKjg%3D%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	"apple",
	"kiwi",
	"banana",
	"orange",
	"mango",
	"kiwi"
];
arrayInsertAt( fruitArray, 3, "new inserted item" );
dump( fruitArray );
// member function
fruitArray.insertAt( 3, "member added item" );
dump( fruitArray );

```


