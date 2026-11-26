### arraySplice inserting replacements at position 2 while removing 0 elements



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFcuVWZKaC1IPFHJLTQKJJBYVJVYGF%2BRkJqdqKOSCDdRRMNJRMNBRACvW5CovAjL8S0sKSks0FLyC%2Ff2CU4syE3Myq%2BAaFDQVNAGViyjO" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
]
item = [
	"Feb"
]
arraySplice( months, 2, 0, item )
writeOutput( JSONSerialize( months ) )

```

Result: ["Jan","Feb","March","April","June"]

### arraySplice inserting replacements at position 3 while removing 2 elements



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFcuVWZKaC1IPFHJLTQKJJBYVJVYGF%2BRkJqdqKOSCDdRRMNZRMNJRACvW5CovAjL8S0sKSks0FLyC%2Ff2CU4syE3Myq%2BAaFDQVNAGWHijR" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
]
item = [
	"Feb"
]
arraySplice( months, 3, 2, item )
writeOutput( JSONSerialize( months ) )

```

Result: ["Jan","March","Feb"]

### arraySplice inserting replacements at position -3 while removing 0 elements



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFcuVWZKaC1IPFHJLTQKJJBYVJVYGF%2BRkJqdqKOSCDdRR0DXWUTDQUQCr1uQqLwIy%2FEtLCkpLNBS8gv39glOLMhNzMqvgOhQ0FTQBt1so%2FA%3D%3D" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
]
item = [
	"Feb"
]
arraySplice( months, -3, 0, item )
writeOutput( JSONSerialize( months ) )

```

Result: ["Jan","Feb","March","April","June"]

### arraySplice inserting replacements at position 5 which is greater than the length of the array



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFcuVWZKaC1IPFHJLTQKJJBYVJVYGF%2BRkJqdqKOSCDdRRMNVRMNBRACvW5CovAjL8S0sKSks0FLyC%2Ff2CU4syE3Myq%2BAaFDQVNAGWJCjR" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
]
item = [
	"Feb"
]
arraySplice( months, 5, 0, item )
writeOutput( JSONSerialize( months ) )

```

Result: ["Jan","March","April","June","Feb"]

### Splice an array using member function



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFcuVWZKaC1IPFHJLTQKJ5ILN0CsuyMlMTtVQMNJRMNBRACvT5CovAjL8S0sKSks0FLyC%2Ff2CU4syE3Myq4AKIfoUNBU0AZvOJrE%3D" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
]
item = [
	"Feb"
]
months.splice( 2, 0, item )
writeOutput( JSONSerialize( months ) )

```

Result: ["Jan","Feb","March","April","June"]

### Remove elements without replacements

```java
months = [ "Jan", "Feb", "March", "April" ]
months.splice( 2, 2 )
writeOutput( months.toList() )
```

Result: Jan,April

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxzSawsVrBViFbg4lQKLs1T0gHSvvkQOjw1BUyHZJQWFYNZbkWZYDo4sUSJK5YrsyQ1F6QZpKY0tRgk5FhUlFgZXJCTmZyqoeACNFxHwVhHwUBHAaxWk6u8CMhIKc0tgMgqaAIAE3YiNA%3D%3D" target="_blank">Run Example</a>

```java
Days = [ 
	"Sun",
	"Mon",
	"Wed",
	"Thurs",
	"Fri",
	"Sat"
]
item = [
	"Tues"
]
ArraySplice( Days, 3, 0, item )
writedump( Days )

```


