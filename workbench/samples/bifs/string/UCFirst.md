### Basic usage

Capitalizes the first character of the first word only.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKE12yywqBjKUMlJzcvIVyvOLclIUlRQ0FTQBMWgNgA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( ucFirst( "hello world!" ) )

```

Result: Hello world!

### Capitalize all the words in string

Using the optional `doAll` argument capitalizes the first character of every whitespace separated word.

```java
writeOutput( ucFirst( "boxlang.ortusbooks.com is your (everyone's) resource for BX-related documentation!", true ) )

```

Result: Boxlang.ortusbooks.com Is Your (everyone's) Resource For BX-related Documentation!

### Handling of strings in all uppercase

Using the optional `doLowerIfAllUppercase` argument lowercases words that are completely uppercase before capitalizing them.

```java
writeOutput( ucFirst( "boxlang.ortusbooks.com YOUR (EVERYONE'S) RESOURCE FOR BX-related DOCUMENTATION!", true, true ) )

```

Result: Boxlang.ortusbooks.com Your (everyone's) Resource For BX-related Documentation!

### Lowercase uppercase words without capitalizing every word

```java
writeOutput( ucFirst( "HELLO WORLD", false, true ) )

```

Result: Hello world

### Whitespace is collapsed

```java
writeOutput( ucFirst( "hello    world", true ) )

```

Result: Hello World

### Member function

```java
writeOutput( "box lang".ucFirst() )

```

Result: Box lang
