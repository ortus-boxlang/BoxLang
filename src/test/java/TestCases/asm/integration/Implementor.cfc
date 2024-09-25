component implements="ICacheProvider" {
    
    variables.name = "";

    function getName(){
        return variables.name;
    }
    
	function setName( required name ){
        variables.name = name;
    }
}