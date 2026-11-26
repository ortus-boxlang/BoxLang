### Simple Example

Changes the delimiters in the list

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnHOSMxLT3VJzcnMLdZQUErLz9dJSizSyckvSs3VySwoLs1V0lFQqlFS0FTQBAC%2F8xTZ" target="_blank">Run Example</a>

```java
writeOutput( listChangeDelims( "foo,bar,lorem,ipsum", "|" ) )

```

Result: foo|bar|lorem|ipsum

### Example with Custom Delimiter

Changes the delimiters in the list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJwVxDEKwCAMAMCvBCcFf9HufYOF1AaMERPpEvr20hvumWR4LBvLIjRS2%2B7SK%2B7YiDVCuETyWaY3mciZhi52RldhtJt6DRnC%2B%2BcBEqQPrRgbzw%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listChangeDelims( "foo,bar|lorem,ipsum|me|something", "~", "|" ) )

```

Result: foo,bar~lorem,ipsum~me~something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxlzssKwjAUhOF9n2LIqiXB4GUrCO2youATxHpsA0lakhP7%2BloLblwP%2FN9ojZv1kyM8c%2BjYjqGYo2W6ZJ4yl2ht4nowoaeGnPWphLg6E1j1kSioZF6kyEQehII4CVSoCq1xJn%2Bn%2BGtitjygy4lHj8cS%2BhCxSBzbxDhCbOVO7uVBrHiT%2FVRiXTfu%2F4FeMPnF3qFyPrU%3D" target="_blank">Run Example</a>

```java
// Simple function
writeOutput( ListChangeDelims( "Plant,green,save,earth", "@" ) )
// Member function with custom delimiter
strLst = "1+2+3+4"
writeDump( strLst.listChangeDelims( "/", "+" ) )

```


