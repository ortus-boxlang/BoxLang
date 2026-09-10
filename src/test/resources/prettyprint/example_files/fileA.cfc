/**
 * My File A
 *
 * @package some.package.name
 * @author  Joe Schmoe <jschmoe@example.com>
 */
component
	extends  ="SomeBaseComponent"
	delegates="xxx@what,foo@bar,others"
	accessors="true"
	singleton
{

	// Properties
	property name="hyperBuilder" inject="HyperBuilder@hyper";
	property name="logger"       inject="logbox:logger:{this}";
	property name="settings";


	/**
	 * Constructor
	 */
	fileA function init(){
		super.init();
		variables.settings = {
			"seefsef"      : "val1",
			"sfsefessf"     : "val2",
			"ddddddd"    : "val3",
			"ddddd"     : "val4",
			"aaaa" : "val5",
			"ffff" : "val6",
			"eeeeee" : 60,
			"bbbbbb" : 60,
			// important data
			"sseefsefsef"     : "EFEFEFEEF"
		};
		return this;
	}

	function onDIComplete(){
		structAppend(
			variables.settings,
			{
				"Setting_A"      : getSystemSetting( "NAMESPACE_SETTING_A" ),
				"Setting_B"      : getSystemSetting( "NAMESPACE_SETTING_B" ),
				"Setting_C"      : getSystemSetting( "NAMESPACE_SETTING_C" ),
				"Setting_D"           : getSystemSetting( "NAMESPACE_SETTING_D" ),
				"Setting_E"         : getSystemSetting( "NAMESPACE_SETTING_E" ),
				"Setting_F"       : getSystemSetting( "NAMESPACE_SETTING_F" ),
				"Setting_G"    : getSystemSetting( "NAMESPACE_SETTING_G", "G_DEFAULT" ),
				"Setting_H" : getSystemSetting( "NAMESPACE_SETTING_H" ),
				"Setting_I" : getSystemSetting( "NAMESPACE_SETTING_I", "I_DEFAULT" ),
				"Setting_J"     : getSystemSetting( "NAMESPACE_SETTING_J" ),
				"Setting_K"     : getSystemSetting( "NAMESPACE_SETTING_K" )
			},
			true
		);

		variables.responseCodes = {
			"superCategory" : {
				"B" : "bAAAAA",
				"D" : "daaa",
				"I" : "Iaaaaa",
				"O" : "OAAAAA",
			}
		};

		structAppend(
			variables.someData,
			{
				"aaa"                  : true,
				"bbb"     : true,
				"ccc"            : true,
				"ddd" : false,
				"eee"             : true,
				"fff"             : true,
				"ggg"             : true,
				"hhhhhh"            : true,
				"sefsefsefsefsef"           : true,
				"sefse"            : true,
				"sefsefsefse"        : true,
				"ergegergerg"        : true,
				"wergfwefgwefw"         : false,
				"wefwefe"    : false,
				"rthrthrthrt"       : 4,
				"rthrthtrh"           : {
					"qqqqqwqe"   : true,
					"rrrrrg"     : true,
					"gffgfdgdfg" : true,
					"sdddddddddd"     : false,
					"dddddd"  : true,
					"eeeeeee"     : false,
					"eeeeee"        : true,
					"eeeefwfw"  : 55,
					// this will be populated by the super DI completion
					"sefsefsef"   : []
				},
				"aaaaaaaa" : [
					{
						"yyyyyyyyyyyyyyy"  : { "tyyyyy" : { "lte" : 4 } },
						"llllllllll" : 75
					},
					{
						"yyyyyyyyyyyyyyy"  : { "tyyyyy" : { "gte" : 5, "lte" : 14 } },
						"llllllllll" : 90
					},
					{
						"yyyyyyyyyyyyyyy"  : { "tyyyyy" : { "gte" : 15 } },
						"llllllllll" : 120
					}
				]
			},
			true
		);

		super.onDIComplete();

	}
}