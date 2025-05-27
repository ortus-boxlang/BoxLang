### getFunctionCalledName Basic Example

Show results of calling a function directly versus by reference

<a href="https://try.boxlang.io/?code=eJydjrEOgjAQhmf7FBdYYHJxkspi4qiLL3CWA5rUlpQrDMZ3V1A0Jp0c77%2Fv%2F%2FKLwekK6mAVa2cBFQc0h%2Fd5xCtlOdzEavSa6RS4C5xBEqVG7EGhMVQB9ltIG%2BKF2M%2Fxi0vlxZcJ5IW4i1%2BrbH0p20050do20S1y%2FQTmduxb%2FGGEQSN4qsmTVfT1f6KzWwqwixii5DTlAWJed%2FI%3D" target="_blank">Run Example</a>

```java

void function actualFunctionName() {
	writeOutput( "actualFunctionName() was called as: #getFunctionCalledName()#<br>" );
}
writeOutput( "<hr><h4>Calling actualFunctionName()</h4>" );
actualFunctionName();
writeOutput( "<hr><h4>Calling actualFunctionName() via reference</h4>" );
referenceToFunction = actualFunctionName;
referenceToFunction();

```

Result: 

### Getters and Setters Example

Example of using getFunctionCalledName to create dynamic getters and setters


```java
//callednamedemo.cfc
component
{
    variables.x1 = 1;
    variables.y1 = 2;

    function init() {
        return this;
    }

    function get() {
        var name = getFunctionCalledName();
        return variables[mid(name,4,len(name)-3)];
    }

    function set(value) {
        var name = getFunctionCalledName();
        variables[mid(name,4,len(name)-3)] = value;
    }

    this.getX1 = get;
    this.getY1 = get;
    this.setX1 = set;
    this.setY1 = set;
}

<!--- calledname.cfm --->

<bx:script>

	function test() {
		return getFunctionCalledName();
	}
	writeOutput( test() & "<br>" ); // test
	a = test;
	writeOutput( variables.a() & "<br>" ); // a
	o = new callednamedemo();
	// shows *real* methods get(), SetX1() and getY1(), etc.
	writeDump( o );
	o.setX1( 10 );
	o.setY1( 20 );
	writeOutput( o.getX1() & "<br>" ); // 10
	writeOutput( o.getY1() & "<br>" );
</bx:script>
 <!--- 20 --->
```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljs0KwjAQhM%2FNUww5pSD0AUIEETz6Dmua1kJ%2BSn%2BUIL67SVpB8LTszDc7G421UBA4%2BQhPzqCGOuLFKqPvQeySZFW7ulHgQZPqzXJZvV6G4M9krWmviRH1AZZuxir%2BNfGkGboQoJnnK2%2FJYioU4HkUaaaY%2BvMq0TTQYYzo9gvZTGxGuik4lEDcPk7ifwDU0%2BB%2FO7Zggkv2A8H8Sts%3D" target="_blank">Run Example</a>

```java
yell = ( Any name ) => {
	echo( name );
	dump( var=getFunctionCalledName(), label="Function was called as" );
};
yell( "yell" );
say = yell; // copy function
say( "say from " );
yell = say; // copy function again
yell( "yell from say" );

```


