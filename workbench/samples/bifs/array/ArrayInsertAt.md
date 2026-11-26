### Insert an Item in an Array at Position 2

Inserts the number 4 at position 2

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWK5EkLhnXnFqUYljiYZCMUypjoKRjoKJgiZXeVFmSap%2FaUlBKVDaK9jfLzi1KDMxJ7MqFUm1gqaCJgAHuh9M" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
]
arrayInsertAt( someArray, 2, 4 )
writeOutput( JSONSerialize( someArray ) )

```

Result: [1,4,2,3]

### Insert using the member function

```java
letters = [ "a", "c" ]
letters.insertAt( 2, "b" )
writeOutput( letters.toList() )
```

Result: a,b,c

### Insert at the end of an array

Using a position one greater than the array size appends the value.

```java
numbers = [ 1, 2 ]
arrayInsertAt( numbers, 3, 9 )
writeOutput( numbers.toList() )
```

Result: 1,2,9

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx9jDEOwjAMRWdyCisTSBUdmBk6cgbE4BIHWRAnMokqbk8aUMuEvDz5fT2vhfOgii84whnMxmJKD7JdpTtP3GBEqdcwKsrto0OluA7NxeDcOcmTNA95C35pd3DowApNwM2SA84ULOyMKyH9Tuur7yFQGEnBF7lmjmJWv%2BelPze%2FQ3Tub%2FMNBa1JXQ%3D%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	"apple",
	"kiwi",
	"banana",
	"orange",
	"mango",
	"kiwi"
]
arrayInsertAt( fruitArray, 3, "new inserted item" )
dump( fruitArray )
// member function
fruitArray.insertAt( 3, "member added item" )
dump( fruitArray )

```


