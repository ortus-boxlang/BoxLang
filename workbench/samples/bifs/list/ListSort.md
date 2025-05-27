### Simple example for listSort function

Uses the listSort() function to get the list which sorted by type text(case-sensitive)

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUHL293FxCw329PfTSc7PSUkrLc7Mz9NJTMlPStXxKU12ddUJcvT08Vey5irOLyrxgejKAVLBQK4GmKWjoBSSWlGiBKRTUouTlRQ0rbnKizJLUv1LSwpKgYrgOoESADo9JvA%3D" target="_blank">Run Example</a>

```java
list = "COLDFUSION,coldfusion,adobe,LucEE,RAILO";
sortList = listSort( list, "Text", "desc" );
writeOutput( sortList );

```

Result: coldfusion,adobe,RAILO,LucEE,COLDFUSION 

### Example for listSort function with delimiters

Uses the listSort() function with delimiters to get the list which sorted by type numeric

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDI0sDYysNa1tLQ2MbM2NVCy5irOLyrxgcjmAKlgIFcDzNJRUPIrzU0tykxWAjITi8GUtZKCpjVXeVFmSap%2FaUlBKVAt3ACgBAAFZx6i" target="_blank">Run Example</a>

```java
list = "10;20;-99;46;50";
sortList = listSort( list, "Numeric", "asc", ";" );
writeOutput( sortList );

```

Result: -99;10;20;46;50

### Simple Example for listSort function using sortType(textnocase)

Uses the listSort() function with delimiters to get the list which sorted by type textnocase(case-insensitive)

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDI0qAlydampTPXxyS%2Bv0TUyMatJL0pNzavxD0rMS09VsuYqzi8q8YGozgFSwUCuBpilo6AUklpR4pfvnFicqgTkJRYng6gaJQVNa67yosySVP%2FSkoJSoHK4GUAJAMBvJ%2Bs%3D" target="_blank">Run Example</a>

```java
list = "10|RED|yeLLow|-246|green|ORange";
sortList = listSort( list, "TextNoCase", "asc", "|" );
writeOutput( sortList );

```

Result: -246|10|green|ORange|RED|yeLLow

### Additional Examples

<a href="https://try.boxlang.io/?code=eJylkMsKwjAQRfd%2BxZCVwmiIWF34AB8IQtVF8QPadIRC20ge6OebWEVRBMFFkgvhzrl3ysLYnatIFxKmwAbYFUPso4iwG%2BEIhWDj1lkXlvbOnpxtQ%2BkNRum7ulsRWN0o5mVqJIMOdN6cbJLpWTjs4%2Bs5lM1zlRGXqsyPzhSq5rGTRFxEPJ4nYbqliw1vTuZG43%2By8MnCMrBC%2BRdWrWRq6OdenMOWqow0HF0trZ%2FaMlbHHhr2eyuDS09cN0QPwuSQbNBnWdBj2StXndrgfSFsL1xJk%2FhboCvvopB%2F" target="_blank">Run Example</a>

```java
listNumeric = "4,-16,2,15,-5,7,11";
writeOutput( listsort( listNumeric, "numeric", "asc" ) );
writeOutput( "<br><br>" );
writeOutput( listsort( "Adobe/coldfusion/Lucee/15/LAS", "text", "desc", "/" ) );
writeOutput( "<br><br>" );
writeOutput( listsort( "Adobe,coldfusion,lucee,15,LAS", "textnocase", "asc" ) );
writeOutput( "<br><br>" );
// Member function
strList = "Lucee,ColdFusion,LAS,SUSI,AdoBe";
writeDump( strlist.listSort( "textnocase", "asc" ) );

```


