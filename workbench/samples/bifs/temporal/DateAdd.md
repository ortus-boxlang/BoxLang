### Add Days to a Date

Add 30 days to August 3rd, 2014.

<a href="https://try.boxlang.io/?code=eJxLSSxJdUxJ0VBQSlHSUTA20FFQstA31jcyMDRRUtC05gIAhqsG9Q%3D%3D" target="_blank">Run Example</a>

```java
dateAdd( "d", 30, "8/3/2014" );

```

Result: {ts '2014-04-07 00:00:00'}

### Subtract Days from a Date

Subtract 30 days from August 3rd, 2014.

<a href="https://try.boxlang.io/?code=eJxLSSxJdUxJ0VBQSlHSUdA1NtBRULLQN9Y3MjA0UVLQtOYCAI3mByI%3D" target="_blank">Run Example</a>

```java
dateAdd( "d", -30, "8/3/2014" );

```

Result: {ts '2014-02-06 00:00:00'}

### Add Weeks to a Date

Here we're adding 8 weeks to the date August 3rd, 2014.

<a href="https://try.boxlang.io/?code=eJxLSSxJdUxJ0VBQKi9X0lGw0FFQstA31jcyMDRRUtC05gIAjpQHVA%3D%3D" target="_blank">Run Example</a>

```java
dateAdd( "ww", 8, "8/3/2014" );

```

Result: {ts '2014-05-03 00:00:00'}

### Add Days to a Date (Member Function)

Here we're adding 1 day to the current date/time.

<a href="https://try.boxlang.io/?code=eJxLLkpNLEl1AWINBSMDIyMdBUMDIFbQ1EtMSdFQUEpRAvOsuQDtZQnG" target="_blank">Run Example</a>

```java
createDate( 2022, 10, 1 ).add( "d", 1 );

```

Result: {ts '2022-10-02 00:00:00'}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyV0LsKwzAMBdA9XyEyuRDIY8jSqZ%2FiWgIbbLn4QcjfV%2B1aqGNpEVzuGTTPUCzBk3w8wEQkcGwSBeKSYV0gOO9dJhMZs0SgTanaA%2BpCA9bwUt%2Fzgahg9OMklQk4HuoGsvdh%2FsfvC1yUs8h7nxwc10JNmTvlDWysqclaYbfr6irVs2Xi5709ZohcbEsNvepJOrXQU%2BbHfQOok8Hu" target="_blank">Run Example</a>

```java
// the below code increments 10 milliseconds in actual date
dump( dateAdd( "l", 10, now() ) );
// the below code increments 60 seconds in actual date
dump( dateAdd( "s", 60, now() ) );
// the below code increments 60 minutes in actual date
dump( dateAdd( "n", 60, now() ) );
// the below code increments 2 hours in actual date
dump( dateAdd( "h", 2, now() ) );
// the below code increments 1 day in actual date
dump( dateAdd( "d", 1, now() ) );
// the below code increments 1 month in actual date
dump( dateAdd( "m", 1, now() ) );
// the below code increments 1 year in actual date
dump( dateAdd( "yyyy", 1, now() ) );

```


