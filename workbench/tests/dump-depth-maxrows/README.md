# dump() depth / maxRows manual test pages

Manual/visual test pages for the `dump()` / `writeDump()` / `<bx:dump>` `depth` and `maxRows`
parameters (recursion-depth limiting vs. row/item-count limiting), including the CF-compat
`<cfdump top=>` → `depth=value-1` transpiler mapping and Set support.

Each page states what it expects inline, so you can eyeball correctness directly in the browser.

- `index.bxm` - links to all pages below
- `01-depth-struct.bxm` - `depth` at -1/0/1/2/3 on a 3-level nested Struct
- `02-maxrows-struct.bxm` - `maxRows` at -1/0/2 on a flat Struct
- `03-maxrows-array.bxm` - `maxRows` on an Array
- `04-maxrows-query.bxm` - `maxRows` on a Query
- `05-set.bxm` - `depth` / `maxRows` on a `Set` (linked Set + a Set containing a nested Struct)
- `06-combined.bxm` - `depth` + `maxRows` used together
- `07-component-tag.bxm` - `<bx:dump>` tag vs. `writeDump()` BIF parity
- `08-cf-compat.cfm` - real CFML file exercising `<cfdump top=>` and `writeDump(top=)` through
  the CF-compat transpiler

## Running against a local build

`boxlang-miniserver` and `boxlang-web-support` each look for a locally built runtime jar at
`../boxlang/build/libs/boxlang-<version>.jar` relative to themselves (see each project's
`build.gradle`) and use it automatically if present, instead of downloading a published one.
This lets you exercise runtime changes from this checkout before they're published.

That relative path means **your clone of this repo must be at a sibling directory literally
named `boxlang`** (lowercase) alongside the other two - rename/move it or add a symlink
(`ln -s /path/to/this/checkout ../boxlang`) if it's checked out under a different name.

```bash
# Directory layout expected by the sibling projects:
#   somewhere/boxlang/               <- this repo (must be named exactly "boxlang")
#   somewhere/boxlang-web-support/
#   somewhere/boxlang-miniserver/

# 1. Build this repo's runtime jar
cd boxlang
./gradlew shadowJar -x test

# 2. Clone the sibling projects (once), next to this checkout
cd ..
git clone https://github.com/ortus-boxlang/boxlang-web-support
git clone https://github.com/ortus-boxlang/boxlang-miniserver

# 3. Build boxlang-web-support and boxlang-miniserver - both will find and use
#    ../boxlang/build/libs/boxlang-<version>.jar automatically
cd boxlang-web-support && ./gradlew shadowJar jar -x test && cd ..
cd boxlang-miniserver && ./gradlew shadowJar -x test && cd ..

# 4. Run the MiniServer against this test folder
java -jar boxlang-miniserver/build/distributions/boxlang-miniserver-*.jar \
    --port 8085 --webroot boxlang/workbench/tests/dump-depth-maxrows

# 5. Open http://localhost:8085/index.bxm
```
