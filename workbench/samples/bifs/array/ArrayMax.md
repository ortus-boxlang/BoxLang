### Simple example with empty array

To get the largest numeric value of an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFVIBNF%2BqeUaCoYKmtZc5UWZJan%2BpSUFpSUaEDnfxAoNhWK4Dk2QKgDlCBWc" target="_blank">Run Example</a>

```java
someArray = arrayNew( 1 );
writeOutput( arrayMax( someArray ) );

```

Result: 0

### Get largest numeric value of an array

Uses the arrayMax function to get the largest numeric value of an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNDLW4eI0MQUSFuZAwhDE44q15iovyixJ9S8tKSgt0VBIBGnwTazQUCiGa9dU0LTmAgAYNxUk" target="_blank">Run Example</a>

```java
someArray = [ 
	23,
	45,
	87,
	1,
	4
];
writeOutput( arrayMax( someArray ) );

```

Result: 87

### Get largest numeric value of an array using member function

CF11+ Lucee4.5+

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNDLW4eI0MQUSBkBsCOJwxVpzlRdllqT6l5YUlJZoKBTD9OjlJlZoaCpoWnMBAIh4EtQ%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	23,
	45,
	0,
	1,
	4
];
writeOutput( someArray.max() );

```

Result: 45

### Additional Examples


```java
aNames = array( 10412, 42, 33, 2, 999, 12769, 888 );
dump( arrayMax( aNames ) );
// member function
dump( aNames.max() );

```


