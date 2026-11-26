### Simple arrayReduceRight Example

Demonstrate how the function works from right to left.

<a href="https://try.boxlang.io/?code=eJw9jsEKwjAQRM%2FNVww5SAr5g1Chv9CreKjNojkklCW1DeK%2FuyboZWZY3u5sLCPzXDDgAtXpWVvRW9Wlqtfq6lSi%2FcfNX5%2FIbwtN4f7IBrHdsDAYU8HK9LQ1JTpyS8EfLcg2egxnvFTHlDdOlcepwmJCOvW20Bq9UzuHLFVxNfi%2FIOMPjtg1uQ%3D%3D" target="_blank">Run Example</a>

```java
myArray = [ 
	"a",
	"b",
	"c",
	"d"
]
newArray = arrayReduceRight( myArray, ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx
}, "" )
writeOutput( newArray )

```

Result: d4c3b2a1

### Reduce from the right using the member function

The accumulator receives the last element first.

```java
letters = [ "a", "b", "c" ]
writeOutput( letters.reduceRight( ( acc, letter ) -> acc & letter, "" ) )
```

Result: cba

### Subtract from the right

```java
writeOutput( arrayReduceRight( [ 1, 2, 3 ], ( acc, n ) -> acc - n, 10 ) )
```

Result: 4
