### Example using a simple numeric comparison

Take a struct of items with their rating and use structFilter to return ones of a rating 3 and higher.

<a href="https://try.boxlang.io/?code=eJxVjU0LgkAURdfOr7hLBTd9bQKFETKkUnHTWmqMIbMY3xgi%2FffeRIvibc69D85tjNVU1aS7S48IE4Qny3K%2FwRrLUHiJzPk4zDgUlcy37rPicGAumOfMu%2ByYMS7ESzT1cDeaVOq8ztiTsSdKdUvK%2BGh%2B5kL4kN2IqxrDDwx1axUCRDEm4RlF1nTfMo6cHYF4OvnZ3h7s%2Bp8K3qiqPJw%3D" target="_blank">Run Example</a>

```java
fruitRatings = { 
	APPLE : 4,
	BANANA : 1,
	ORANGE : 5,
	MANGO : 2,
	KIWI : 3
}
favoriteFruits = structFilter( fruitRatings, ( Any key, Any value ) => {
	return value >= 3
} )
writedump( favoriteFruits )

```

Result: {apple=4,orange=5,kiwi=3}

### Example using a member function

This is the same example, but using a member function on the struct instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxLKyrNLAlKLMnMSy9WsFWoVuDidAwI8HFVsFIw0eHidHL0A0IgxxDI8Q9y9HMHyZgCOb5Atj%2BQbQRke3uGewKZxly1XGmJZflFmSWpbiBzQSamIVmgl5aZU5JapKGgoeCYV6mQnVqpA2aUJeaUpipoKtjaKVRzcRallpQW5UEF7WxBxipocpWDTE0pzS3QUECzQxMA6KA5%2BQ%3D%3D" target="_blank">Run Example</a>

```java
fruitRatings = { 
	APPLE : 4,
	BANANA : 1,
	ORANGE : 5,
	MANGO : 2,
	KIWI : 3
}
favoriteFruits = fruitRatings.filter( ( Any key, Any value ) => {
	return value >= 3
} )
writedump( favoriteFruits )

```

Result: {apple=4,orange=5,kiwi=3}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kE1LxDAQhs%2FJr3jtqYXi3oUKxY%2BluKxKDyLiIcrUhqappolSlv5307hdLXjdy8CbYZ7JPELLVqgeGXbg7OL2AWeI2q6LUs7uivWUOqmbKZbbvNhMDxEf%2BWqFsu6%2BIJSC%2BGHwS9e%2Bx1DihVQW5b%2BNKMWnMNk%2BIZmG12TnNmwtLFrREHQne%2BJTHfLDx0pr3Ku9lsqSieehFDFyPaChAQmyc%2Bw489ii8jQKr7XoIfxi5QiGrDMaHkSIA37PSTiTVYwN6QP5CcK8uZa07U9vrh7x7PnJhGd%2FKJyNfM6VHyI%2B%2BrsWArZhTb5QsLjsfw%2FCED6cJMtDPZ6GgF9qODmGh%2FuwZ%2BlhcVryDS9bzjk%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	SNAIL : ""
}
// Show all animals
Dump( label="All animals", var=animals )
// Get animals that make noise
noisyAnimals = StructFilter( animals, ( Any key ) => {
	// If the key has a value return true (noisy animal)
	if( Len( animals[ arguments.KEY ] ) ) {
		return true
	}
	return false
} )
Dump( label="Noisy Animals", var=noisyAnimals )
// Get animals that are quiet
quietAnimals = StructFilter( animals, ( Any key ) => {
	// If the key has a value return true (quiet animal)
	if( !Len( animals[ arguments.KEY ] ) ) {
		return true
	}
	return false
} )
Dump( label="Quiet Animals", var=quietAnimals )

```

Result: "Noisy Animals" dumps COW and PIG; "Quiet Animals" dumps SNAIL


