### Script Syntax



<a href="https://try.boxlang.io/?code=eJxdT8FqwzAMPdtfIZJLAqYfUOggYz6UpR24oR2MHTQmVpPMCWoSb4z%2B%2B%2By1a7MdzJOent6TDVr30npYQLLbE7cK7pGxxoCt36ONfY1s61jZumV0b6Gs0GMfnoIV%2BriXSProiR02pT300S6RTCPxgcwlogmjFXYZ8IlSkEHhPmFUP2BP0EAOixv4kmJEBkd%2Bi81A0TK16TwdZ2ffLE8TKf7FTttZzCu6jtxrdvXJpWDqB3YXSh4D6dn2dDe8h%2BuepAjhwhTL9e3DDua%2F50pxVOeR3mqz0RPFn69ehfqx0mZdlOVyUwXZ9Logks%2BQfwNtxnlJ" target="_blank">Run Example</a>

```java
Rainbow = "Whero, Karaka, Kowhai, Kakariki, Kikorangi, Tawatawa, Mawhero"
externalList = ""
reverseRainbow = listMap( rainbow, ( Any v, Any i, Any l ) => {
	var newValue = "#i#:#v.reverse()#"
	externalList = externalList.listAppend( newValue )
	return newValue
} )
writeDump( [
	{
		RAINBOW : rainbow
	},
	{
		REVERSERAINBOW : reverseRainbow
	},
	{
		EXTERNALLIST : externalList
	}
] )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljUEKAjEQBM%2FmFU0OsoF8YQUfIPiFgZ2VwBjDJKMu4t9djXjxVofqrlkttYoRnkoRjoVJ40Upn9i7m6bGk53LgLl7wXU4iinJOpNU24F%2BQsSAfV5wJTGOH0x54nvHt4yAcYeH2yg309xNbOGrd8%2F1%2F6%2F5TYUXfrY5Mw%3D%3D" target="_blank">Run Example</a>

```java
fruits = "apple,pear,orange"
writedump( fruits )
fruitsPlural = listMap( fruits, ( Any value, Any index, Any list ) => {
	return value & "s"
} )
writedump( fruitsPlural )

```

### Transform each item

```java
writeOutput( listMap( "a,b,c", ( value ) -> value.ucase() ) )

```

Result: A,B,C

### Use the index

```java
writeOutput( listMap( "a,b,c", ( value, index ) -> index & ":" & value ) )

```

Result: 1:a,2:b,3:c

### Member function

```java
writeOutput( "1,2,3".listMap( ( value ) -> value * 2 ) )

```

Result: 2,4,6
