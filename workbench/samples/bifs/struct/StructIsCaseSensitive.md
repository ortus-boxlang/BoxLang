### Full Function with Explicit CaseSensitive

Show when the struct is explicitly set to casesensitive.

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLjFUsFWAsPxSyzUUlJITi1OLU%2FOKM0syy1KVFDS5cmEK9Vx9XH1BypVSc1xz%2FfNSldDkjMByPqm5JeX%2B6HLGEH1AuYyiVKDO8qLMklT%2F0pKA0hINqP2exc5Au4NhdmsowE1Q0FTQBABFzTrK" target="_blank">Run Example</a>

```java
myStruct1 = StructNew( "casesensitive" )
myStruct1.ELEM1 = "elEmOne"
myStruct1.ELEM2 = "eLemtwO"
myStruct1.ELEM3 = "elemthree"
writeOutPut( StructIsCaseSensitive( myStruct1 ) )

```

Result: true

### Member Function with Explicit CaseSensitive

Show when the struct is explicitly set to casesensitive.

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLjFUsFWAsPxSyzUUlJITi1OLU%2FOKM0syy1KVFDS5cmEK9Vx9XH1BypVSc1xz%2FfNSldDkjMByPqm5JeX%2B6HLGEH1AuYyiVKDO8qLMklT%2F0pKA0hINBYTSzGJnoP3BMPs1NBU0AarROFM%3D" target="_blank">Run Example</a>

```java
myStruct1 = StructNew( "casesensitive" )
myStruct1.ELEM1 = "elEmOne"
myStruct1.ELEM2 = "eLemtwO"
myStruct1.ELEM3 = "elemthree"
writeOutPut( myStruct1.isCaseSensitive() )

```

Result: true

### Full Function with Implicit Struct Creation and Default Case Sensitivity

Show when the struct is implicitly created.

<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLjFRsFWoVuDiVErNSc01VFKwUgCyXHP981KVdMCirrlGEFGf1NyScn%2BoaGquMVQtUDSjKDVViauWq7wosyTVv7QkoLREQwFivGexc2JxanBqXnFmSWZZqoZCLtxeTQVNAD16KTA%3D" target="_blank">Run Example</a>

```java
myStruct4 = { 
	"elem1" : "elEmOne",
	"elEm2" : "eLemtwO",
	"elem3" : "elemthree"
}
writeOutPut( StructIsCaseSensitive( myStruct4 ) )

```

Result: false

