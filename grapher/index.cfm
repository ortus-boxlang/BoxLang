<cfoutput>
<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" href="./styles.css"/>
    </head>
    <body>
        <script
      type="text/javascript"
      src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"
    ></script>
        <!--- <script type="text/javascript" src="https://cdn.jsdelivr.net/npm/d3@7"></script> --->
        <div id="app">
            <div id="side-panel">
                <h2>AST Files</h2>
                <ul class="file-list">
                    <cfloop array="#directoryList( "./data" )#" index="file">
                        <cfif !file.reFind( 'json$' )>
                            <cfcontinue/>
                        </cfif>
                        <li><a class="fileName" href="##">#listLast( file, '\' )#</a></li>
                    </cfloop>
                </ul>
                <div>
                    <button id="layoutButtonPacked" class="layout-control-button">Packed</button>
                    <button id="layoutButtonWide" class="layout-control-button">Wide</button>
                </div>
            </div>
            <div id="display-panel">
            </div>
        </div>
        <script type="text/javascript" src="./graph-app.js"></script>
    </body>
</html>
</cfoutput>