# cbMockData

Welcome to the age of mocking and data modeling.  `cbMockData` allows you to generate fake JSON data as a JSON REST Service, a ColdBox Module or a simple CFC Service API.  The idea being that you may be offline, may not have access to an API, or simply need some fake data to test on your front end or seed a complete database with fake data.  It can also be used as a testing or data migration tool to generate fake data for your application.

`cbMockData` allows you to define the return JSON model in a very deterministic and simple modeling DSL.  You can define the number of objects, the types of data, nested data, and even custom data suppliers.  You can also define the return type as an array of objects or a struct of objects.  Please note that this project is also included as a module in [TestBox](https://www.testbox.run), so you can leverage it in your testing specs.

Read on :rocket: for some modeling goodness!

## Requirements

* BoxLang 1+
* ColdFusion 2021+
* Lucee 5+

## Installation

Leverage CommandBox and type `box install cbmockdata` to install the module.  Please remember that if you have TestBox installed, you have this module already.

```bash
box install cbmockdata
```

## Usage

Once installed you can leverage it in different ways:

1. **Class** : Install it into your application, instantiate the `MockData.cfc` and call the `mock` method using the mocking argument DSL: `new cbMockData.models.MockData().mock()`.
2. **REST Service** : Startup a CommandBox server in the root of the package once installed (`box server start`) and execute it via port: `3000`.  You can execute `GET` commands and pass the mocking DSL via the query string or execute a `POST` command with the mocking DSL as the body in JSON.
3. **ColdBox Module** : Install it via CommandBox in a ColdBox app and hit the service via `/cbMockData` with a `GET` using the query string mocking DSL or a `POST` using the mocking DSL as the body in JSON.  You can also get access to the mocking instance via the WireBox ID: `MockData@cbMockData` and call the `mock` method using the mocking argument DSL.

### Customizing the service port

To specify a port or change the port, just add it an argument to the `server start` command or modify the `server.json` port configuration to your liking.  You can even add SSL if you need to.

```
box server start port=XXXX
```

### Getting Data

To get data from the REST service, point your XHR or `cfhttp` calls to the following entry points and either pass the mocking DSL via the query string or as a JSON `POST` body.

```
# Standalone Service
http://localhost:3000/

# ColdBox Module Service
http://localhost:8080/cbMockData
```

By default it will produce a glorious array of 10 objects of nothing! Since we did not specify any modeling data. So let's continue.

> **Note:** MockData uses CORS so if you're running a virtual domain then you will still be able to hit the service.(As long as you have a decent browser.)

### Number of objects (`$num`)

The number of objects to be returned by the service is determined by the `$num` argument, which defaults to `10` items:

```js
# service call
http://localhost:3000/?$num=5

# ColdBox Module Service
http://localhost:8080/cbMockData?$num=5

# object
var data = getInstance( "MockData@cbMockData" )
    .mock(
        $num = 5
    );
```

#### Random Numbers

You can also specify a random return number by using the `rnd` or `rand` suffix in the following forms:

* `$num:rand:10` - A random number between 1-10.
* `$num:rnd:5:20` - A random number between 5-20.

```js
# service call
http://localhost:3000/?$num=rand:10

# ColdBox Module Service
http://localhost:8080/cbMockData?$num=rand:10

# object
var data = getInstance( "MockData@cbMockData" )
    .mock(
        $num = "rnd:10:20"
    );
```

### Available Return Types : (`$returntype`)

By default the service/method call will return **X** amount of records in the form of an `array`.  However, if you would like to just return an object literal representation you can do so by using the `$returnType` argument.

Available return types:

* `array` - **Default**, returns an array of objects
* `struct` - Returns an object literal struct

```js
// Method Call
var data = getInstance( "MockData@cbMockData" )
    .mock(
		$returnType = "struct",
		name = "name",
		age = "age",
		id = "uuid",
		email = "email"
	);

// Service call
http://127.0.0.1:60299/cbMockData?$returnType=struct&name=name&age=age&id=uuid&email=email
```

The output will be something like this:

```js
// The output will be something like this
{
    "id": "91659091-A489-4706-BAC64FA8E1665509",
    "name": "Danny Tobias",
    "age": 33,
    "email": "idegeneres@microsoft.com"
}
```

### Available Mocking Types

The available types cbMockData supports are:

* `age`: Generates a random "adult" age of 18 to 75.
* `all_age`: Generates a random age of 1 to 100.
* `autoincrement` : Returns an incremented index starting from 1
* `baconlorem`: Returns bacon lorem ipsum text. If used as `baconlorem:N`, returns N paragraphs. If used as `baconlorem:X:Y`, returns a random number of paragraphs between X and Y.
* `boolean`: Generates a random boolean value of `true` or `false`.
* `boolean-digit` : Generates a random boolean value as a digit of 0 or 1.
* `date`: Generates a random date
* `datetime`: Generates a random date and time value
* `datetime-iso` : Generates a random date and time value in ISO format
* `email`: Generates a random email.
* `fname`: Generates a random first name.
* `imageurl` : Generates a random image URL with a random protocol
* `imageurl_http` : Generates a random image URL with `http` only protocol
* `imageurl_https` : Generates a random image URL with `https` only protocol
* `ipaddress` : Generates an ipv4 address
* `name`: Generates a random name.
* `lname`: Generates a random last name.
* `lorem`: Returns lorem ipsum text. If used as `lorem:N`, returns N paragraphs. If used as `lorem:X:Y`, returns a random number of paragraphs between X and Y.
* `num`: By default, a number from 1 to 10. You can also use the form `num:X` for a random number between 1 and X. Or `num:X:Y` for a random number between X and Y.
* `oneof:x:y`: Requires you to pass N values after it delimited by a colon. Example: `oneof:male:female`. Will return a random value from that list.
* `rnd:N`, `rand:N`, `rnd:x:y`, `rand:x:y` : Generate random numbers with a specific range or range cap.
* `sentence`: Generates a sentences. If used as `sentence:N`, returns N sentences.  If used as `sentence:X:Y`, returns a random number of sentences beetween X and Y.
* `ssn`: Generates a random Social Security number.
* `string`: Generates a random string of length 10 by default.  You can increase the length by passing it `string:length`.
* `string-alpha` : Generates a random alpha string of length 10 by default.  You can increase the length by passing it `string-alpha:length`.
* `string-numeric` : Generates a random numeric string of length 10 by default.  You can increase the length by passing it `string-numeric:length`.
* `string-secure` : Generates a random secure (alpha+numeric+symbols) string of length 10 by default.  You can increase the length by passing it `string-secure:length`.
* `tel`: Generates a random (American) telephone number.
* `guid`: Generates a 36 characgter Microsoft formatted GUID
* `uuid`: Generates a random UUID
* `url` : Generates a random URL with a random protocol
* `url_http` : Generates a random URL with `http` only protocol
* `url_https` : Generates a random URL with `https` only protocol
* `website` : Generates a random website with random protocol
* `website_http` : Generates a random website, `http` only protocol
* `website_https` : Generates a random website, `https` only protocol
* `words`: Generates a single word. If used as `word:N`, returns N words.  If used as `words:X:Y`, returns a random number of words beetween X and Y.

### Calling Types By Function Name

Please check out the apidocs at : https://apidocs.ortussolutions.com/#/coldbox-modules/cbMockData/ for the latest methods, but you can also use the mocking methods instead of going via the `mock()` method.

* `baconLorem()`
* `dateRange()`
* `email()`
* `firstName()`
* `imageUrl()`
* `ipAddress()`
* `lastName()`
* `lorem()`
* `num()`
* `oneOf()`
* `sentence()`
* `ssn()`
* `string()`
* `telephone()`
* `uri()`
* `websiteUrl()`
* `words()`

### Supplier Type (Custom Data)

You can also create your own content by using a supplier closure/lambda as your type.  This is a function that will create the content and return it for you.

> Please note that this only works when using the direct function call approach, not the REST service since you have to pass in a closure.

```js
"name" : function( index ){
	return "luis";
}
```

The function receives the currently iterating `index` as an argument as well.  All you need to do is return back content.  Here is another example to return a random item from an array:

```js
"name" : ( index ) => {
	var names = [ "luis", "joe", "jose" ];
	return names[ randRange( 1, names.len() ) ];
}
```

### Mocking DSL

In order to define the type of data returned, you must specify one or more additional query string variables or arguments. The form is `name_of_field=type`, where `name_of_field` will be the name used in the result and `type` is the type of data to mock the value with.

```js
http://localhost:3000/?$num=3&author=name

# object
var data = getInstance( "MockData@cbMockData" )
    .mock(
        $num = 3,
        "author" = "name"
    );
```

This tells the service to return 3 objects with each containing an `author` field that has a type value of `name`. (More on types in a minute.) The result then would look something like this:

```json
[
    {
        author: "Frank Smith"
    },
    {
        author: "Gary Stroz"
    },
    {
        author: "Lynn Padgett"
    }
]
```

Additional fields for the object model can just be appended to the URL or method call:

```json
http://localhost:3000/?$num=3&author=name&gender=oneof:male:female

# object
var data = getInstance( "MockData@cbMockData" )
    .mock(
        $num = 3,
        "author" = "name",
        "gender" = "oneOf:male:female"
    );
```

Which gives...

```json
[
    {
        author : "Lisa Padgett",
        gender : "male"
    },
    {
        author : "Roger Clapton",
        gender : "male"
    },
    {
        author : "Heather Degeneres",
        gender : "male"
    }
]
```

### Nested Data

Since version `v3.0.0`, cbMockData supports the nesting of the field models to represent rich and complex JSON return structures.  We currently support the following nested types:

* array of objects - `name = [ { ... } ]`
* array of values - `name = [ { $type = "" } ]`
* object - `name = { ... }`

Let's imagine the following object graph:

```
Author
    Has Many Books
        Has Many Categories
    Has Keywords
    Has A Publisher
```

I can then use this mocking DSL to define it:

```js
getInstance( "MockData@cbMockData" )
    .mock(

        fullName    = "name",
        description = "sentence",
        age         = "age",
        id          = "uuid",
        createdDate = "datetime",
        isActive	= "oneof:true:false",

        // one to many complex object definitions
        books = [
            {
                $num = "rand:1:3",
                "id" = "uuid",
                "title" = "words:1:5",
                "categories" = {
                    "$num"      = "2",
                    "id"        = "uuid",
                    "category"  = "words"
                }
            }
        ],

        // object definition
        publisher = {
            "id" 	= "uuid",
            "name" 	= "sentence"
        },

        // array of values
        keywords = [
            {
                "$num" 	= "rand:1:10",
                "$type" = "words"
            }
        ]
    );
```

#### Nested Array of Values

To create nested array of values you will define the `name` of the property and then an array with a struct defining how many and of which type using the special keys: `$num, $type`

```js
// array of values
keywords = [
    {
        "$num" 	= "rand:1:10",
        "$type" = "words"
    }
]
```

#### Nested Array of Objects

To create nested array of objects you will define the name of the property and then an array with a struct defining how many and the definition of the object (Not there will be no `type` key):

```js
// array of objects
books = [
    {
        $num = "rand:1:3",
        "id" = "uuid",
        "title" = "words:1:5",
        "categories" = {
            "$num"      = "2",
            "id"        = "uuid",
            "category"  = "words"
        }
    }
]
```

### Nested Object

To create a nested object you will define the name of the property and then a struct defining it:

```js
// object definition
publisher = {
    "id" 	= "uuid",
    "name" 	= "sentence"
}
```
