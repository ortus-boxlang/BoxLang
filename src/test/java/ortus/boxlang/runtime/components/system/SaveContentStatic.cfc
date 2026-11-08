component {
    function run() {
        static html = savecontent { writeOutput( "static content" ); }
        return static.html;
    }
}
