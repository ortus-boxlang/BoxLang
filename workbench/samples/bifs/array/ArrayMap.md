### Script Syntax



<a href="https://try.boxlang.io/?code=eJxLzs8tyEmtcEksSVSwVYhW4OKs5uLkdFSwUjDh4qzVQXANLVD5poZAPlcsV15quWNRUWIlUHciiPZNLNBQSEaYqqOgoeCYV6mQWZKaq6CpYGunADShKLWktCgPLKbnyFWroMlVXgTk%2BJeWFJSWaEAMCsn3ySwGcuAWaCpoAgBinDFN" target="_blank">Run Example</a>

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
]
newArray = arrayMap( complexData, ( Any item ) => {
	return item.A
} )
writeOutput( arrayToList( newArray ) )

```

Result: 4,18,51

### Using the member function

The callback can use a lambda since it only uses its own argument.

```java
numbers = [ 1, 2, 3 ]
doubled = numbers.map( ( n ) -> n * 2 )
writeOutput( doubled.toList() )
```

Result: 2,4,6

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1jrEKwjAQhufcUxyZGigWOwoVXIW6OIrD2V6xQ66SNmiRvrtpAvoEbh%2Fc%2Ff%2F%2F0Yksj1jhBUHpmlzjR50HPJOje6TjMHLXC2u4QuvtI0NKIQPCz4jbUEDO0VzT95xjhgeZsZ%2FY5omk5VfC8IwGqz2%2BQTmevJOVlJaQ1LiLIVALLGEkbf6mDBQFWrY3dth5aaZ%2BkK9JuZpE2tjV5S8OJZoP%2FWldng%3D%3D" target="_blank">Run Example</a>

```java
aNames = [ 
	"Marcus",
	"Sarah",
	"Josefine"
]
dump( aNames )
newNames1 = arrayMap( aNames, ( Any item, Any index, Any arr ) => {
	return {
		"name" : item
	}
} )
dump( newNames1 )
// member function
newNames2 = aNames.map( ( Any item, Any index, Any arr ) => {
	return {
		"name" : item
	}
} )
dump( newNames2 )

```


