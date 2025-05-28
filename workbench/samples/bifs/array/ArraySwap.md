### Swap the position of two values in an array



<a href="https://try.boxlang.io/?code=eJwrLi1ILcrML3IsKkqsVLBViFbg4lQKLshMSS3S9U3MU9IBct2LUlPzFNzzk3IyIQIu%2Bckl%2BUUK%2FkCyoLQYLBSWmpefq8QVa82VCDIpuDyxQEOhGNlwHQVDHQVjBU1rrvKizJJUl9JcdBUgOQDRVi2p" target="_blank">Run Example</a>

```java
superiorArray = [ 
	"Spider-Man",
	"Green Goblin",
	"Doctor Octopus",
	"Venom"
];
arraySwap( superiorArray, 1, 3 );
writeDump( superiorArray );

```

Result: ['Doctor Octopus', 'Green Goblin', 'Spider-Man', 'Venom']

### Swap the position of two values in an array using the member function

<a href="https://try.boxlang.io/?code=eJwrLi1ILcrML3IsKkqsVLBViFbg4lQKLshMSS3S9U3MU9IBct2LUlPzFNzzk3IyIQIu%2Bckl%2BUUK%2FkCyoLQYLBSWmpefq8QVa81VjGyiXnF5YoGGgqGOgrGCpjVXeVFmSapLaS5QCEUZSA4Av3srjA%3D%3D" target="_blank">Run Example</a>

```java
superiorArray = [ 
	"Spider-Man",
	"Green Goblin",
	"Doctor Octopus",
	"Venom"
];
superiorArray.swap( 1, 3 );
writeDump( superiorArray );

```

Result: ['Doctor Octopus', 'Green Goblin', 'Spider-Man', 'Venom']

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLi1ILcrML3IsKkqsVLBViFbg4lQKLshMSS3S9U3MU9IBct2LUlPzFNzzk3IyIQIu%2Bckl%2BUUK%2FkCyoLQYLBSWmpefq8QVa82VCDIpuDyxQEOhGNlwHQVDHQVjBU1rrpTSXHRJkLC%2BvkJuam5SapFCWmlecklmfh4Xihq9YrChBEwBADEkQw8%3D" target="_blank">Run Example</a>

```java
superiorArray = [ 
	"Spider-Man",
	"Green Goblin",
	"Doctor Octopus",
	"Venom"
];
arraySwap( superiorArray, 1, 3 );
dump( superiorArray );
// member function
superiorArray.swap( 1, 3 );
dump( superiorArray );

```


