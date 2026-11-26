### Clear the value of an array

Uses the arrayClear function to clear the value of an array

<a href="https://try.boxlang.io/?code=eJw9jkEKwjAURNf9pxi6SkAoHqCCFSyuFBFciItAv1pak%2FCbUIp4d2ulruYx8xbTuSevRcyAHBdQkh65Shdjnh914IlKYbYTFW38VYfaNildqZdR2sfgY1CUZTiJsZ2vWxb0ThojLtoKNycodlsIhygWYfBMSsHIfQmNfIUXJeb7YdOykXmgZPYlMr21Qvf%2FqqE%2Fwlk35g%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	"Red",
	"White",
	"Green",
	"Blue",
	"Pink"
]
writeOutput(
// Transpiler workaround for BIF return type
(( arg1 ) => {
	arrayClear( arg1 )
	return true
})( someArray ) )

```

Result: true

### Clear the value of an array
To clear the value of an array

<a href="https://try.boxlang.io/?code=eJw9jkEKwjAURNf9pxi6SkAoHqCCFSyuFBFciItAv1pak%2FCbUIp4d2ulruYx8xbTuSevRcyAHBdQkh65Shdjnh914IlKYbYTFW38VYfaNildqZdR2sfgY1CUZTiJsZ2vWxb0ThojLtoKNycodlsIhygWYfBMSsHIfQmNfIUXJeb7YdOykXmgZPYlMr21Qvf%2FqqE%2Fwlk35g%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	"Red",
	"White",
	"Green",
	"Blue",
	"Pink"
]
writeOutput(
// Transpiler workaround for BIF return type
(( arg1 ) => {
	arrayClear( arg1 )
	return true
})( someArray ) )

```

Result: true

### Clear value of an array using member function

Uses the member function is the same as running arrayClear.

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65YrjyErF5yTmpikYYmV3lRZkmqf2lJQWmJhgKygpzUPA1NBU0AAtIZxA%3D%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
]
numberArray.clear()
writeOutput( numberArray.len() )

```

Result: 0

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljk0OgjAQhdfMKd5SE6WibF0QjmFcVJ0ICS2kTDXc3lJjQ3Axizd5P59SqB1r4RHaQjunJ9x7K7q1rX2iBHds2MqIdysNpGFYb27sRhTYo8zpJ8%2B4UFbsKDuGO4Ur6UoPb4ZNSmyhFHovgw99c9XfXuyk4Ko71m5pCvo1OxJl5Dkkvpyq%2BR1zi8U1Aa0R2AwyfSs%2FuJJW5A%3D%3D" target="_blank">Run Example</a>

```java
// Creates an array containing 4 elements with the numbers 1 - 4.
numbers = [
	1,
	2,
	3,
	4
]
dump( numbers ) // outputs the array containing 1 - 4
// Clears the array leaving an array with 0 elements.
ArrayClear( numbers )
dump( numbers )
 // outputs the empty array

```


