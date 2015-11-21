package stats

import java.io._

import utils.Dir._

import globals.Globals._

import record._

import scala.collection.mutable.ArrayBuffer

import scala.io.StdIn.readLine

class Stats
{
	def create():String =
	{
		println("creating stats")

		mkdirs(List("stats","stats"))

		deleteFilesInDir("stats/stats")

		for(key<-collected_keys)
		//for(key<-List("title"))
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

						val lines=readTxtLines(keycountdir+"/"+filename)

						class Stat(set_path:String)
						{
							var ALL:Int=0
							var M:Int=0
							var F:Int=0
							var MF:Int=0
							val path=set_path
						}

						var statsmap=scala.collection.mutable.Map[String,Stat]()

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

								val stat=statsmap(filter)

								val record=new Record(line)

								var cond=true

								if(filter.contains("m"))
								{
									if(!record.hasAge)
									{
										cond=false
									}
									else
									{
										cond=(record.age>=20)&&(record.age<=40)
									}
								}

								if(filter.contains("a"))
								{
									if(record.inActive)
									{
										cond=false
									}
								}

								if(cond)
								{

									stat.ALL=stat.ALL+1

									if(record.hasSex)
									{
										stat.MF=stat.MF+1
										if(record.sex=="M")
										{
											stat.M=stat.M+1
										}
										else
										{
											stat.F=stat.F+1
										}
									}

								}

								statsmap(filter)=stat
							}
						}

						for(filter<-filters)
						{
							val stat=statsmap(filter)
							var content="ALL\t"+stat.ALL+"\nM\t"+stat.M+"\nF\t"+stat.F+"\nMF\t"+stat.MF+"\n"

							saveTxt(stat.path,content)
							htmlify(stat.path)
						}

					}
				}
			}
		}

		"Done."
	}
}