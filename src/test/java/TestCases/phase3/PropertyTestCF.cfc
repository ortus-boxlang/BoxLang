component accessors=true {
    property name="myProperty" default="myDefaultValue" type=string inject preAnno;
    
    /**
     * This is my property
     * @brad wood
     * @luis
     */
    property string anotherprop preanno=["myValue", "anothervalue"];

    function init() {
        getMyProperty();
    }
}