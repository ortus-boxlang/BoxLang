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

<a href="https://try.boxlang.io/?code=eJyNjr8KwkAMxmfvKcJNFQp9gHJDtYtLl8NJHNKSQiFSSe8svr13EcSl4vLlD1%2B%2B%2FFAEHFzA7CzHgciWqfNnf7LmWptVpkBtvN0LeKA4FCmBsSd29kDjLGRhX5u0xmdLTIG6%2BYgLFaBOu0Q%2FqeNHTjMGkmyCqgJFyHkdrRkrsaASNb2W4T2o4gZhOv0fUs35RxLmDdbvxA%2BueQEcpF1f" target="_blank">Run Example</a>

```java
arr = [ 
	"lucee",
	"SUSI"
];
writeDump( var=arr, label="Before" );
arrayDeleteNoCase( arr, "suSi" );
writeDump( var=arr, label="After" ); // lucee
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


