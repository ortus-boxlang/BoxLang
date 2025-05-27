### Prepend a value to an array

Uses the arrayPrepend function to prepend a value to the beginning of an array and shifts the positions of the existing elements.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNbh4jQCYkOuWGuuRJBEQFFqQWpeioZCMUypjoKJgqY1V3lRZkmqS2luAZIUSBwA6XgYvw%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	3,
	2,
	1
];
arrayPrepend( someArray, 4 );
writeDump( someArray );

```

Result: [4,3,2,1]

### Prepend a value to an array using the Array member function

CF11+ Lucee4.5+ Invoking the prepend function on an array is the same as running arrayPrepend.

<a href="https://try.boxlang.io/?code=eJxLyU8uyS9yLCpKrFSwVYhW4OJUck1OzkktLsnPU9IB8kJS8%2FIS80rA7ODczJIMMMs5sSAxJyVTiSvWmisFYYReQVFqQWpeioaCkkdpUYmSgqY1V3lRZkmqS2lugYYCkkqQDABQTSa%2B" target="_blank">Run Example</a>

```java
doctorArray = [ 
	"Eccleston",
	"Tennant",
	"Smith",
	"Capaldi"
];
doctorArray.prepend( "Hurt" );
writeDump( doctorArray );

```

Result: ['Hurt','Eccleston','Tennant','Smith','Capaldi']

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNbh4jQCYkOuWGuuRJBEQFFqQWpeioZCMUypjoKJgqY1V0ppbgGSKEhIX18hNzU3KbVIIa00L7kkMz%2BPKyU%2FuSS%2FKDwjH24LF6dSeEZmSUlidmqREtAqpZDUvLzEvBIw2wksCrIcVaNeAcwZSh6lRSVKCAegWQAUBwDY4UIY" target="_blank">Run Example</a>

```java
someArray = [ 
	3,
	2,
	1
];
arrayPrepend( someArray, 4 );
dump( someArray );
// member function
doctorWhoArray = [
	"Whittaker",
	"Tennant",
	"Baker"
];
doctorWhoArray.prepend( "Hurt" );
dump( doctorWhoArray );

```


