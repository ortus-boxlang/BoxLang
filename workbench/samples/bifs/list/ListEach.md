### List Loop using listEach

Using a semicolon delimiter.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUEq0TrJOVuLKAXJdE5MzNBRALB0FDQXHvEqF1JzU3NQ8IBfEycxLSa2AMEFqFDQVbO0Uqrk4y4syS1L9S0sKSks0FJSUwcqUrZShepWtlRQ0uWp1FJRADACh8SPY" target="_blank">Run Example</a>

```java
list = "a;b;c"
listEach( list, ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;" )
}, ";" )

```

Result: 1:a;2:b;3:c;

### Member Function Example

List Loop list.listEach()

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUEq0TrJOVuLKAXL1QIRrYnKGhoKGgmNepUJqTmpual6JDpiTmZeSWgFhgtQpaCrY2ilUc3GWF2WWpPqXlhSUlmgoKCmDlSlbKUP1KlsrKWhy1eooKIEYAI%2F7I7o%3D" target="_blank">Run Example</a>

```java
list = "a;b;c"
list.listEach( ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;" )
}, ";" )

```

Result: 1:a;2:b;3:c;

### Example using a Closure

Example 1

<a href="https://try.boxlang.io/?code=eJxVjbEOwjAMRGf8FVaGppXyBUgMHVhhYEQMaTFqUJtUqSNaIf6dJAUkJp997840jLX3esEdnhE24u46K1QUIzFl0bhGwAV6M%2FEpUkImRCqUiUgzAlKATi173XYl0qdT4dz2bgqesMr51c5Nfx7cgm3ZOPs7lljbJRVZPZDKi7FXmrHCJ2we3jAdA4%2BB87dDhLBAgZpXbBt18U3A6w1kukeq" target="_blank">Run Example</a>

```java
empArray = [ 
	"john",
	"pete",
	"bob"
]
listS = "'john', 'pete', 'bob'"
arrayEach( empArray, xclosure )
listEach( listS, xclosure )

function xclosure( Any empname, Any index ) {
	writeOutput( empName & " at index: " & index )
}

```

Result: john at index: 1pete at index: 2bob at index: 3'john' at index: 1 'pete' at index: 2 'bob' at index: 3

### Another Closure Example

Example 2

<a href="https://try.boxlang.io/?code=eJxFTs0KwjAYO9unCD1IB30CwdPwNlEUvNdStw9mHW2KiPjudkPwlj%2BSeOGrk0xsoY9C5mtJ%2FWBxcmOQGezF3cWicxmX0LuslbqV6CmPiClJZFsbDM6suIevBA3eavVMwnAonAoNdFtSCpGLv4HG%2BpdUHzXW9Z3zg1mk%2BYv9F6P5Al31NnE%3D" target="_blank">Run Example</a>

```java
cityList = "Pittsburgh, Raleigh, Miami, Las Vegas"

function printCity( String city ) {
	writeOutput( "Current city: " & city )
}
listEach( cityList, printCity )

```

Result: Current city: PittsburghCurrent city: RaleighCurrent city: MiamiCurrent city: Las Vegas

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1j0sKwkAQRNfmFMVkE8lg9moCLnSl6BXyac3AZCKTjlHEuzv5iHgAFw3VVNWD0qphxBAnnRqWF0tkZJPeSFJquRSedv42zcsAvZIIsDEPkKaKXH54lCnoPso%2BgzniBE9v1lnFdGz52nIA4Q8xf%2BlPXX%2B1zmwiMPde7qIIB6oysji3JmdVG3SKS%2BRtw3WFgrSqHM16v9AeMfV2U%2B9Dbdjux2m1oZC7OuTSrROTsfgO%2B8ckCRE68QaLJnH2" target="_blank">Run Example</a>

```java
list = "Plant,green,save,earth"
listEach( list, ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;<br>" )
} )
// Member function with custom delimiter
writeOutput( "<br>Member Function<br>" )
strLst = "one+two+three"
strLst.listEach( ( Any element, Any index, Any list ) => {
	writeOutput( "#index#:#element#;<br>" )
}, "+" )

```


