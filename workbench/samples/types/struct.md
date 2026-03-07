### Creating structs using the function `structNew`

```java
// Create a default struct ( unordered )
myStruct = structNew();

// Create an ordered struct which will maintain key order of insertion
myStruct = structNew( "ordered" );

// Create a case-sensitive struct which will require key access to use the exact casing
myStruct = structNew( "casesensitive" );
myStruct[ "cat" ] = "pet";
myStruct[ "Cat" ] = "wild";

// Create a sorted struct 
myStruct = structNew( "sorted", "textAsc" )
```


### Creating structs using object literal syntax

```java
// Create an empty default struct ( unordered )
myStruct = {};

// Create an empty struct and populate it with values
animals = {
  cow: "moo",
  pig: "oink"
};

// JavaScript-style key shorthand
sound = "moo";
animal = { sound }; // same as { sound: sound }

// Create an ordered struct which will maintain key order of insertion
// Note that you must provide the ordered struct with data to prevent confusion as to whether it is an array or struct
orderedAnimals = [
  cow: "moo",
  pig: "oink"
];
```

### Object destructuring assignment

```java
data = { a: 10, b: 20, c: { d: 40, e: 50 }, f: 60 };

// Basic destructuring into the default assignment scope
({ a, b } = data);

// Scoped assignment via explicit rename
({ a: variables.a, b: request.b } = data);

// Defaults, nested destructuring, and rest
({ a, z = 999, c: { d }, ...rest } = data);
```
