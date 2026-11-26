### Simple numeric comparison

Take an array of struct items and use arrayFilter to return ones of a rating 3 and higher.

<a href="https://try.boxlang.io/?code=eJxt0E0LgkAQBuCz%2ByuGPbkgQV%2BXQMGLUQeD7BYdNtqVJV1lWhML%2F3u7HYJU5vby8A4zEhtlYkTeQQhnIN6beB6VLqWwAcrruhA0cCFyo3Tu0hXx%2BmBEr1zbGdr5pK2Q63zUu560paXVkC4m6V21aiiXVpILkfxZoTIicfhhj%2BXu6EQVRqAP8veGAHyIdQeWlsAgjMDuQGEa1N9sdoxPu3QLUWibe2Ckda23pqx92GeHNBOoeKFewpb%2Br2TAPvHAZgU%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		"fruit" : "apple",
		"rating" : 4
	},
	{
		"fruit" : "banana",
		"rating" : 1
	},
	{
		"fruit" : "orange",
		"rating" : 5
	},
	{
		"fruit" : "mango",
		"rating" : 2
	},
	{
		"fruit" : "kiwi",
		"rating" : 3
	}
]
favoriteFruits = arrayFilter( fruitArray, ( Any item ) => {
	return item.RATING >= 3
} )
writedump( JSONSerialize( favoriteFruits ) )

```

Result: [{"fruit":"apple","rating":4},{"fruit":"orange","rating":5},{"fruit":"kiwi","rating":3}]

### Using a member function

This is the same example as above, but using a member function on the array instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxtz70KwjAUBeA5eYpLpwak4N8itNBF0aGCdROHiEkJtmm5ppYqfXcTB4e03O3wcS5HYqtMish7iOEClHwoIYF0aQAbCHjTlCKYuRC5Ubpw6YqSYTaiN67t%2BXY%2BaWvkuhj1ridtZWnt08UkfahO%2BXJpJb1SyV81KiO2Dj%2FtWPlfHklVGoEhhJDqHiyqgEGcgG1HYVrUvyw6ped9toMktp0DMNq5vntbNSEc8mOWC1S8VG8RgveMAfsCf6Jj6A%3D%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		"fruit" : "apple",
		"rating" : 4
	},
	{
		"fruit" : "banana",
		"rating" : 1
	},
	{
		"fruit" : "orange",
		"rating" : 5
	},
	{
		"fruit" : "mango",
		"rating" : 2
	},
	{
		"fruit" : "kiwi",
		"rating" : 3
	}
]
favoriteFruits = fruitArray.filter( ( Any item ) => {
	return item.RATING >= 3
} )
writedump( JSONSerialize( favoriteFruits ) )

```

Result: [{"fruit":"apple","rating":4},{"fruit":"orange","rating":5},{"fruit":"kiwi","rating":3}]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtz08LgkAQh%2BHz7qcYPLkgQf8ugYIXw0sHsVN02GiNJV1l2i0k%2FO6NEcGKzG14eOFXodM2RZQ9xHACzt6csaw45iXsIJBdV6sgoleRlvlhT78NZ0M0YRdp6Hy3nHEtSnOb9LYzriHW%2Bmw1w%2B76pX21JsXPvJLPFrVV2TjuQcPkODDTtVUYQvWfHEEIqemBaAMC4gSoj8o6NN%2Ff4hdOYioPIPjVNR0F%2FLz4ACnrVN8%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		FRUIT : "apple",
		RATING : 4
	},
	{
		FRUIT : "banana",
		RATING : 1
	},
	{
		FRUIT : "orange",
		RATING : 5
	},
	{
		FRUIT : "mango",
		RATING : 2
	},
	{
		FRUIT : "kiwi",
		RATING : 3
	}
]
favoriteFruits = arrayFilter( fruitArray, ( Any item ) => {
	return item.RATING >= 3
} )
dump( favoriteFruits )

```


<a href="https://try.boxlang.io/?code=eJxtz08LgkAQh%2BHz7qcYPCmI0L9LoODF8NJB7BQdNtqNJXddprWQ8Ls3RQSKzO3HwwujsNM%2BRxQ9pHAEzl6csaI6lDVsIRDONTKIaaryutzvaFtzNsQTdhaWbuwWM65FYa%2BT3mbGGWLtmC1n2E0%2F9VitSPETV%2BLRovay%2BDx3p8fU%2F8tE6cZLDCGE3PZAyEAEaQZURuk7tN8t%2BSWzlJoDRPzSGRfCJBy9AR97UsI%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		FRUIT : "apple",
		RATING : 4
	},
	{
		FRUIT : "banana",
		RATING : 1
	},
	{
		FRUIT : "orange",
		RATING : 5
	},
	{
		FRUIT : "mango",
		RATING : 2
	},
	{
		FRUIT : "kiwi",
		RATING : 3
	}
]
favoriteFruits = fruitArray.filter( ( Any item ) => {
	return item.RATING >= 3
} )
dump( favoriteFruits )

```


