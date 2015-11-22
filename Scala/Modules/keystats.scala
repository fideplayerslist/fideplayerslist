package keystats

import utils.Dir._
import utils.Parse._
import globals.Globals._

class KeyStats
{

	def create():String =
	{

		mkdir("stats/keystats")
		deleteFilesInDir("stats/keystats")
		
		for(filter<-filters)
		{
		
			for(key<-collected_keys)
			//for(key<-List("title"))
			{
				println("Key stats for "+key+". Filter - "+filter)

				val keydir="stats/keystats/"+key

				mkdir(keydir)
				deleteFilesInDir(keydir)
				
				val filterdir=keydir+"/"+filter

				mkdir(filterdir)
				deleteFilesInDir(filterdir)

				val statsdir="stats/stats/"+key+"/"+filter
				
				var lines=Array[Array[String]]()
				
				for(subkey_filename<-getListOfFileNames(statsdir))
				{

					val subkey_filename_parts=subkey_filename.split("\\.")

					if((subkey_filename_parts.length==2)&&(subkey_filename_parts(1)=="txt"))
					{

						val subkey=subkey_filename_parts(0)
						
						println("Key stats: "+subkey)

						val spath=statsdir+"/"+subkey_filename
						
						var linearray:Array[String]=(for(record<-parseTxtSmart(spath)) yield record("value")).toArray
						linearray=subkey+:linearray
						lines=lines:+(linearray)

					}
				}
				
				def create_by_sortindex(sortindex: Int, bywhat: String)
				{
					lines=lines.sortWith((leftE,rightE) => myToFloat(leftE(sortindex)) > myToFloat(rightE(sortindex)))
					
					var content=key+"\t"+keystat_fields.mkString("\t")+"\n"
					
					content=content+(for(line<-lines) yield (line.mkString("\t")+"\n")).mkString("")
					
					val tpath="stats/keystats/"+key+"/"+filter+"/"+bywhat+".txt"
					
					saveTxt(tpath,content)
					
					htmlify(tpath)
				}
				
				if(key=="birthday") create_by_sortindex(0,"by"+key)
				create_by_sortindex(keystat_indices("PARF"),"byparf")
				create_by_sortindex(keystat_indices("PARFR"),"byparfr")
				create_by_sortindex(keystat_indices("AVGRF"),"byavgrf")
				create_by_sortindex(keystat_indices("AVGRM"),"byavgrm")
				create_by_sortindex(keystat_indices("AVGR"),"byavgr")
				create_by_sortindex(keystat_indices("ALL"),"byall")
				create_by_sortindex(keystat_indices("R"),"byrated")
				create_by_sortindex(keystat_indices("RM"),"byratedm")
				create_by_sortindex(keystat_indices("RF"),"byratedf")
				
			}
			
		}
		
		"Done."
		
	}

}