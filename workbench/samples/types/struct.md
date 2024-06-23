### Creating structs using the function `structNew`

```java
// Create a default struct ( unordered )
myStruct = structNew();

// Create an ordered struct which will maintain key order of insertion
myStruct = structNew( "ordered" );

// Create a case-sensitive struct which will require key access to use the exact casing
myStruct = structNew( "casesenstive" );
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

// Create an ordered struct which will maintain key order of insertion
// Note that you must provide the ordered struct with data to prevent confusion as to whether it is an array or struct
orderedAnimals = [
  cow: "moo",
  pig: "oink"
];
```
