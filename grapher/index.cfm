<cfoutput>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="./styles.css"/>
    </head>
    <body>
        <script type="text/javascript" src="https://cdn.jsdelivr.net/npm/d3@7"></script>
        <div id="app">
            <div id="side-panel">
                <h2>AST Files</h2>
                <ul class="file-list">
                    <cfloop array="#directoryList( "./data" )#" index="file">
                        <li><a class="fileName" href="##">#listLast( file, '\' )#</a></li>
                    </cfloop>
                </ul>
            </div>
            <div id="display-panel">
            </div>
        </div>
        <script type="text/javascript" src="./graph-app.js"></script>
    </body>
</html>
</cfoutput>