### Extract month from date

Grabs the month out of a raw date yyyymmdd value.

<a href="https://try.boxlang.io/?code=eJzLzUzRUFAyMjAwMDQ0MlLSUVAyBRFGSgqa1lwAXLcFZA%3D%3D" target="_blank">Run Example</a>

```java
mid( "20001122", "5", "2" );

```

Result: 11

### Additional Examples

<a href="https://try.boxlang.io/?code=eJw9zMEKgkAQBuD7PsWfJ4UlLwWBdOtgYPoMuTvBHFZldsbw7QOjvgf4sgquKO7o5pXQWSBCJllJisa9hZUG08W0ROJYIqt4XDzOqFA1rq7xoDSS4GVTUJ4nl79hyxgtxs2j50CIz%2B3wC2%2BWln067uXpv30AG6wqdQ%3D%3D" target="_blank">Run Example</a>

```java
str = "I Love Lucee server";
writeOutput( mid( str, 8, 5 ) );
// Member function
str = "Hi buddy, Nice day!";
writeDump( str.mid( 4, 5 ) );

```


