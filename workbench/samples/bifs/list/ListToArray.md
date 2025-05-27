### Simple example for listToArray function

Uses the listToArray() function to retrieve a list as an array

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUCpKTdFJL0pNzdPJL0rMS09VsuZKTy1xLCpKrARK5wBVheSDeRpgjoKmNVdxfm6qV7C%2FH1AeRAWnFmUm5mRWpWoowHUCVZUXZZak%2BpeWFJSWaCjAtQAlAOB7KJE%3D" target="_blank">Run Example</a>

```java
list = "red,green,orange";
getArray = listToArray( list );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```

Result: ["red", "green", "orange"]

### Example for listToArray function with delimiter

Uses the listToArray() function with a semicolon delimiter to retrieve a list as an array

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErOz0lJKy3OzM%2BzLsgosM5KLEu0Li7MUbLmSk8tcSwqSqwEqsoBKg7JB%2FM0wBwdBSVrJQVNa67i%2FNxUr2B%2FP6AiEBWcWpSZmJNZlaqhANcOVFVelFmS6l9aUlBaoqEA1wKUAAD5Lyxd" target="_blank">Run Example</a>

```java
list = "coldfusion;php;java;sql";
getArray = listToArray( list, ";" );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```

Result: ["coldfusion", "php", "java", "sql"]

### Example for listToArray function with includeEmptyFields

If includeEmptyFields is true, empty value add in array elements

<a href="https://try.boxlang.io/?code=eJw9jDEOwjAMRfeewsoUpN7AYmBloEO5QFRcMDIkOA4ITl%2FTodPX03%2F%2FC1eDPYQpy2VulfMTy60g3tM7YX1JwO5KdlBNX9fE7XNeKa7QQ8DQg2kj2GFX84OO43By9R8jKSfhH0XYTtz6KBsNzUqzCNvEiwU1ni6k" target="_blank">Run Example</a>

```java
list = "coldfusion;php;;java;sql";
getArray = listToArray( list, ";", true );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```

Result: ["coldfusion", "php", " " , "java", "sql"]

### Example for listToArray function with multiCharacterDelimiter

Uses the listToArray() function to retrieve a list as an array with multiCharacterDelimiter

<a href="https://try.boxlang.io/?code=eJw9jDEOwjAMRfeewsoUJN%2Bg6sDKQIdygQhcCDIk2A6oKIcndOj09fXf%2BxzVYAB3TnyZi8b0xHzLWI3U8B7eAau%2B2PXdlWwvEpYGc3NOaW1%2BLQgOq0OYAyshmBSCXd9petBhGo9N%2BcdEEgPHL3nYzhr1kWg0FsvFPGxKG343TDPk" target="_blank">Run Example</a>

```java
list = "coldfusion,php,|test,java,|sql";
getArray = listToArray( list, ",|", false, true );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```

Result: ["coldfusion,php", "test,java", "sql"]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUCpKTdFJL0pNzdPJL0rMS09VsuZKTy1xLCpKrARK5wBVheSDeRpgjoKmNVdxfm6qV7C%2FH1AeRAWnFmUm5mRWpWoowHUCVZUXZZak%2BpeWFJSWaCjAtQAlAOB7KJE%3D" target="_blank">Run Example</a>

```java
list = "red,green,orange";
getArray = listToArray( list );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```


<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErOz0lJKy3OzM%2BzLsgosM5KLEu0Li7MUbLmSk8tcSwqSqwEqsoBKg7JB%2FM0wBwdBSVrJQVNa67i%2FNxUr2B%2FP6AiEBWcWpSZmJNZlaqhANcOVFVelFmS6l9aUlBaoqEA1wKUAAD5Lyxd" target="_blank">Run Example</a>

```java
list = "coldfusion;php;java;sql";
getArray = listToArray( list, ";" );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```


<a href="https://try.boxlang.io/?code=eJw9jDEOwjAMRfeewsoUpN7AYmBloEO5QFRcMDIkOA4ITl%2FTodPX03%2F%2FC1eDPYQpy2VulfMTy60g3tM7YX1JwO5KdlBNX9fE7XNeKa7QQ8DQg2kj2GFX84OO43By9R8jKSfhH0XYTtz6KBsNzUqzCNvEiwU1ni6k" target="_blank">Run Example</a>

```java
list = "coldfusion;php;;java;sql";
getArray = listToArray( list, ";", true );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```


<a href="https://try.boxlang.io/?code=eJw9jDEOwjAMRfeewsoUJN%2Bg6sDKQIdygQhcCDIk2A6oKIcndOj09fXf%2BxzVYAB3TnyZi8b0xHzLWI3U8B7eAau%2B2PXdlWwvEpYGc3NOaW1%2BLQgOq0OYAyshmBSCXd9petBhGo9N%2BcdEEgPHL3nYzhr1kWg0FsvFPGxKG343TDPk" target="_blank">Run Example</a>

```java
list = "coldfusion,php,|test,java,|sql";
getArray = listToArray( list, ",|", false, true );
someJSON = JSONSerialize( getArray );
writeOutput( someJSON );

```


