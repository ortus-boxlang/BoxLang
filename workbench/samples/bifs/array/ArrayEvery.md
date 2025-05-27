### Example for positive result

Checks whether all items in an array are greater than 2 and outputs true because all of them fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI00eHiNAViMyA254q15iovyixJ9S8tKSgt0VBIBKl0LUstqoSydRQ0FBzzKhXKEnNKUxU0FWztFKq5OItSS0qL8qCCdgpG1ly1QDlNay4AGTodsA%3D%3D" target="_blank">Run Example</a>

```java
array = [ 
	4,
	5,
	6,
	7
];
writeOutput( arrayEvery( array, ( Any value ) => {
	return value > 2;
} ) );

```

Result: true

### Example for negative result

Checks whether all items in an array are greater than 2 and outputs false because some of them do not fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI01OHiNAJiYyA24Yq15iovyixJ9S8tKSgt0VBIBKl0LUstqoSydRQ0FBzzKhXKEnNKUxU0FWztFKq5OItSS0qL8qCCdgpG1ly1QDlNay4AFRodpA%3D%3D" target="_blank">Run Example</a>

```java
array = [ 
	1,
	2,
	3,
	4
];
writeOutput( arrayEvery( array, ( Any value ) => {
	return value > 2;
} ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljkELgjAYhs%2F7fsWLJ4WhFXpJFDxUENSlY4QMWpfmlOmCEf73JhgYXR8envdtXC2MEQ4FriD2JsbO1WmHLYK9EfoZcE%2BqwwTSFbGR%2FzoXKxfGZv1vHNt%2BqWSpV%2BiWk1CqbtXd7zbzhVi%2BpHEhQlTaoZOmbzUiFCV8z8jBGj3TeGqVhX%2BU08gxGCs5MkQ53W3Thfi2PUCS4CFUL%2BkDrGE7rw%3D%3D" target="_blank">Run Example</a>

```java
my_array = [ 
	{
		NAME : "Frank",
		AGE : 40
	},
	{
		NAME : "Sue",
		AGE : 21
	},
	{
		NAME : "Jose",
		AGE : 54
	}
];
all_old = my_array.every( ( Any person ) => {
	return person.AGE >= 40;
}, true, 5 );
dump( all_old );
 // false

```


