### arraySplice inserting replacements at position 2 while removing 0 elements



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFWvNlVmSmgvSABRzS00CCyUWFSVWBhfkZCanaijkgo3UUTDSUTDQUQCr1rTmKi8CslJKcwtgCkCCAGPBI3Y%3D" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
];
item = [
	"Feb"
];
arraySplice( months, 2, 0, item );
writedump( months );

```

Result: ["Jan","Feb","March","April","June"]

### arraySplice inserting replacements at position 3 while removing 2 elements



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFWvNlVmSmgvSABRzS00CCyUWFSVWBhfkZCanaijkgo3UUTDWUTDSUQCr1rTmKi8CslJKcwtgCkCCAGQkI3k%3D" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
];
item = [
	"Feb"
];
arraySplice( months, 3, 2, item );
writedump( months );

```

Result: ["Jan","March","Feb"]

### arraySplice inserting replacements at position -3 while removing 0 elements



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFWvNlVmSmgvSABRzS00CCyUWFSVWBhfkZCanaijkgo3UUdA11lEw0FEAK9e05iovArJSSnMLYCpAggCDJyOk" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
];
item = [
	"Feb"
];
arraySplice( months, -3, 0, item );
writedump( months );

```

Result: ["Jan","Feb","March","April","June"]

### arraySplice inserting replacements at position 5 which is greater than the length of the array



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFWvNlVmSmgvSABRzS00CCyUWFSVWBhfkZCanaijkgo3UUTDVUTDQUQCr1rTmKi8CslJKcwtgCkCCAGQqI3k%3D" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
];
item = [
	"Feb"
];
arraySplice( months, 5, 0, item );
writedump( months );

```

Result: ["Jan","March","April","June","Feb"]

### Splice an array using member function



<a href="https://try.boxlang.io/?code=eJzLzc8ryShWsFWIVuDiVPJKzFPSAdK%2BiUXJGWCWY0FRZg6Y5VWal6rEFWvNlVmSmgvSABRzS00CC%2BWCTdErLsjJTE7VUDDSUTDQUQCr07TmKi8CslJKcws0FCDqQIIAiRAhWQ%3D%3D" target="_blank">Run Example</a>

```java
months = [ 
	"Jan",
	"March",
	"April",
	"June"
];
item = [
	"Feb"
];
months.splice( 2, 0, item );
writedump( months );

```

Result: ["Jan","Feb","March","April","June"]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxzSawsVrBViFbg4lQKLs1T0gHSvvkQOjw1BUyHZJQWFYNZbkWZYDo4sUSJK9aaK7MkNRekG6SoNLUYLOZYVJRYGVyQk5mcqqHgAjReR8FYR8FARwGsWNOaq7wIyEopzS2ASIOEALc4Iyo%3D" target="_blank">Run Example</a>

```java
Days = [ 
	"Sun",
	"Mon",
	"Wed",
	"Thurs",
	"Fri",
	"Sat"
];
item = [
	"Tues"
];
ArraySplice( Days, 3, 0, item );
writedump( Days );

```


