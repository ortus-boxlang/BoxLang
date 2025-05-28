### Output the literal string "Hello World"



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQUPJIzcnJVwjPL8pJUVLQtOYCAKyDbxm%3D" target="_blank">Run Example</a>

```java
writeOutput( "Hello World" );

```


### Output the equivalent string as a variable



<a href="https://try.boxlang.io/?code=eJxLL0pNLcnMS1ewVVDySM3JyVcIzy%2FKSVGy5iovyixJ9S8tKSgt0VBIhynTtOYCAM6DEV8%3D" target="_blank">Run Example</a>

```java
greeting = "Hello World";
writeOutput( greeting );

```


### Using the encodeFor argument

CF2016+ Passing in `html` to the `encodeFor` argument wraps the result with a call to encodeForHTML.

<a href="https://try.boxlang.io/?code=eJzLS8xNVbBVUApILUlVsuYqL8osSfUvLSkoLdFQUPJIzcnJV1BSUFPIAyrTUVDKKMnNUVLQtOYCAOVaEGw%3D" target="_blank">Run Example</a>

```java
name = "Pete";
writeOutput( "Hello " & name, "html" );

```

Result: Hello Pete

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVUEFOxDAMvPMKqwcEB9o72620dyQOIK6rbOo2hjQOjtNqeT0JixZx9IxnPGOniz9GI2YWEx3soenj8OoQnrJFhCj8jlaBEngc4XQGvXKHlNiSUeJwG04p7l420i8Ub8IIBgKHhyKfSC9sb8AJTvumcaoxPXYdhnajD4o4kmlZ5q5OXXFJ6fiGghSaZkh1BPN3q%2B%2FM0MIBZuGNwnyNuDmyrga1vCykWuIq%2F8RN2VosJjwB6YXPgfRc64zoaUWpRp%2FZ%2BIomnnQzgvDbI4vmunCpUcGUY2TRIgQMKwmHBYPCxFL8VvQcUVK9PmN5XVjZrzi2fReHZnezCSk%2BZ41Z78D9f%2F%2F97uYbebiSmQ%3D%3D" target="_blank">Run Example</a>

```java
html_paragraph = "<p>The Boxlang project is led by the Boxlang Association&nbsp;Switzerland a non-profit&nbsp;<a href=""https://en.wikipedia.org/wiki/Swiss_Verein"">swiss association</a>. A growing project which is committed to the success of its community by delivering quality software and a nurturing&nbsp;and supportive environment for developers to get involved.</p>";
writeOutput( html_paragraph );

```


