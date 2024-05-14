# Literals

In BoxLang, literals are fixed values that are not variables and do not change. Here are the types of literals in BoxLang:

1. **String literals**: These are sequences of characters. In BoxLang, you can denote them using single or double quotes. For example, `"Hello, World!"` or `'Hello, World!'`.

2. **Numeric literals**: These are integer or floating-point numbers. For example, `123`, `456.789`.

3. **Boolean literals**: These represent truth values and can be either `true` or `false`.

4. **Null literal**: This represents a null value and is denoted by `null`.

5. **Array literals**: These are denoted by square brackets `[]` and can contain a list of values. For example, `[1, 2, 3]`.

6. **Struct literals**: These are denoted by curly braces `{}` and can contain key-value pairs. For example, `{ "key1": "value1", "key2": "value2" }`.  An ordered (linked) struct literal can be accomplished using square braces in lieu of curly: `[ "key1": "value1", "key2": "value2" ]`.

7. **Date/Time literals**: These represent a specific point in time. Strings which contain parseable dates can be interpreted as date/time objects in certain contexts.  For example, `dateFormat( "2024-05-12", "MM/dd/yyyy" )` will be operated upon as a date object.  Note, however, that member date/time functions are not immediately available on a string literal.

8. **Query literals**: These are used to create a query object. They are represented in the format `queryNew("column1,column2", "type1,type2", [ [ "data1", "data2" ] ])`.

Remember, the way literals are used can vary depending on the context within the BoxLang code.