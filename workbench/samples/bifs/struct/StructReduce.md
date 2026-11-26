### Script Syntax



<a href="https://try.boxlang.io/?code=eJxVjsEKwjAQRM%2FmK5YUpIWCd6kBQfDgQfCgeIxm0dCalDVrEfHfTVIRvM0%2BZmaHtHUnP8ACXiAmcodGwhzk4YrkZR3JlrS7YIYbTbrVmR6x6%2FwwUj9ctc10TYjua2012dZK8RZsY%2Fs9EJ9DrOczlkDj1xpKWLon9IQP6%2Fm%2B1x1jnVGLz1E8EoMKFgpeYkIYmNx%2FAKYgGxNUETNFM4uqMUYVOZhuo%2BKKOnk6JWNTss%2BSFgPZgCu%2B9b9FUI1wy6HnUELcXn0AlxBY5g%3D%3D" target="_blank">Run Example</a>

```java
rainbow = { 
	"Red" : "Whero",
	"Orange" : "Karaka",
	"Yellow" : "Kowhai",
	"Green" : "Kakariki"
}
ui = structReduce( rainbow, ( Any previousValue, Any key, Any value ) => {
	return previousValue & "<dt>#key#</dt><dd>#value#</dd>"
}, "<dl>" ) & "</dl>"
writeDump( rainbow )
writeOutput( ui )

```

Result: an HTML definition list (<dl>...</dl>) with one dt/dd pair per entry; the entry order is not guaranteed for a default struct


### Using Member Function

 

<a href="https://try.boxlang.io/?code=eJxVjsEKwjAQRM%2FmK5YUpIVi71IDguDBQ8GD4jGaRUNrUtbEIsV%2FN0n14G32MTM7JLU52wFWMAKb8T0qDkvgxxuS5WUgDUlzxQR3kmQrEz1h19lhona4SZ3olhDN19pK0q3m7M28Du00%2FVkQKn%2FBHHJYmxf0hE9t%2FeMgO49lQi2%2BJvGMDApYCRjZjNB5Mv8BmAOvlRNZyGR1FVStlMhSMN5KhPdl9HSCh6Zor6JmA2mHG3%2Fv898wKCbYeNd7l0MYXXwA53RWQw%3D%3D" target="_blank">Run Example</a>

```java
rainbow = { 
	"Red" : "Whero",
	"Orange" : "Karaka",
	"Yellow" : "Kowhai",
	"Green" : "Kakariki"
}
ui = rainbow.reduce( ( Any previousValue, Any key, Any value ) => {
	return previousValue & "<dt>#key#</dt><dd>#value#</dd>"
}, "<dl>" ) & "</dl>"
writeDump( rainbow )
writeOutput( ui )

```

Result: an HTML definition list (<dl>...</dl>) with one dt/dd pair per entry; the entry order is not guaranteed for a default struct


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1kNFrgzAQxp%2FNX3HkYShIfR9TkCJDVtrRtBvrW6a3VRrNiKalK%2F7vS6LdcGNv3%2BX73eW%2B401Vc9FCDBcg3nz1DLdwIZ63XOUsM5rWUtLQPLB852rB1TtS4vXm8TG%2F%2F43LqjlM%2BBrLStdjwzzd%2FJmP8jRpaM0%2BwvKkJ6WuP3wQ%2FBVFTFMhgA%2Fr0hCOXMVjBQEZVN68SZOEdUoX3RpLXaB%2FbQnBh7Q5g8JWiy50%2BoDnQRy50AgBxIldTmGnVQMmqK6x6drZOmPbxQZugN6JKqFG%2FHgP2YsztEisuZRViybFhHlKF9tsNkS2bGRAC7Pq8x%2FWHeMbjexwqyjpw%2BEvarZ1vtUkioDt5WkMR7DYy2twd5PgCwP3i2Q%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : {
		NOISE : "moo",
		SIZE : "large"
	},
	PIG : {
		NOISE : "oink",
		SIZE : "medium"
	},
	CAT : {
		NOISE : "meow",
		SIZE : "small"
	}
}
dump( label="All animals", var=animals )
animalInfo = StructReduce( animals, ( Any result, Any key, Any value ) => {
	return arguments.RESULT & "<li>" & arguments.KEY & "<ul><li>Noise: " & arguments.VALUE.NOISE & "</li><li>Size: " & arguments.VALUE.SIZE & "</li></ul></li>"
}, "<ul>" ) & "</ul>"
// Show result
echo( animalInfo )

```

Result: an HTML unordered list with a nested list of Noise and Size for each of COW, PIG and CAT

### Sum the values of a struct

```java
totals = { a : 1, b : 2, c : 3 }
sum = structReduce( totals, ( acc, k, v ) -> acc + v, 0 )
writeOutput( sum )
```

Result: 6
