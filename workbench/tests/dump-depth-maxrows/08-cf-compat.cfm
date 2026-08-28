<cfscript>
	nested = {
		"alpha": "a",
		"beta" : {
			"charlie": "c",
			"delta": "d"
		},
		"echo" : {
			"foxtrot" : {
				"golf" : "g",
				"hotel" : "h"
			}
		}
	};
</cfscript>
<!DOCTYPE html>
<html>
<head><title>08 - CF-compat cfdump top= transpiled to depth</title>
<style>body{font-family:system-ui,sans-serif;max-width:1000px;margin:30px auto;padding:0 20px;} h2{border-bottom:2px solid #333;padding-bottom:4px;margin-top:40px;} .expect{background:#fffbe6;border-left:4px solid #e0a800;padding:8px 12px;margin:8px 0;}</style>
</head>
<body>
<p><a href="index.bxm">&larr; back</a></p>
<h1>08 - CF-compat: &lt;cfdump top=&gt; transpiled to depth=value-1</h1>
<p>This is a real <code>.cfm</code> file, so it goes through the CF-compat transpiler. Same fixture as page 01.</p>

<h2>&lt;cfdump var="#nested#" top="1"&gt;</h2>
<div class="expect">top=1 -&gt; depth=0. Expect: "Depth Limit reached" only.</div>
<cfdump var="#nested#" top="1">

<h2>&lt;cfdump var="#nested#" top="3"&gt;</h2>
<div class="expect">top=3 -&gt; depth=2. Expect: charlie/delta/foxtrot visible, golf/hotel NOT visible (same as depth=2 on page 01).</div>
<cfdump var="#nested#" top="3">

<h2>writeDump( var=nested, top=3 ) as a cfscript BIF call</h2>
<div class="expect">Same top=3 -&gt; depth=2 mapping, via the writeDump() BIF instead of the tag.</div>
<cfoutput>#writeDump( var = nested, top = 3, output = "buffer", format = "html" )#</cfoutput>

</body>
</html>
