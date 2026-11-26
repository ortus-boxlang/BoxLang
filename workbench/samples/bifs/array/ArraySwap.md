### Swap the position of two values in an array



<a href="https://try.boxlang.io/?code=eJwrLi1ILcrML3IsKkqsVLBViFbg4lQKLshMSS3S9U3MU9IBct2LUlPzFNzzk3IyIQIu%2Bckl%2BUUK%2FkCyoLQYLBSWmpefq8QVy5UIMii4PLFAQ6EY2WwdBUMdBWMFTa7yosySVP%2FSkoLSEg0Fr2B%2Fv2CgosSczKpUNB0KmgqaAL67M1w%3D" target="_blank">Run Example</a>

```java
superiorArray = [ 
	"Spider-Man",
	"Green Goblin",
	"Doctor Octopus",
	"Venom"
]
arraySwap( superiorArray, 1, 3 )
writeOutput( JSONSerialize( superiorArray ) )

```

Result: ["Doctor Octopus","Green Goblin","Spider-Man","Venom"]

### Swap the position of two values in an array using the member function

<a href="https://try.boxlang.io/?code=eJwrLi1ILcrML3IsKkqsVLBViFbg4lQKLshMSS3S9U3MU9IBct2LUlPzFNzzk3IyIQIu%2Bckl%2BUUK%2FkCyoLQYLBSWmpefq8QVy1WMbKBecXligYaCoY6CsYImV3lRZkmqf2lJQWmJhoJXsL9fMFBlYk5mVaqGAoo2BU0FTQCMcjE%2F" target="_blank">Run Example</a>

```java
superiorArray = [ 
	"Spider-Man",
	"Green Goblin",
	"Doctor Octopus",
	"Venom"
]
superiorArray.swap( 1, 3 )
writeOutput( JSONSerialize( superiorArray ) )

```

Result: ["Doctor Octopus","Green Goblin","Spider-Man","Venom"]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLi1ILcrML3IsKkqsVLBViFbg4lQKLshMSS3S9U3MU9IBct2LUlPzFNzzk3IyIQIu%2Bckl%2BUUK%2FkCyoLQYLBSWmpefq8QVy5UIMii4PLFAQ6EY2WwdBUMdBWMFTa6U0lx0OaCovr5CbmpuUmqRQlppXnJJZn4eF4oSvWKwkfjMAADCbEHe" target="_blank">Run Example</a>

```java
superiorArray = [ 
	"Spider-Man",
	"Green Goblin",
	"Doctor Octopus",
	"Venom"
]
arraySwap( superiorArray, 1, 3 )
dump( superiorArray )
// member function
superiorArray.swap( 1, 3 )
dump( superiorArray )

```


