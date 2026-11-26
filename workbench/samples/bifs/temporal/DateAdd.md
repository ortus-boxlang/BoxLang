### Add Days to a Date

Add 30 days to August 3rd, 2014.

<a href="https://try.boxlang.io/?code=eJxLSSxJdUxJ0VBQSlHSUTA20FFQstA31jcyMDRRUtAEAHjLBrA%3D" target="_blank">Run Example</a>

```java
dateAdd( "d", 30, "8/3/2014" )

```

Result: {ts '2014-04-07 00:00:00'}

### Subtract Days from a Date

Subtract 30 days from August 3rd, 2014.

<a href="https://try.boxlang.io/?code=eJxLSSxJdUxJ0VBQSlHSUdA1NtBRULLQN9Y3MjA0UVLQBAB%2FrAbd" target="_blank">Run Example</a>

```java
dateAdd( "d", -30, "8/3/2014" )

```

Result: {ts '2014-02-06 00:00:00'}

### Add Weeks to a Date

Here we're adding 8 weeks to the date August 3rd, 2014.

<a href="https://try.boxlang.io/?code=eJxLSSxJdUxJ0VBQKi9X0lGw0FFQstA31jcyMDRRUtAEAH%2F2Bw8%3D" target="_blank">Run Example</a>

```java
dateAdd( "ww", 8, "8/3/2014" )

```

Result: {ts '2014-05-03 00:00:00'}

### Add Days to a Date (Member Function)

Here we're adding 1 day to the current date/time.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSC5KTSxJdQFiDQUjAyMjHQVDAyBW0NRLTEnRUFBKUQLzFDQB4TwOzg%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( createDate( 2022, 10, 1 ).add( "d", 1 ) )

```

Result: {ts '2022-10-02 00:00:00'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyV0DEKwzAMBdA9pxCZUgg4yZC9R3EtgQ22XGKb4NtX6dihjqVF8PlvkFKQLcGLfDzBRCRwbA4KxDnBukBw3rtEJjImiUCbXLQH1JkGLOE9fc8n4gSjH2epzMDxnB4gO6h%2F%2Br7ATTgJvHfBwXHJ1IS5D97AxnI0VSvqdhtdpVlbJF6v7SBD5GxbaOhEK%2BmjZVaZH%2FYDNG7ARw%3D%3D" target="_blank">Run Example</a>

```java
// the below code increments 10 milliseconds in actual date
dump( dateAdd( "l", 10, now() ) )
// the below code increments 60 seconds in actual date
dump( dateAdd( "s", 60, now() ) )
// the below code increments 60 minutes in actual date
dump( dateAdd( "n", 60, now() ) )
// the below code increments 2 hours in actual date
dump( dateAdd( "h", 2, now() ) )
// the below code increments 1 day in actual date
dump( dateAdd( "d", 1, now() ) )
// the below code increments 1 month in actual date
dump( dateAdd( "m", 1, now() ) )
// the below code increments 1 year in actual date
dump( dateAdd( "yyyy", 1, now() ) )

```


### Add months, clamping to the end of the month

Results assume the runtime default timezone is UTC.

```java
writeOutput( dateAdd( "m", 1, createDate( 2024, 1, 31 ) ) )
```

Result: {ts '2024-02-29 00:00:00'}

### Subtract hours

```java
writeOutput( dateAdd( "h", -5, createDateTime( 2024, 1, 1, 3, 0, 0 ) ) )
```

Result: {ts '2023-12-31 22:00:00'}

### An unknown datepart throws an error

```java
try {
	dateAdd( "x", 1, createDate( 2024, 1, 1 ) )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: The datepart unit of [x] is not recognized.
