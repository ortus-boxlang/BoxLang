### Find first match for a nested struct key



<a href="https://try.boxlang.io/?code=eJxl0cuKwjAUBuB18xSHrCp0420z4qLOeB2t0voCUQ8aqImkpwwd8d3nFBlsze7kJ3z5k0xQUY4FjOEOIthN02ybdOED7iIIll88dCOeZss02yfxZsqBXNmLkXW6jl%2FhGo2xRorgEf0zvSbT85idKnOP2Rw%2FlSODVUvqN6W%2BJ83RujN61kI5p4u3UsMmNfSo%2BHDgs9%2Bl1KpTSxk0lYGnpNqcradkxIVqRjxG4lp9Y8WvXpArjzTT5sTrECbP74hA5qqgRF35WiCtQQmdkfhxmnBb0q2kEFbcI0OnVa5%2FMYQn2Km3%2FQHUr3Wf" target="_blank">Run Example</a>

```java
Beatles = { 
	PERSON1 : {
		ID : 1,
		FIRSTNAME : "John",
		LASTNAME : "Lennon"
	},
	PERSON2 : {
		ID : 2,
		FIRSTNAME : "Paul",
		LASTNAME : "McCartney"
	},
	PERSON3 : {
		ID : 3,
		FIRSTNAME : "George",
		LASTNAME : "Harrison"
	},
	PERSON5 : {
		ID : 5,
		FIRSTNAME : "Abbey",
		LASTNAME : "Road"
	},
	PERSON4 : {
		ID : 4,
		FIRSTNAME : "Ringo",
		LASTNAME : "Starr"
	}
};
myKey = structFindKey( Beatles, "lastName", "one" );
writeOutput( JSONSerialize( myKey ) );

```

Result: [{"path":".PERSON3.LASTNAME","owner":{"LASTNAME":"Harrison","FIRSTNAME":"George","ID":3},"value":"Harrison"}]

### Find all matches of a nested struct key



<a href="https://try.boxlang.io/?code=eJxl0MsKwjAQBdB18xVDVhW6aXWluPBR31Zp%2FYGgQw3EVNIpUsV%2FN0XEanYzl%2BFwmTEKUljCEB7AvH2cZrskhD48mOctp3YIAzvNlml2SEbb2AZ8VZw1b9LN6BtuUOtCc%2BY9gw8TtZnIYfaiUg6zPU6EIY31j9RtS11HmmNhcnSshTBGln%2Blem2q51Cp1HnhSBlZqmHYc8Au9Rpr%2B6%2BSTHWkmdQnu%2Fswfj8yAK5ESYm42ELAhVIcOgN2M5JwV9G1Ih9WtkeGRgol7%2BjDG%2Bw0Zy%2BSL2X7" target="_blank">Run Example</a>

```java
Beatles = { 
	PERSON1 : {
		ID : 1,
		FIRSTNAME : "John",
		LASTNAME : "Lennon"
	},
	PERSON2 : {
		ID : 2,
		FIRSTNAME : "Paul",
		LASTNAME : "McCartney"
	},
	PERSON3 : {
		ID : 3,
		FIRSTNAME : "George",
		LASTNAME : "Harrison"
	},
	PERSON4 : {
		ID : 4,
		FIRSTNAME : "Ringo",
		LASTNAME : "Starr"
	}
};
myKey = structFindKey( Beatles, "lastName", "all" );
writeOutput( JSONSerialize( myKey ) );

```

Result: [{"path":".PERSON3.LASTNAME","owner":{"LASTNAME":"Harrison","FIRSTNAME":"George","ID":3},"value":"Harrison"},{"path":".PERSON1.LASTNAME","owner":{"LASTNAME":"Lennon","FIRSTNAME":"John","ID":1},"value":"Lennon"},{"path":".PERSON2.LASTNAME","owner":{"LASTNAME":"McCartney","FIRSTNAME":"Paul","ID":2},"value":"McCartney"},{"path":".PERSON4.LASTNAME","owner":{"LASTNAME":"Starr","FIRSTNAME":"Ringo","ID":4},"value":"Starr"}]

### Find first match for a nested struct key using member function

CF11+ calling the findKey member function on a struct.

<a href="https://try.boxlang.io/?code=eJxl0MsKwjAQBdB18xVDVhVE8LVRXNT3s0rrD0QdNVATSVOkiv%2FuFBGr2Q2XcOZm%2Bihsgin04AHM24yieB3WoQMP5nmzIQ31Kk3jWRRvw2A1ooDP9VnxIl0G33CJSmnFmfesfphGmWk4zEZkicOs9gNhrML8R2qWpaYjTVCbEzrWVBgj079S7TLVdqhgt6Pd%2F1KkxeFHaZWVlqNEUp20o8SWChUMe3bZJV9gTlfvv%2B9fO0p1oMQHnojUhuJC%2FwGuFXKodNnNSIvrzF4z68OcCsRopEjkHX14S5Xi2QuE5nL8" target="_blank">Run Example</a>

```java
Beatles = { 
	PERSON1 : {
		ID : 1,
		FIRSTNAME : "John",
		LASTNAME : "Lennon"
	},
	PERSON2 : {
		ID : 2,
		FIRSTNAME : "Paul",
		LASTNAME : "McCartney"
	},
	PERSON3 : {
		ID : 3,
		FIRSTNAME : "George",
		LASTNAME : "Harrison"
	},
	PERSON5 : {
		ID : 5,
		FIRSTNAME : "Abbey",
		LASTNAME : "Road"
	},
	PERSON4 : {
		ID : 4,
		FIRSTNAME : "Ringo",
		LASTNAME : "Starr"
	}
};
myKey = Beatles.findKey( "lastName", "one" );
writeOutput( JSONSerialize( myKey ) );

```

Result: [{"path":".PERSON3.LASTNAME","owner":{"LASTNAME":"Harrison","FIRSTNAME":"George","ID":3},"value":"Harrison"}]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtkM1qwzAQhM%2FWUwy6xAZD7i0%2BmPQHU2hKVQj0piZKIiJLxZIb0pB379pWMW5z0%2B7OzjcraXUtjUeBM1iyWK5wgzNLkudlJe7pzWvneE4NUb33tZHNTnGWXKj5Uj3%2BlTttD3G6KN%2F%2BmSl3nLh5gptOzy63bD6H2LsjqAU55GJ3bf2ZwsgPZQpejgOe40s2RayQ9dsP2m7AO8coS32GtbNBaqvtDgd1gtti5vW3mrEtqcthf6XDXlCTvkGEpl2HzulJndJfXE5RaU7UwT4b0zbKtyZ4aIsrjtMDXqOWQkw5I2bgdKCexLN46bW0lOIHxuKHjg%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : {
		NOISE : "moo",
		SIZE : "large"
	},
	PIG : {
		NOISE : "oink"
	},
	CAT : {
		NOISE : "meow",
		SIZE : "small"
	}
};
// Show all animals
Dump( label="All animals", var=animals );
// Find "all" animal(s) containing key of 'size'
findAnimalsWithSize = StructFindKey( animals, "size", "all" );
// Show results in findAnimalsWithSize
Dump( label="Results of StructFindKey(animals, ""size"", ""all"")", var=findAnimalsWithSize );

```


