### Changing a struct compared to changing its copy

`myNewStruct` holds a reference to `myStruct` so if you change `myNewStruct`, `myStruct` is changed accordingly as well, because they are the same struct just assigned to two variables.
In comparison `myOtherNewStruct` is a copy so if you change `myOtherNewStruct`, `myStruct` stays untouched because with the duplicate, a new, unique structure with the same key-value pairs is created thus they do not share the same reference

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVahW4OJUSsvPV1KwUlDyyS9KzVXILCguzVXSAYonJRaBxZMSq5S4aq25civ9UsvhWnOhpoDE%2FUsyUouQJVNKC3IykxNLUjXg6hQ0UUzQc%2FP3BypUSszITS1RKC7NU8I0CKYmJT8nv0ihOLMEqKa8KLMk1b%2B0pKC0BGE2WKGagpLCo7ZJQFJNAd0iVDkstgAdBwDc31vf" target="_blank">Run Example</a>

```java
myStruct = { 
	"foo" : "Lorem ipsum",
	"bar" : "baz"
};
myNewStruct = myStruct;
myOtherNewStruct = duplicate( myStruct );
myNewStruct.FOO = "ahmet sun";
myOtherNewStruct.FOO = "dolor sit";
writeOutput( myStruct.FOO & " → " & myNewStruct.FOO & " → " & myOtherNewStruct.FOO );

```

Result: ahmet sun → ahmet sun → dolor sit

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrSC0qzs9TsFWoVuDidPMMCg5RsFJQckpMSlXS4eL0cYTwg0pLMpS4aq25UkpzCzQUCiCaNK0V9PUVQGoVQAq4knPy81KBRqWUFuRkJieWpCKphOqEKEHXCFGlB7bNVkEpODcTaB1uy8DyaAZyoZoIAKLOP%2BU%3D" target="_blank">Run Example</a>

```java
person = { 
	FIRST : "Babe",
	LAST : "Ruth"
};
dump( person ); // Babe Ruth
clone = duplicate( person );
dump( clone ); // Babe Ruth
person.LAST = "Smith";
dump( person ); // Babe Smith
dump( clone );
 // Babe Ruth

```


