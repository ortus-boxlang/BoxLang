# Operators

## Equality

BoxLang supports the following equality operators:

1. `==` (Equal To): This operator checks if the values of two operands are equal or not. If true, then the condition becomes true.

```BoxLang
if (a == b) {
    // Executes if a is equal to b
}
```

2. `!=` (Not Equal To): This operator checks if the values of two operands are equal or not. If the values are not equal, then the condition becomes true.

```BoxLang
if (a != b) {
    // Executes if a is not equal to b
}
```

3. `EQ` (Equal To): This operator is similar to `==`. It checks if the values of two operands are equal or not. If true, then the condition becomes true.

```BoxLang
if (a EQ b) {
    // Executes if a is equal to b
}
```

4. `NEQ` (Not Equal To): This operator is similar to `!=`. It checks if the values of two operands are equal or not. If the values are not equal, then the condition becomes true.

```BoxLang
if (a NEQ b) {
    // Executes if a is not equal to b
}
```

5. `IS` (Equal To): This operator is another way to check equality in BoxLang. It works the same way as `==` and `EQ`.

```BoxLang
if (a IS b) {
    // Executes if a is equal to b
}
```

6. `IS NOT` (Not Equal To): This operator is another way to check non-equality in BoxLang. It works the same way as `!=` and `NEQ`.

```BoxLang
if (a IS NOT b) {
    // Executes if a is not equal to b
}
```

BoxLang is case-insensitive, so `eq`, `neq`, `is`, and `is not` are equivalent to `EQ`, `NEQ`, `IS`, and `IS NOT` respectively.

## Mathematical

BoxLang supports the following mathematical operators:

1. `+` (Addition): Adds two numbers.
   ```BoxLang
   a = 5 + 3; // a is 8
   ```

2. `-` (Subtraction): Subtracts the second number from the first.
   ```BoxLang
   a = 5 - 3; // a is 2
   ```

3. `*` (Multiplication): Multiplies two numbers.
   ```BoxLang
   a = 5 * 3; // a is 15
   ```

4. `/` (Division): Divides the first number by the second.
   ```BoxLang
   a = 6 / 3; // a is 2
   ```

5. `MOD` (Modulus): Returns the remainder of the first number divided by the second.
   ```BoxLang
   a = 7 MOD 3; // a is 1
   ```

6. `^` (Exponentiation): Raises the first number to the power of the second.
   ```BoxLang
   a = 2 ^ 3; // a is 8
   ```

7. `++` (Increment): Increases the value of a variable by 1.
   ```BoxLang
   a = 5>
   a++; // a is 6
   ```

8. `--` (Decrement): Decreases the value of a variable by 1.
   ```BoxLang
   a = 5>
   a--; // a is 4
   ```

9. `+=` (Addition assignment): Adds the right operand to the left operand and assigns the result to the left operand.
   ```BoxLang
   a = 5>
   a += 3; // a is 8
   ```

10. `-=` (Subtraction assignment): Subtracts the right operand from the left operand and assigns the result to the left operand.
    ```BoxLang
    a = 5>
    a -= 3; // a is 2
    ```

11. `*=` (Multiplication assignment): Multiplies the right operand with the left operand and assigns the result to the left operand.
    ```BoxLang
    a = 5>
    a *= 3; // a is 15
    ```

12. `/=` (Division assignment): Divides the left operand by the right operand and assigns the result to the left operand.
    ```BoxLang
    a = 6>
    a /= 3; // a is 2
    ```

These operators can be used in mathematical expressions, and the order of operations (parentheses, exponentiation, multiplication and division, addition and subtraction) applies.

## Boolean

BoxLang supports the following boolean operators:

1. `AND`: This operator returns true if both operands are true.

```bx:ml
<bx:set a = true>
<bx:set b = false>
<bx:output>#a AND b#</bx:output> <!--- Outputs: false --->
```

2. `OR`: This operator returns true if at least one of the operands is true.

```bx:ml
<bx:set a = true>
<bx:set b = false>
<bx:output>#a OR b#</bx:output> <!--- Outputs: true --->
```

3. `NOT`: This operator returns the inverse of the operand.

```bx:ml
<bx:set a = true>
<bx:output>#NOT a#</bx:output> <!--- Outputs: false --->
```

4. `EQ` or `==`: This operator checks if the operands are equal.

```bx:ml
<bx:set a = 10>
<bx:set b = 10>
<bx:output>#a EQ b#</bx:output> <!--- Outputs: true --->
```

5. `NEQ` or `!=`: This operator checks if the operands are not equal.

```bx:ml
<bx:set a = 10>
<bx:set b = 20>
<bx:output>#a NEQ b#</bx:output> <!--- Outputs: true --->
```

6. `LT` or `<`: This operator checks if the left operand is less than the right operand.

```bx:ml
<bx:set a = 10>
<bx:set b = 20>
<bx:output>#a LT b#</bx:output> <!--- Outputs: true --->
```

7. `GT` or `>`: This operator checks if the left operand is greater than the right operand.

```bx:ml
<bx:set a = 20>
<bx:set b = 10>
<bx:output>#a GT b#</bx:output> <!--- Outputs: true --->
```

8. `LTE` or `<=`: This operator checks if the left operand is less than or equal to the right operand.

```bx:ml
<bx:set a = 10>
<bx:set b = 10>
<bx:output>#a LTE b#</bx:output> <!--- Outputs: true --->
```

9. `GTE` or `>=`: This operator checks if the left operand is greater than or equal to the right operand.

```bx:ml
<bx:set a = 20>
<bx:set b = 10>
<bx:output>#a GTE b#</bx:output> <!--- Outputs: true --->
```

10. `IS`: This operator is similar to `EQ` or `==`, it checks if the operands are equal.

```bx:ml
<bx:set a = 10>
<bx:set b = 10>
<bx:output>#a IS b#</bx:output> <!--- Outputs: true --->
```

11. `IS NOT`: This operator is similar to `NEQ` or `!=`, it checks if the operands are not equal.

```bx:ml
<bx:set a = 10>
<bx:set b = 20>
<bx:output>#a IS NOT b#</bx:output> <!--- Outputs: true --->
```

12. `XOR`: This operator returns true if exactly one of the operands is true.

```bx:ml
<bx:set a = true>
<bx:set b = false>
<bx:output>#a XOR b#</bx:output> <!--- Outputs: true --->
```

These operators are used to perform logical operations in bx:ML.