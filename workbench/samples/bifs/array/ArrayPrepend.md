### Prepend a value to an array

Uses the arrayPrepend function to prepend a value to the beginning of an array and shifts the positions of the existing elements.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNbh4jQCYkOuWK5EkHhAUWpBal6KhkIxTKWOgomCJld5UWZJqn9pSUFpiYaCV7C%2FX3BqUWZiTmZVKpJSBU0FTQCgxR5y" target="_blank">Run Example</a>

```java
someArray = [ 
	3,
	2,
	1
]
arrayPrepend( someArray, 4 )
writeOutput( JSONSerialize( someArray ) )

```

Result: [4,3,2,1]

### Prepend a value to an array using the Array member function

 Invoking the prepend function on an array is the same as running arrayPrepend.

<a href="https://try.boxlang.io/?code=eJxNjLEKQjEMAGf7FSFTC%2BIfOIgI4uAb6iYOpQ0YqGnJSxH9euUtut0Nd6Vla7pTTS%2FYwhXcCg85V5qtCa6%2FdiGRJLZwfLDdF9qnnmphdDdXfodNV%2BokxQMehxpCcE9lo2lYH%2BbhFKdzJOVU%2BU0e%2FkoIED7Ygixx" target="_blank">Run Example</a>

```java
doctorArray = [ 
	"Eccleston",
	"Tennant",
	"Smith",
	"Capaldi"
]
doctorArray.prepend( "Hurt" )
writeOutput( JSONSerialize( doctorArray ) )

```

Result: ["Hurt","Eccleston","Tennant","Smith","Capaldi"]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNbh4jQCYkOuWK5EkHhAUWpBal6KhkIxTKWOgomCJldKaW4BkiBQRF9fITc1Nym1SCGtNC%2B5JDM%2FjyslP7kkvyg8Ix9uBRenUnhGZklJYnZqkRLQHqWQ1Ly8xLwSMNsJLAq0GVWfXgHMDUoepUUlSnDb0YzXBABDXECs" target="_blank">Run Example</a>

```java
someArray = [ 
	3,
	2,
	1
]
arrayPrepend( someArray, 4 )
dump( someArray )
// member function
doctorWhoArray = [
	"Whittaker",
	"Tennant",
	"Baker"
]
doctorWhoArray.prepend( "Hurt" )
dump( doctorWhoArray )

```


