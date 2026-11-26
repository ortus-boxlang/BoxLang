### Simple example

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNgFiU65YrvKizJJU%2F9KSgtISDYVEkGLPYpfUtMy81BQNhWKYAToKxgqaCpoAddIWxQ%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4,
	5
]
writeOutput( arrayIsDefined( someArray, 3 ) )

```

Result: true

### Simple example

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmMgNgFiU65YrvKizJJU%2F9KSgtISDYVEkGLPYpfUtMy81BQNhWKYAToKZgqaCpoAdeEWyA%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3,
	4,
	5
]
writeOutput( arrayIsDefined( someArray, 6 ) )

```

Result: false

### Simple example with two dimensional array

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJxzyywqLnEsKkqsVLBViFbg4jTU4eI0AmJjrliu4tTk%2FLwUuCxQEiRrCJI2BMkn5%2BcmZealwhQkgmi%2F1HINBSMFTS4wz7GgIDUvRUMBWaWOQhrCUrzqkO3X5CovyixJ9S8tKSgt0YDY5VnskpoGVI6h0VhBU0ETAPiSRV4%3D" target="_blank">Run Example</a>

```java
FirstArray = [ 
	1,
	2,
	3
]
secondArray = [
	11,
	12,
	13
]
combineArray = arrayNew( 2 )
arrayAppend( combineArray, firstArray )
arrayAppend( combineArray, secondArray )
writeOutput( arrayIsDefined( combineArray, 3 ) )

```

Result: false

### Simple example with two dimensional array

To check an array element is define or not

<a href="https://try.boxlang.io/?code=eJxzyywqLnEsKkqsVLBViFbg4jTU4eI0AmJjrliu4tTk%2FLwUuCxQEiRrCJI2BMkn5%2BcmZealwhQkgmi%2F1HINBSMFTS4wz7GgIDUvRUMBWaWOQhrCUrzqkO3X5CovyixJ9S8tKSgt0YDY5VnskpoGVI6hEWi%2FgiYA%2BI1FXQ%3D%3D" target="_blank">Run Example</a>

```java
FirstArray = [ 
	1,
	2,
	3
]
secondArray = [
	11,
	12,
	13
]
combineArray = arrayNew( 2 )
arrayAppend( combineArray, firstArray )
arrayAppend( combineArray, secondArray )
writeOutput( arrayIsDefined( combineArray, 2 ) )

```

Result: true

### Using the indexExists member function

Indexes are one-based, so index 0 never exists and neither does an index past the end of the array.

```java
letters = [ "a", "b" ]
writeOutput( letters.indexExists( 2 ) & "," & letters.indexExists( 3 ) & "," & letters.indexExists( 0 ) )
```

Result: true,false,false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjcsKwkAMRdfOV4SuKhQrdO1CcOM3FBepzUiw8yCdQfx7M1Uc7iIHknNjJXM6i%2BAbTjCC2TUY40JNp%2FTkF28woddsGAT947t2SqEempuZs4stYGm7rhey7Gluwf5fdDDAXtP3kCST0enITSRgs78nDv5XUZUD16LhWGxTdIvLSh%2BBmDiq" target="_blank">Run Example</a>

```java
fruitArray = [ 
	"apple",
	"kiwi",
	"banana",
	"orange",
	"mango",
	"kiwi"
]
dump( arrayIsDefined( fruitArray, 3 ) ) // true
// member function
dump( fruitArray.isDefined( 30 ) )
 // false

```


