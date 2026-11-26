### Simple structEvery example

Here we have simple example about structevery function. 


```java
var struct = { 
	"Name" : "Dhar",
	"Age" : "20",
	"Country" : "US"
}
structevery( struct, ( Any key, Any value ) => {
	writeOutput( key & ":" & value & " " )
	return true
} )

```

Result: Country:US Name:Dhar Age:20

### Simple structEvery member function example

Here we have simple example about structevery member function. 


```java
var struct = { 
	"Name" : "Dhar",
	"Age" : "20",
	"Country" : "US"
}
struct.every( ( Any key, Any value ) => {
	writeOutput( key & ":" & value & " " )
	return false
} )

```

Result: Country:US

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kEtLw0AUhdeZX3HMKgOh3QsRQi1SFB%2BoiEgXU7m1QyYzOo9KKP3vTiZUWuxK6PKee%2FnuOUdo2QrlUGEDlk3uXnCOvDUmL1l2P7vqJyN104%2BT%2BiktyXznbMvGYzw7wqO34d1P12S7gsMbSE9WeIKJEhrqHKSGS1cjTJRxwRIs%2BWC1Q5RpvIwGaMSEUvXgprZ0a6Troqt9PMSwLlGg1l0PB0d1gQ3LopvZEn5FSV0JB4G1UGH3Kn1CoRN24HCWyWWBG9K%2F5DcI%2BxFa0t6NrqevmEc%2B7%2FHZHoVlW7abk3W2BWeXof0soMSCVJX%2FjZKX0Y6tjmTkJ27yIUjy%2F25SOiyU0M1hjV%2BJeVDj2cl7TEGO9Tgk5D%2BYkOvo" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
}
// Use StructEvery() to iterate over keys in struct. Closure returns true/false.
allAnimalsAreNoisy = StructEvery( animals, ( Any key ) => {
	// If the key has a value return true (noisy animal)
	if( Len( animals[ arguments.KEY ] ) ) {
		return true
	}
	return false
} )
Dump( label="allAnimalsAreNoisy", var=allAnimalsAreNoisy )
// Use StructEvery() to iterate over keys in struct. Closure returns true/false.
allAnimalsAreQuiet = StructEvery( animals, ( Any key ) => {
	// If the key is blank return true (quiet animal)
	if( !Len( animals[ arguments.KEY ] ) ) {
		return true
	}
	return false
} )
Dump( label="allAnimalsAreQuiet", var=allAnimalsAreQuiet )

```

Result: allAnimalsAreNoisy dumps true; allAnimalsAreQuiet dumps false


