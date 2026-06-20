# Tutorial — Build an Items app (twice)

We'll build the same small **list + detail + add** app two ways: the WebView/MVC track and
the Compose track. Both share the same `Application.bx` mental model and handlers-first flow.

## Part A — WebView / MVC track

### 1. Routes (`Application.bx`)

```java
class {
    this.name = "Items";
    function configureRouter( router ) {
        router.setDefaultEvent( "Main.index" );
        router.get( "/items" ).withName( "items" ).to( "Items.list" );
        router.get( "/items/:id" ).to( "Items.show" );
        router.post( "/items/add" ).to( "Items.add" );
    }
    function onApplicationStart() {
        application.items = [ "Apple", "Banana", "Cherry" ];
        return true;
    }
}
```

### 2. Handler (`handlers/Items.bx`) — actions run first

```java
class {
    function list( event, rc ) {
        event.setValue( "items", application.items );
        event.setView( "items/list" );
    }
    function show( event, rc, id ) {
        event.setValue( "item", application.items[ id ] );
        event.setView( "items/detail" );
    }
    function add( event, rc, title ) {
        application.items.append( title );
        event.getFlash().put( "message", "Added: " & title );
        event.relocate( "/items" );          // post-redirect-get
    }
}
```

### 3. Layout + views

```html
<!-- layouts/main.bxm -->
<bx:output><html><body>
  <bx:if flash.exists( "message" )><p class="flash">#flash.get( "message" )#</p></bx:if>
  <main>#renderedView#</main>
</body></html></bx:output>

<!-- views/items/list.bxm -->
<bx:output>
  <bx:loop from="1" to="#rc.items.len()#" index="i">
    <div><a href="/items/#i#">#rc.items[ i ]#</a></div>
  </bx:loop>
  <form method="post" action="/items/add">
    <input name="title"><button type="submit">Add</button>
  </form>
</bx:output>

<!-- views/items/detail.bxm -->
<bx:output><h2>#rc.item#</h2><a href="/items">Back</a></bx:output>
```

### 4. Run

```bash
./gradlew :app:installDebug
```

Tap an item → `Items.show` renders the detail. Submit the form → `Items.add` runs, stages a
flash message, and relocates to `/items`, where the list re-renders with the new item and the
flash banner. All in-process — no server.

> **How it works:** the JS bridge captures the POST form and calls into the runtime; link taps
> are intercepted and routed. Both go through the same `Application.bx` request lifecycle.

## Part B — Compose track

Same data, native widgets. Author the UI as a BoxLang tree.

### 1. `handlers/Main.bx`

```java
class {
    function index() { return new ItemsScreen().build(); }
}
```

### 2. `ItemsScreen.bx`

```java
class {
    variables.UI = createObject( "java", "ortus.boxlang.runtime.android.ui.UI" );
    variables.render = createObject( "java", "ortus.boxlang.runtime.android.ComposeBridge$RenderState" );

    function build() {
        var col = variables.UI.column().child( variables.UI.text( "Items" ).prop( "size", 24 ) );
        for ( var name in application.items ) {
            col.child( variables.UI.text( "• " & name ) );
        }
        col.child(
            variables.UI.button( "Add Durian" ).on( "onClick", () => {
                application.items.append( "Durian" );
                variables.render.requestRender();
            } )
        );
        return col;
    }
}
```

The thin Kotlin host (`MainActivity`) invokes `Main.index()` and hands the tree to the Compose
renderer; tapping the button mutates BoxLang state and requests recomposition.

## Limitations & gotchas

- **AOT only.** No `eval`/runtime class generation on device — compile `.bx`/`.bxm` at build
  time. (On-device class loading depends on the *preloaded boxpiler* runtime item — see the
  proposal.)
- **Disabled services:** file watchers, file-watch schedulers, JDBC/Hikari — use SQLite/Room.
- **R8:** keep BoxLang + `boxgenerated.*` + ServiceLoader providers (rules provided).
- **POST bodies** must use the JS bridge (Android can't read POST bodies in
  `shouldInterceptRequest`); the runtime injects the hook automatically.
- **Threading:** WebView JS callbacks run off the UI thread — the renderer marshals back via
  `webView.post(...)`. Keep heavy work off the main thread.
