### Test to see if a struct is empty



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVaiuteYqL8osSfUvLSkoLdFQKAbLeBa75haUVGoo5MKUaipoWnMBAGaYFCQ%3D" target="_blank">Run Example</a>

```java
myStruct = {};
writeOutput( structIsEmpty( myStruct ) );

```

Result: true

### Test to see if a struct contains something



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLgnPLMkIycjMSy9WsFWoVuDiVMrPS1VSsFJQSsvPV9IB8kvK88H8pMQiJa5aa67yosySVP%2FSkoLSEg2FYrAhnsWuuQUllRoKuZimaipoWnMBAK9QI8s%3D" target="_blank">Run Example</a>

```java
myStructWithThings = { 
	"one" : "foo",
	"two" : "bar"
};
writeOutput( structIsEmpty( myStructWithThings ) );

```

Result: false

### Using Member Function

CF11+ Lucee4.5+

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVaiuteYqL8osSfUvLSkoLdFQyIXK6XkWu%2BYWlFRqaCpoWnMBANb3EW0%3D" target="_blank">Run Example</a>

```java
myStruct = {};
writeOutput( myStruct.IsEmpty() );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzT11fwy89TSM0tKKlUKC4pKk0u4UrMy8xNzClWsFWo5uJ09g9XsFJQys3PV9Lh4gzwdAfx8jPzspW4aq259PUVXJG1piUW5YL0QaSCwYKexWAlGlBjNblSkzPyNRSUbArsHKE2QXQrZBZDHAK0QUENTbcCzFWaQCklG%2F0COyUFTSy2gFyAbIUbyEXEmA92OqrhAMfTWKE%3D" target="_blank">Run Example</a>

```java
// Non empty struct
animals = {
	COW : "moo",
	PIG : "oink"
};
// Empty struct
farm = {};
// StructIsEmpty(animals)
echo( "<p>Animals struct is empty: " & StructIsEmpty( animals ) & "</p>" );
// StructIsEmpty(farm)
echo( "<p>Farm struct is empty: " & StructIsEmpty( farm ) & "</p>" );

```


