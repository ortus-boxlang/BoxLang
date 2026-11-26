### Simple example for listSort function

Uses the listSort() function to get the list which sorted by type text(case-sensitive)

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUHLyj%2FBx9HPXScqvyEnMS9dJTMlPStVxgvKCHD19%2FJW4ivOLSnwgGnKAVDCQqwFm6SgohaRWlCgB6ZTU4mQlBU2u8qLMklT%2F0pKCUqAauEZNACP4JRw%3D" target="_blank">Run Example</a>

```java
list = "BOXLANG,boxlang,adobe,Boxlang,RAILO"
sortList = listSort( list, "Text", "desc" )
writeOutput( sortList )

```

Result: boxlang,adobe,RAILO,Boxlang,BOXLANG 

### Example for listSort function with delimiters

Uses the listSort() function with delimiters to get the list which sorted by type numeric

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDI0sDYysNa1tLQ2MbM2NVDiKs4vKvGBSOYAqWAgVwPM0lFQ8ivNTS3KTFYCMhOLwZS1koImV3lRZkmqf2lJQSlQKVy%2FJgCV1R3n" target="_blank">Run Example</a>

```java
list = "10;20;-99;46;50"
sortList = listSort( list, "Numeric", "asc", ";" )
writeOutput( sortList )

```

Result: -99;10;20;46;50

### Simple Example for listSort function using sortType(textnocase)

Uses the listSort() function with delimiters to get the list which sorted by type textnocase(case-insensitive)

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDI0qAlydampTPXxyS%2Bv0TUyMatJL0pNzavxD0rMS09V4irOLyrxgSjOAVLBQK4GmKWjoBSSWlHil%2B%2BcWJyqBOQlFieDqBolBU2u8qLMklT%2F0pKCUqBquBGaACxjJzA%3D" target="_blank">Run Example</a>

```java
list = "10|RED|yeLLow|-246|green|ORange"
sortList = listSort( list, "TextNoCase", "asc", "|" )
writeOutput( sortList )

```

Result: -246|10|green|ORange|RED|yeLLow

### Additional Examples

<a href="https://try.boxlang.io/?code=eJylUEkKAjEQvPuKJieF1hBx9KKC4kVwOQw%2BYJZWBmYSyYI%2B30TjigfBQ1IFSXVVdV0Zu3EN6aqACbABdsUQ%2BygS7CY4QiFY66QrS1tnj862ofb%2FjdKRRSUCkzfGPM1MwaADnXchG%2Bd6Gg77fHmOZLNS5cRzda4zeeDziCLhq1kaRls624AlmasV%2F8sIo9EDfesXI6mKzNCPjTiHNTU5adg7WdhKyZaxeuUdw1pjE7yjd8F0ly7R55hTXPHCNcc2eFXI2QtXegv7PcwFKlGNOA%3D%3D" target="_blank">Run Example</a>

```java
listNumeric = "4,-16,2,15,-5,7,11"
writeOutput( listsort( listNumeric, "numeric", "asc" ) )
writeOutput( "<br><br>" )
writeOutput( listsort( "Adobe/boxlang/Boxlang/15/LAS", "text", "desc", "/" ) )
writeOutput( "<br><br>" )
writeOutput( listsort( "Adobe,boxlang,boxlang,15,LAS", "textnocase", "asc" ) )
writeOutput( "<br><br>" )
// Member function
strList = "Boxlang,Boxlang,LAS,SUSI,AdoBe"
writeDump( strlist.listSort( "textnocase", "asc" ) )

```


