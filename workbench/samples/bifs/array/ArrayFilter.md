### Simple numeric comparison

Take an array of struct items and use arrayFilter to return ones of a rating 3 and higher.

<a href="https://try.boxlang.io/?code=eJxt0MEKgkAQBuCz%2BxTDnhQkKOuSKHgx6mCQ3aLDRruypKtMa2Lhu7fbIchkbj8f%2FzAjsJU6QWQ9RHAC4ryI41BhUwproKxpSk59GyLTUhU2XRJn8P%2FohSkzYzuftDUyVfz1riZtZWg9potJepOdHMvASHIOiWCPGqXmqdV3cy2zV6ey1BxdEN8%2F%2BOBConowtAIPohjMEuS6RfXJZofkuM02EEcQhGQALySd7b22VePCLt9nOUfJSvnkpvZ3qWf1G9r8Zvs%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		"fruit" : "apple",
		"rating" : 4
	},
	{
		"fruit" : "banana",
		"rating" : 1
	},
	{
		"fruit" : "orange",
		"rating" : 5
	},
	{
		"fruit" : "mango",
		"rating" : 2
	},
	{
		"fruit" : "kiwi",
		"rating" : 3
	}
];
favoriteFruits = arrayFilter( fruitArray, ( Any item ) => {
	return item.RATING >= 3;
} );
writedump( JSONSerialize( favoriteFruits ) );

```

Result: [{"fruit":"apple","rating":4},{"fruit":"orange","rating":5},{"fruit":"kiwi","rating":3}]

### Using a member function

This is the same example as above, but using a member function on the array instead of a standalone function.

<a href="https://try.boxlang.io/?code=eJxtz0ELgkAQBeCz%2ByuGPSmIUNYlUfBS1MEgu0WHjXZlSVeZ1sTC%2F95uhw4qc3t8vOEJbKVOEVkPMVyAOB%2FiOFTYlMIGKGuaklPfhsi0VIVNV8QZ%2FAm9MWVubBeztkamiknvetZWhtZjupylD9nJsQyNJNeICPaqUWq%2Btfpp1or%2F9EDIUnN0wYVU9WBQBR7ECZh65LpF9cuCU3reZztIYggjMoAXkc423tuqceGQH7Oco2SlfHMXRu88q79fCGTe" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		"fruit" : "apple",
		"rating" : 4
	},
	{
		"fruit" : "banana",
		"rating" : 1
	},
	{
		"fruit" : "orange",
		"rating" : 5
	},
	{
		"fruit" : "mango",
		"rating" : 2
	},
	{
		"fruit" : "kiwi",
		"rating" : 3
	}
];
favoriteFruits = fruitArray.filter( ( Any item ) => {
	return item.RATING >= 3;
} );
writedump( JSONSerialize( favoriteFruits ) );

```

Result: [{"fruit":"apple","rating":4},{"fruit":"orange","rating":5},{"fruit":"kiwi","rating":3}]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtz0ELgkAQBeDz7q8YPClIUNYlUfBieOkgdooOG62ypKtMayHhf2%2BMCFZkbo%2BPN7wSe2USRDFABGfg7M0ZS%2FNTVsAeHNF1tXR8ivKkyI4Hyracjf6MXYWms916wbUodDXr2y24hlhrs80Cu6uXslVAil9CXopni8rIdFr3oGViWpiq2kh0ofxv9sGFRA9AtAEPohjoAUrTo%2F5mq19zHEEQ8hG8kN%2F6pqMK%2BwHlH8T%2BVdU%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		FRUIT : "apple",
		RATING : 4
	},
	{
		FRUIT : "banana",
		RATING : 1
	},
	{
		FRUIT : "orange",
		RATING : 5
	},
	{
		FRUIT : "mango",
		RATING : 2
	},
	{
		FRUIT : "kiwi",
		RATING : 3
	}
];
favoriteFruits = arrayFilter( fruitArray, ( Any item ) => {
	return item.RATING >= 3;
} );
dump( favoriteFruits );

```


<a href="https://try.boxlang.io/?code=eJxtz0ELgkAUBODz7q94eFIQoaxLouDF8NJB7BQdNtqVJV3ltRYS%2FvdeEYEicxs%2BBkZhr22KKAaI4QScvThjWXHMS9iBI7qulo5PVZGW%2BWFP3Yaz0Z%2BxizCUqVstuBaFqWZ72wXXEGunbL3Abvqppyokxc8RV%2BLRorYy%2B7y70zP1vxkoXVuJLriQmgEINeBBnABNo7Q9mm8X%2FDaTGMKIj%2BBF%2FNo3nQuzaerfsLhTuA%3D%3D" target="_blank">Run Example</a>

```java
fruitArray = [ 
	{
		FRUIT : "apple",
		RATING : 4
	},
	{
		FRUIT : "banana",
		RATING : 1
	},
	{
		FRUIT : "orange",
		RATING : 5
	},
	{
		FRUIT : "mango",
		RATING : 2
	},
	{
		FRUIT : "kiwi",
		RATING : 3
	}
];
favoriteFruits = fruitArray.filter( ( Any item ) => {
	return item.RATING >= 3;
} );
dump( favoriteFruits );

```


