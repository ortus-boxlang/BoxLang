### Basic usage

Capitalizes the first character of the first word only.

<a href="https://try.boxlang.io/?code=eJwrTXbLLCou0VBQykjNyclXKM8vyklRVFLQtOYCAIPJCHg%3D" target="_blank">Run Example</a>

```java
ucFirst( "hello world!" );

```

Result: Hello world!

### Capitalize all the words in string

Using the optional doAll parameter capitalizes the first character of all words. Word separators are: whitespace, period, parenthesis, or dash.

```java
ucFirst( "boxlang.ortusbooks.com is your (everyone's) resource for BX-related documentation!", true );

```

Result: boxlang.ortusbooks.com Is Your (everyone's) Resource For BX-related Documentation!

### Handling of strings in all uppercase

Using the optional doLowerIfAllUppercase parameter allows for intelligent capitalization of words in all caps.


```java
ucFirst( "boxlang.ortusbooks.com YOUR (EVERYONE'S) RESOURCE FOR BX-related DOCUMENTATION!", true, true );

```

Result: boxlang.ortusbooks.com Your (everyone's) Resource For BX-related Documentation!

### Additional Examples

```java
string = "submitting bugs and feature requests via our online system";
dump( UcFirst( string, false, false ) );
dump( UcFirst( string, true, false ) );

```


