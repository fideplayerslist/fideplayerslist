package record

import utils.Parse._
import globals.Globals._

class Record(line: String)
{

	var country_str:String=""
	var hasCountry:Boolean=false
	var birthday:Int=0
	var birthday_str:String=""
	var hasBirthday:Boolean=false
	var age:Int=0
	var age_str:String=""
	var hasAge:Boolean=false
	var rating:Int=0
	var rating_str:String=""
	var hasRating:Boolean=false
	var sex:String=""
	var sex_str:String=""
	var hasSex:Boolean=false
	var flag_str:String=""
	var inActive:Boolean=false

	var key=""
	var value=""

	for(field<-line.split("\t").tail)
	{

		if(key=="")
		{
			key=field
		}
		else
		{
			value=field

			if(key=="sex")
			{
				sex_str=value
				if((value=="M")||(value=="F"))
				{
					hasSex=true
					sex=value
				}
			}
			else if(key=="rating")
			{
				rating_str=value
				if(isInt(value))
				{
					hasRating=true
					rating=value.toInt
				}
			}
			else if(key=="birthday")
			{
				birthday_str=value
				if(isInt(value))
				{
					hasBirthday=true
					birthday=value.toInt

					age=REFERENCE_YEAR-birthday
					hasAge=true
				}
			}
			else if(key=="country")
			{
				country_str=value
				if(value!="")
				{
					hasCountry=true
				}
			}
			else if(key=="flag")
			{
				flag_str=value
				if(value.contains("i"))
				{
					inActive=true
				}
			}

			key=""
			value=""
		}
	}
}