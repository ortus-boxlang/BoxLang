### Delete an element from an array

Deletes the first `apple` element from the array `arr`.

<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDiVEosKMhJVdIBsvKLEvPSIcyC1MQiMMMRLMsVa82VWFSUWOmSmpNakuqX75xYnKqhABTSUYAqUdC05iovyixJdSnNLQBLgUQA9s0eFg%3D%3D" target="_blank">Run Example</a>

```java
arr = [ 
	"apple",
	"orange",
	"pear",
	"Apple"
];
arrayDeleteNoCase( arr, "Apple" );
writeDump( arr );

```

Result: ['orange','pear','Apple']

### Additional Examples

<a href="https://try.boxlang.io?code=eJyNjr8KwjAQxmfzFEemCoU%2BQMlQ7eLSJTiJw0WuUoi2XFOrb29yirhUXL77w3ff%2FZAZDBxArbTr7x6vZ53H3u7tTqtjqWbuAtXTZcjghmyQOQePjrzRG2p7Jg3rUsU1PmryFKjptzhSBuLU42Q7cfzIqdpAnExQFPCGSIkNzQkt0qAwVU7K6TWI4gJjPP0fU8zpRxTvF2i%2FEz%2FAT7cRXw8%3D" target="_blank">Run Example</a>

```java
arr = [ 
	"boxlang",
	"SUSI"
];
writeDump( var=arr, label="Before" );
arrayDeleteNoCase( arr, "suSi" );
writeDump( var=arr, label="After" ); // boxlang
arrNew = [
	"a",
	"Ab",
	"c",
	"A",
	"a"
];
writeDump( var=arrNew, label="Before" );
arrayDeleteNoCase( arrNew, "a", "all" );
writeDump( var=arrNew, label="After" );

```


