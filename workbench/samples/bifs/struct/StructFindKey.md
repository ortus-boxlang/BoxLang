### Find first match for a nested struct key



<a href="https://try.boxlang.io/?code=eJxl0c%2BKwjAQBvBz8xRDTi30Uv9chD3UXXV1tUrrC6Q6aKAmSzplqeK774iI1dwm3%2BGXL5MxKqqwhg%2B4gAg2k7xYZwmM4CKCYP7FQxLzNJ3nxTZLVxMO5MIejbyly%2FQZLtEYa6QIrvGD6XWZnsdsVFN5zGr3qRwZbF%2Bkflfqe9IMrTugZ30r53T9VmrYpYYelZYl3%2F0u5VbtX5RBVxl4Sq7NwXpKQVzoxoirOLU%2F2PLSa3LNjqba7Pkcwvj%2BGzHIStWUqRO%2FCqQ1KCESf04Trhv6bSiEBbco0GlV6TOGcPciiP4BC%2FJ05A%3D%3D" target="_blank">Run Example</a>

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
}
myKey = structFindKey( Beatles, "lastName", "one" )
writeOutput( JSONSerialize( myKey ) )

```

Result: [{"path":".person3.lastname","owner":{"LASTNAME":"Harrison","FIRSTNAME":"George","ID":3},"value":"Harrison"}]

### Find all matches of a nested struct key



<a href="https://try.boxlang.io/?code=eJxl0MsKwjAQBdB18xVDVi100%2BpKcOH7XaX1B0IdNJCmkk6RKv67kSJWs7tzF4fLjFGQwgqG8ADmHWZptk8iGMCDed5qakMU2jRfpdkxGe1mtuDr8qL5u92OvuUWtS41Z94z%2FDBxl4kd5iBq5TC7fCIMaWx%2BpF5X6jnSAktzRsdaCmNk9Teq36X6DpVKfS4dKSNLvRn2ZEWzwca%2BqyJT5zSX%2BmRvH8btH0PgSlSUiMLuAS6U4hCwm5GE%2B5quNfmwtisyNFIoeUcfWi%2BA4AUH%2BWVA" target="_blank">Run Example</a>

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
}
myKey = structFindKey( Beatles, "lastName", "all" )
writeOutput( JSONSerialize( myKey ) )

```

Result: [{"path":".person3.lastname","owner":{"LASTNAME":"Harrison","FIRSTNAME":"George","ID":3},"value":"Harrison"},{"path":".person1.lastname","owner":{"LASTNAME":"Lennon","FIRSTNAME":"John","ID":1},"value":"Lennon"},{"path":".person2.lastname","owner":{"LASTNAME":"McCartney","FIRSTNAME":"Paul","ID":2},"value":"McCartney"},{"path":".person4.lastname","owner":{"LASTNAME":"Starr","FIRSTNAME":"Ringo","ID":4},"value":"Starr"}]

### Find first match for a nested struct key using member function

Calling the findKey member function on a struct.

<a href="https://try.boxlang.io/?code=eJxl0MsKwjAQBdB18xVDVi2I4GsjuKjvZ5XWH4g6aqAmkqZIFf%2FdERFbsxvu4tyZ6aOwKWbQgwcwbzOKk3XUgC48mOfNhjQ0ajSNZ3GyjcLViAI%2B12fF3%2Bky%2FIVLVEorzrxn7cs0y0zTYTYiTx1mtR8IYxUWFalVllqONEFtTuhYU2GMzP6W6pSpjkOFux11%2F0uxFoeK0i4rbUeJpTppR0ksLfRm2JNdigUW9PT%2B5%2F31o1QHSnzgqchsJC50DnCtkEPAbkZaXOf2mlsf5lSfoJEilXf04QMFELwAxaByQQ%3D%3D" target="_blank">Run Example</a>

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
}
myKey = Beatles.findKey( "lastName", "one" )
writeOutput( JSONSerialize( myKey ) )

```

Result: [{"path":".person3.lastname","owner":{"LASTNAME":"Harrison","FIRSTNAME":"George","ID":3},"value":"Harrison"}]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtkM1qwzAQhM%2FWUwy6xAJD7gUfTPqDKTSlKgR6U1MlEZGlYskNaci7dxW7DW5y086O5htJOdMoG1DiAJbN5gvc4MCy7Gleyzs688Z7XpAg67fTbFW71pxlRxKf64f%2Fdm%2FcdtjOqteLMO13o7RAcJv87MimU8iN34EUqL4Wu%2B2azxxWvWtb8uq84AW%2BVFsOE0S6fG%2FcB3jKG1x5EFh6F5Vxxq2x1Xv4FSbBfOsJW5G76q8vTNxIEukTZGy7ZUxJj3qf%2F9IKKkp7gvbx4q9rq0NnY4BxuBI4rv8yeKnDGHOm9JjEOYG4GN55raz4AWpkhpg%3D" target="_blank">Run Example</a>

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
}
// Show all animals
Dump( label="All animals", var=animals )
// Find "all" animal(s) containing key of 'size'
findAnimalsWithSize = StructFindKey( animals, "size", "all" )
// Show results in findAnimalsWithSize
Dump( label="Results of StructFindKey(animals, ""size"", ""all"")", var=findAnimalsWithSize )

```

Result: a dump of all animals, then a dump of the matches for the key size: an array with two result structs (COW and CAT; PIG has no size)


