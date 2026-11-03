### Script Syntax

```java
component displayname="Script Widget" output="false" { 
 // functions and properties here 
}
```

### The attributes scope

The `attributes` scope contains every attribute passed to the component call. Declare expected
attributes with `bx:param` so they get validated and defaulted.

```java
<!-- Component: productDisplay.bxm -->
<bx:param name="attributes.productId" type="string" required="true">
<bx:param name="attributes.showPrice" type="boolean" default="true">
<bx:param name="attributes.currency" type="string" default="USD">

<div class="product" data-id="#attributes.productId#">
	<h3>#attributes.productName#</h3>
	<bx:if attributes.showPrice>
		<p class="price">#attributes.price# #attributes.currency#</p>
	</bx:if>
</div>
```

Called from the caller template:

```java
<bx:productDisplay productId="42" productName="Coffee Mug" price="12.99">
```

### The variables scope

The `variables` scope is a localized scope for the component's internal logic. Values set here are
private to the component and do not leak into the caller.

```java
<!-- Component: calculator.bxm -->
<bx:param name="attributes.operation" type="string" required="true">
<bx:param name="attributes.a" type="numeric" required="true">
<bx:param name="attributes.b" type="numeric" required="true">

<bx:switch expression="#attributes.operation#">
	<bx:case value="add">
		<bx:set variables.result = attributes.a + attributes.b />
	</bx:case>
	<bx:case value="multiply">
		<bx:set variables.result = attributes.a * attributes.b />
	</bx:case>
	<bx:defaultcase>
		<bx:set variables.result = "Invalid operation" />
	</bx:defaultcase>
</bx:switch>

<div class="calculation-result">
	<p>Result: #variables.result#</p>
</div>
```

### The caller scope

The `caller` scope gives the component access to the variables of the template that called it.
This lets a component read values from, and write values back to, the calling template.

```java
<!-- Component: greeting.bxm -->
<bx:param name="attributes.name" type="string" default="World">

<bx:set caller.greeting = "Hello, " & attributes.name & "!">
<p>#caller.greeting#</p>
```

Called from the caller template, the value written to `caller` is visible afterwards:

```java
<bx:greeting name="Brad">
<bx:output>#greeting#</bx:output>
```

### The thisComponent scope

The `thisComponent` scope manages component execution and content. `thisComponent.executionMode`
is `"start"` while the opening tag is rendering and `"end"` when a body has been provided and the
closing tag is rendering, which lets a component wrap its body content.

```java
<!-- Component: accordion.bxm -->
<bx:param name="attributes.title" type="string" required="true">
<bx:param name="attributes.expanded" type="boolean" default="false">

<bx:if thisComponent.executionMode eq "start">
	<div class="accordion-item">
		<button class="accordion-header" onclick="toggleAccordion(this)">
			#attributes.title#
		</button>
		<div class="accordion-content" style="#attributes.expanded ? '' : 'display:none'#">
</bx:if>

<bx:if thisComponent.executionMode eq "end">
		</div>
	</div>
</bx:if>
```

Called with body content, which is rendered inside the wrapper:

```java
<bx:accordion title="Shipping Details" expanded="true">
	<p>Orders ship within 2 business days.</p>
</bx:accordion>
```
