### Simple Example



<a href="https://try.boxlang.io/?code=eJzLSS0pSS0qVrBViFbg4lRKVNIBkklgMhlMpihxxVpzJRYVJVa6JiZnaCjkQHToKGgoOOZVKqTmpOam5pXogDmZeSmpFQqaCrZ2CtVcnOVFmSWp%2FqUlBaUlGgpKymBJZStlqA5layUFTWuuWhABAKk3JdU%3D" target="_blank">Run Example</a>

```java
letters = [ 
	"a",
	"b",
	"c",
	"d"
];
arrayEach( letters, ( Any element, Any index ) => {
	writeOutput( "#index#:#element#;" );
} );

```

Result: 1:a;2:b;3:c;4:d;

### Member Function Example



<a href="https://try.boxlang.io/?code=eJxLVLBViFbg4lRKVNIBkklgMlmJK9aaK1EvNTE5Q0NBQ8Exr1IhNSc1NzWvRAfMycxLSa2AMBOLihIrFTQVbO0Uqrk4y4syS1L9S0sKSks0FJSUweqUrZShmpWtlRQ0rblqQQQAdHghQA%3D%3D" target="_blank">Run Example</a>

```java
a = [ 
	"a",
	"b",
	"c"
];
a.each( ( Any element, Any index, Any array ) => {
	writeOutput( "#index#:#element#;" );
} );

```

Result: 1:a;2:b;3:c;

### Additional Examples


```java
aNames = array( "Marcus", "Sarah", "Josefine" );
arrayEach( aNames, ( Any element ) => {
	dump( element );
} );

```


<a href="https://try.boxlang.io/?code=eJxVkEFrwzAMhc%2FJr3jYFwc81rFblwbG2HmX3EoPrqO1pk5SbIWtjP33JdoY5PKhJ570hDK7xNjhRNwGf3kZp4FN9VS6ubcvC%2BWUnXkUemEnJOG78CQ8C4MqD%2FN0Su726vzZwFkYPA83UKSeBrYiwtDR528pXlTYNfgqi48UmN4mvk5soGo%2FdtRoceut%2Fluhsdfre%2FWhvhdrfUyNwnx%2FkSPR1eBhs1nktwWniSweF7UOaUd2EW3oaQtt1otxh7x8qNLoQ4whkx%2BHLv%2FH%2FAC9Els8" target="_blank">Run Example</a>

```java
start = getTickCount();
a = [
	"a",
	"b",
	"c",
	"d",
	"e",
	"f",
	"g",
	"h",
	"i"
];
arrayEach( a, ( Any element, Any index, Any array ) => {
	writeOutput( "<code>#index#:#element# [#getTickCount()#]</code><br>" );
	sleep( 100 );
}, true, 3 );
writeOutput( "Total Time: #(getTickCount() - start)# milliseconds<br>" );

```


