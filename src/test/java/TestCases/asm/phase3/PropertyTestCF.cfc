component accessors=true {
    property name="myProperty" default="myDefaultValue" type=string inject preAnno;
    
    /**
     * This is my property
     * @brad wood
     * @luis
     */
    property string anotherprop preanno=["myValue", "anothervalue"];

    property shortcutWithDefault default="myDefaultValue";

    property String typedShortcutWithDefault default="myDefaultValue2";

    function init() {
        getMyProperty();
    }
}