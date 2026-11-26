### Simple Example



<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBViFbg4lRKVNIBkklgMhlMpihxxXIlFhUlVromJmdoKORANOgoaCg45lUqpOak5qbmleiAOZl5KakVCpoKtnYK1Vyc5UWZJan%2BpSUFpSUaCkrKYEllK2WoDmVrJQVNrloFTQAadCUa" target="_blank">Run Example</a>

```java
letters = [ 
	"a",
	"b",
	"c",
	"d"
]
arrayEach( letters, ( Any element, Any index ) => {
	writeOutput( "#index#:#element#;" )
} )

```

Result: 1:a;2:b;3:c;4:d;

### Member Function Example



<a href="https://try.boxlang.io/?code=eJxLVLBViFbg4lRKVNIBkklgMlmJK5YrUS81MTlDQ0FDwTGvUiE1JzU3Na9EB8zJzEtJrYAwE4uKEisVNBVs7RSquTjLizJLUv1LSwpKSzQUlJTB6pStlKGala2VFDS5ahU0AfcpIIU%3D" target="_blank">Run Example</a>

```java
a = [ 
	"a",
	"b",
	"c"
]
a.each( ( Any element, Any index, Any array ) => {
	writeOutput( "#index#:#element#;" )
} )

```

Result: 1:a;2:b;3:c;

### Additional Examples


```java
aNames = [ "Marcus", "Sarah", "Josefine" ]
arrayEach( aNames, ( Any element ) => {
	dump( element )
} )

```


<a href="https://try.boxlang.io/?code=eJxVkDFrwzAQhWfrVzykRQaXpnQLjqGUzl28hQyKfE1EZDlIZ9pQ%2Bt%2FrXrN4%2Bbh3PN47rrDLjB1OxH3wl9dpTmxr5ZbVXlXa6WbhUeiFg5CEH8KT8CwMWh2Uy9nd3pw%2FW7gGFi%2FpBoo0UuJGREgDff2P4kWNXYdvVX3mwPQ%2B83VmC936aaDOiNtszT3CYG%2FW15pD%2ByjW9pg7jVpVJRJdLZ42m0X9NOA8U4PnRawb%2BoldRB9G2sLYdSoeUP6eUxuMIcZQyE9pKPeOX3xoWdA%3D" target="_blank">Run Example</a>

```java
start = getTickCount()
a = [
	"a",
	"b",
	"c",
	"d",
	"e",
	"f",
	"g",
	"h",
	"i"
]
arrayEach( a, ( Any element, Any index, Any array ) => {
	writeOutput( "<code>#index#:#element# [#getTickCount()#]</code><br>" )
	sleep( 100 )
}, true, 3 )
writeOutput( "Total Time: #(getTickCount() - start)# milliseconds<br>" )

```


