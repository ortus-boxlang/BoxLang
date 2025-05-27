### List Loop using listEach

Using a semicolon delimiter.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUEq0TrJOVrLmygHyXROTMzQUQCwdBQ0Fx7xKhdSc1NzUPCAXxMnMS0mtgDBBahQ0FWztFKq5OMuLMktS%2FUtLCkpLNBSUlMHKlK2UoXqVrZUUNK25anUUlCAsACwvJJM%3D" target="_blank">Run Example</a>

```java
list = "a;b;c";
listEach( list, ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;" );
}, ";" );

```

Result: 1:a;2:b;3:c;

### Member Function Example

List Loop list.listEach()

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUEq0TrJOVrLmygHy9UCEa2JyhoaChoJjXqVCak5qbmpeiQ6Yk5mXkloBYYLUKWgq2NopVHNxlhdllqT6l5YUlJZoKCgpg5UpWylD9SpbKyloWnPV6igoQVgAGaQkdQ%3D%3D" target="_blank">Run Example</a>

```java
list = "a;b;c";
list.listEach( ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;" );
}, ";" );

```

Result: 1:a;2:b;3:c;

### Example using a Closure

Example 1

<a href="https://try.boxlang.io/?code=eJxVjcEOgjAMhs%2F0KZodGCR7AokHDl7l4NF4GFjDDGxkbBFifHe3oSae%2Brf9%2BpXGqbZWrrjHM0LG7qbXTIQwkaMUWtMyuFQwqNmdAsZ4ZLhAHpFYA8FZBTJ6DrLrC6SPVeDSDWb2lrDcDNs%2Buf6XcPO6c8ro37TAWq9RpeVIIjVKX2nBEp%2BQPaxy1Hg3eZf%2BHQOEOTKUbsN2Ieffiwpe8AbgiEjb" target="_blank">Run Example</a>

```java
empArray = [ 
	"john",
	"pete",
	"bob"
];
listS = "'john', 'pete', 'bob'";
arrayEach( empArray, xclosure );
listEach( listS, xclosure );

function xclosure( Any empname, Any index ) {
	writeOutput( empName & " at index: " & index );
}

```

Result: john at index: 1pete at index: 2bob at index: 3'john' at index: 1 'pete' at index: 2 'bob' at index: 3

### Another Closure Example

Example 2

<a href="https://try.boxlang.io/?code=eJxFTksKwjAUXJtTDFlIhJzA4qq4qygW3McQ0wc1LckLUsS7mxTB3fyYGUu8dJQYB8gLMad7jn7QuJrRUQUnMk%2FS6EzCzXmTZCPEIwfLNAXMkQK3pUKh54I9bCHY4S02r0jszpnnzAqyzTG6wKu%2Fh8T2l2zER4xl%2F2jsoFatvtH%2F5hr5AiovNyw%3D" target="_blank">Run Example</a>

```java
cityList = "Pittsburgh, Raleigh, Miami, Las Vegas";

function printCity( String city ) {
	writeOutput( "Current city: " & city );
}
listEach( cityList, printCity );

```

Result: Current city: PittsburghCurrent city: RaleighCurrent city: MiamiCurrent city: Las Vegas

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1j0sKwjAURcd2FZd0Ummwc2sLDnSk6Bb6edpAmkr6ahVx7%2Fan4gKchBvePQeuVjUjgjjqxLA8WyIj6%2BRKkhLLhQgd3RU2SVZ46JOEh7W5gzSV1AHDR5mcbmPsO5gjivFwZq1VTIeGLw17EO5Qc5fuxLrhKrWxwDx0nv0TBNhTmZLFqTEZq8qgVVwga2quSuSkVdn5rPOr7SUTt524j7dmuxvnVYZ8biufi26heF8W33F%2FmSUh%2FCG9AMg6c50%3D" target="_blank">Run Example</a>

```java
list = "Plant,green,save,earth";
listEach( list, ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;<br>" );
} );
// Member function with custom delimiter
writeOutput( "<br>Member Function<br>" );
strLst = "one+two+three";
strLst.listEach( ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;<br>" );
}, "+" );

```


