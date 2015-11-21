package stats

import java.io._

import utils.Dir._

import globals.Globals._

import scala.collection.mutable.ArrayBuffer

class Stats
{
	def create():String =
	{
		println("creating stats")

		mkdirs(List("stats","stats"))

		deleteFilesInDir("stats/stats")

		for(key<-collected_keys)
		{
			println("creating stats for "+key)

			val keydir="stats/stats/"+key

			mkdir(keydir)
			deleteFilesInDir(keydir)

			for(filter<-filters)
			{
				val filterdir=keydir+"/"+filter

				mkdir(filterdir)
				deleteFilesInDir(filterdir)
			}

			val keycountdir="stats/keycounts/"+key

			val filenames=getListOfFileNames(keycountdir)

			for(filename<-filenames)
			{
				val filenamefields=filename.split("\\.")

				if(filenamefields.length==2)
				{
					if(filenamefields(1)=="txt")
					{
						val value=filenamefields(0)

						println("value : "+value)

						val lines=readTxtLinesVerbose(keycountdir+"/"+filename)

						class Stat(set_path:String)
						{
							var ALL:Int=0
							val path=set_path
						}

						var statsmap=Map[String,Stat]()

						for(filter<-filters)
						{
							val filterdir=keydir+"/"+filter
							val filterpath=filterdir+"/"+filename
							statsmap+=(filter->new Stat(filterpath))
						}

						for(line<-lines)
						{
							for(filter<-filters)
							{
								statsmap(filter).ALL=statsmap(filter).ALL+1
							}
						}

						for(filter<-filters)
						{
							val stat=statsmap(filter)
							var content="ALL\t"+stat.ALL+"\n"

							saveTxt(stat.path,content)
						}

					}
				}
			}
		}

		"Done."
	}
}