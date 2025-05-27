### Run a function asynchronously and get the result



<a href="https://try.boxlang.io/?code=eJxLKy0pLUpVsFUoKs1zLK7MS9ZQ0NBUsLVTqObiLEoFyuUpKHmk5uTkK4TnF%2BWkKCpZc9UqaFpzlRdllqT6l5YUlJZoKKSBDdFLTy0B6gVKAgCTuhrL" target="_blank">Run Example</a>

```java
future = runAsync( () => {
	return "Hello World!";
} );
writeOutput( future.get() );

```

Result: Hello World!

### Run a function after the asynchronous function and use a five milliseconds timeout when calling get()



<a href="https://try.boxlang.io/?code=eJxdjUEKwkAMRdfOKf5yBqEFxVWp0BN4iJJqwEbJJEgR795QXbkK%2FPf%2Fy%2BTmSuihLkNdZMzIBf0Z77RTCiY4demD0tiNJCAGWcDydMNf7xvucdj6XVKqfrdQT9uP5kqWcQyCto3LFcYzPdwyC%2BZa0kvZ6OIWmozfOjwr13wyHQ%3D%3D" target="_blank">Run Example</a>

```java
future = runAsync( () => {
	return 5;
} ).then( ( Any input ) => {
	return input + 2;
} );
result = future.get( 3 ); // 3 is timeout(in ms)
writeOutput( result );

```

Result: 5

### Run a function asynchronously with then() and error()




```java
future = runAsync( () => {
	return 5;
} ).then( ( Any input ) => {
	return input + 2;
} ).error( () => {
	return "Error occurred.";
} );
writeOutput( future.get() );

```

Result: 7

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjU0OgkAMhdfOKd5yiAmgLgkmnMBDaNFJpJCZNoQY727HceWufT%2FfG1U0EnpE5SFtfPXwFfozXm4XyTzGoe3cG1UtD2JzMfCGwIsK%2FoJF3ONYCp276bR4jGXC%2FkhJn2JjRarvJN7wZqFp8hESJEw0q%2FjAmFLl1hiELipG9vj1jZTz81fNnVPrPrwgPNQ%3D" target="_blank">Run Example</a>

```java
future = runAsync( () => {
	return 10;
} ).then( ( Any input ) => {
	return input + 20;
} );
dump( future );
result = future.get( 10 ); // 10 is timeout(in ms)
writeOutput( result );
 // output is 30

```


