### Simple arrayReduce Example

Sum each `a` element in the array

<a href="https://try.boxlang.io/?code=eJxVjcEKwjAQRM%2FZr5hjikEsKIilQsC74FU8hLgHoUlDTNQi%2FXdjQYq3fTuzb23vQsevg0kGLc4g8SYhNHZYkxjVjPX2nzd1Ybo0dM%2BuXJoYzXDia7YsYWepgoT2A0Lkh5om7tixT6jQ7lFskVOOfipg8UuXuqFRYYWqoWe8JT7mFHKS%2BH4ruw8PZTLx" target="_blank">Run Example</a>

```java
complexData = [ 
	{
		A : 4
	},
	{
		A : 18
	},
	{
		A : 51
	}
];
sum = arrayReduce( complexData, ( Any prev, Any element ) => {
	return prev + element.A;
}, 0 );
writeOutput( sum );

```

Result: 73

### Additional Examples

<a href="https://try.boxlang.io?code=eJyVjs0KglAQhdfOUxzuwh8S1GonBr5C22hx0yGDm8akkUTv3uXqwkUtYjhwhrP4PuF6qLhGgVJEj3v3hjiAvCwmb22zsdnSMUaIsh1RaZExdvWhzcCIUOzwIk%2B4H6SddqymMad3jBRRTvVwvYWQGRflSBKMFzb1HVlK8kODPNWwMZ2yDqpvWNi1U%2Fc0uj2rf618KHv%2Bwk2pb3K0sAucABwcMzigD5k6WtY%3D" target="_blank">Run Example</a>

```java
reduced = ArrayReduce( [ 
	1,
	2,
	3,
	4
], ( Any carry, Any value ) => {
	return carry + value;
}, 0 );
dump( reduced ); // yields 10
reduced = ArrayReduce( [
	"hello",
	"there",
	"boxlang"
], ( Any carry, Any value ) => {
	return carry & " " & value;
}, "" );
dump( reduced );
 // yields 'hello there boxlang'

```


