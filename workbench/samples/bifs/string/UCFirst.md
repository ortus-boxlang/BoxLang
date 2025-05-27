### Basic usage

Lucee4.5+ Capitalizes the first character of the first word only.

<a href="https://try.boxlang.io/?code=eJwrTXbLLCou0VBQykjNyclXKM8vyklRVFLQtOYCAIPJCHg%3D" target="_blank">Run Example</a>

```java
ucFirst( "hello world!" );

```

Result: Hello world!

### Capitalize all the words in string

Lucee4.5+ Using the optional doAll parameter capitalizes the first character of all words. Word separators are: whitespace, period, parenthesis, or dash.

<a href="https://try.boxlang.io/?code=eJwNy8EJgEAMBMC%2FVax%2BVFAb8G8fEvfkQC%2BQ5AS71%2B%2FAVNmyeQzoJB0qvqidyI5Xq2HgQ3u1sPcRRv9NiKQGSbPx2oMH%2FlVvltgja2m7CWGVGNfmA99rH%2FM%3D" target="_blank">Run Example</a>

```java
ucFirst( "cfdocs.org is your (everyone's) resource for cf-related documentation!", true );

```

Result: Cfdocs.org Is Your (everyone's) Resource For Cf-related Documentation!

### Handling of strings in all uppercase

Lucee4.5+ Using the optional doLowerIfAllUppercase parameter allows for intelligent capitalization of words in all caps.

<a href="https://try.boxlang.io/?code=eJwrTXbLLCou0VBQcnZz8XcO1vMPcleI9A8NUtBwDXMNivT3c1UP1lQIcg0Gijm7Krj5Byk4u%2BkGufo4hri6KAC1hPq6%2BoU4hnj6%2Bykq6SiUFJWmQkgFTWsuAO7xGiM%3D" target="_blank">Run Example</a>

```java
ucFirst( "CFDOCS.ORG YOUR (EVERYONE'S) RESOURCE FOR CF-RELATED DOCUMENTATION!", true, true );

```

Result: Cfdocs.org Your (everyone's) Resource For Cf-related Documentation!

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1jDEKg1AQBfuc4mGl4A1CWm%2FgAb66yoL%2Fa%2FbtBry9RNKksBoGhqGblgUvVIwhq%2FvXhliIVCbMkjxMYPIOoRMfTdjCsJVVi4AHXXL1fEyR9xr92KnRa%2FC6tpjTSvkBDZrb0C3%2BuxM5SzPp" target="_blank">Run Example</a>

```java
string = "submitting bugs and feature requests via our online system";
dump( UcFirst( string, false, false ) );
dump( UcFirst( string, true, false ) );

```


