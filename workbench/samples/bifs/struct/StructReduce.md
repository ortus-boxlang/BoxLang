### Script Syntax



<a href="https://try.boxlang.io/?code=eJxVjkGrwjAQhM%2FmVywpPFooeLc1IAgePAgelHfMM8sztCZlzVpE%2FO8mqQjeZj9mZoe0dX9%2BhCU8QMzkHo2EBcjjGcnLOpIdafePGW416U5n%2Bot978eJ%2BvGsbaYbQnRva6fJdlaKZyPYxvprID6F2M8nLIGmtzWUsHJ3GAhv1vP1oHvGOqMO75O4JQYVLBU8xIwwMLnvAPyAbE1QRcwU7Tyq1hhV5GC6jZKNeNbJ1CsZq5J%2FnnQjRrIB13wZPpugetMdh4FDCXF%2BRC%2BVH1oX" target="_blank">Run Example</a>

```java
rainbow = { 
	"Red" : "Whero",
	"Orange" : "Karaka",
	"Yellow" : "Kowhai",
	"Green" : "Kakariki"
};
ui = structReduce( rainbow, ( Any previousValue, Any key, Any value ) => {
	return previousValue & "<dt>#key#</dt><dd>#value#</dd>";
}, "<dl>" ) & "</dl>";
writeDump( rainbow );
writeOutput( ui );

```


### Using Member Function

CF11+ Lucee4.5+ 

<a href="https://try.boxlang.io/?code=eJxVjsEKwjAQRM%2FmK5YUpELRu60BQfDgQfCgeIxm0dCalDWxlOK%2Fm6R68Db7mJkdktpcbAcrGIBN%2BAEVhyXw0x3J8iKQPUlzwwR3kmQtEz1j09hupLa7S53olhDN11pL0rXm7F0yr0M9jY%2FmhMpfMYcc1qaHlvClrX8eZeOxSKjGfhSvyGAGKwEDmxA6T%2BY%2FAFPglXIiC5msWgRVKSWyFIy3Erxk7yKaGsFDVfQvoi5ZR9rhxj%2Fa%2FDcNZl%2B69671LoewO6AP1rdXdA%3D%3D" target="_blank">Run Example</a>

```java
rainbow = { 
	"Red" : "Whero",
	"Orange" : "Karaka",
	"Yellow" : "Kowhai",
	"Green" : "Kakariki"
};
ui = rainbow.reduce( ( Any previousValue, Any key, Any value ) => {
	return previousValue & "<dt>#key#</dt><dd>#value#</dd>";
}, "<dl>" ) & "</dl>";
writeDump( rainbow );
writeOutput( ui );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1kNFLwzAQxp%2Bbv%2BLIg3RQ1ndrB2UUKY5N1k2Zb7E9XVjaSNpszNH%2F3STtlCq%2BfZf73eX7jtW8YqKBGC5AvPnqGW7hQjxvucry1GhaSUkD85BnL64WTL0jJV5nHh%2Bz%2B9%2B45PVhxFdYcl0NA%2FNk82c%2FytNooDF%2BhOVJF5FSVx8%2BCPaKIqaJEMB6vzSAI1PxUMEkIr3M6jdpsuSt0kW7xlIX6F9nAvAhqc%2BgsNGiDZw%2B4LkXRyY0wgTimbWnsNWqBhNVV1i3zXSd5tvFBm6A3gk%2Bo0b89B7SnWtoMbPNpeQNmhwj5ilZbNNpH9qyoQEtnPPPf1h3jm80tMutohHpgv4zauw6wOqIhCHke3ka4hEs9vIa3V3F3OgLFw2MlQ%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : {
		NOISE : "moo",
		SIZE : "large"
	},
	PIG : {
		NOISE : "oink",
		SIZE : "medium"
	},
	CAT : {
		NOISE : "meow",
		SIZE : "small"
	}
};
dump( label="All animals", var=animals );
animalInfo = StructReduce( animals, ( Any result, Any key, Any value ) => {
	return arguments.RESULT & "<li>" & arguments.KEY & "<ul><li>Noise: " & arguments.VALUE.NOISE & "</li><li>Size: " & arguments.VALUE.SIZE & "</li></ul></li>";
}, "<ul>" ) & "</ul>";
// Show result
echo( animalInfo );

```


