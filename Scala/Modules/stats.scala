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
							var R:Int=0
							var CR:Double=0.0
							var RM:Int=0
							var CRM:Double=0.0
							var RF:Int=0
							var CRF:Double=0.0
							var RMF:Int=0
							var CRMF:Double=0.0
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

									if(record.hasRating)
									{
										stat.R=stat.R+1
										stat.CR=stat.CR+record.rating
									}

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
										if(record.hasRating)
										{
											stat.RMF=stat.RMF+1
											stat.CRMF=stat.CRMF+record.rating
											if(record.sex=="M")
											{
												stat.RM=stat.RM+1
												stat.CRM=stat.CRM+record.rating
											}
											else
											{
												stat.RF=stat.RF+1
												stat.CRF=stat.CRF+record.rating
											}
										}
									}

								}

								statsmap(filter)=stat
							}
						}

						for(filter<-filters)
						{
							val stat=statsmap(filter)

							def PERCENT(cnt: Double, den: Double):String =
								if(den!=0) "%.2f".format(cnt/den*100) else "NA"

							def AVERAGE(cnt: Double, den: Double):String =
								if(den!=0) "%.2f".format(cnt/den) else "NA"

							var content=
								"key\tvalue"+
								"\nALL\t"+stat.ALL+
								"\nR\t"+stat.R+
								"\nCR\t"+stat.CR+
								"\nAVGR\t"+AVERAGE(stat.CR,stat.R)+
								"\nMF\t"+stat.MF+
								"\nRMF\t"+stat.RMF+
								"\nCRMF\t"+stat.CRMF+
								"\nAVGRMF\t"+AVERAGE(stat.CRMF,stat.RMF)+
								"\nM\t"+stat.M+
								"\nRM\t"+stat.RM+
								"\nCRM\t"+stat.CRM+
								"\nAVGRM\t"+AVERAGE(stat.CRM,stat.RM)+
								"\nF\t"+stat.F+
								"\nPARF\t"+PERCENT(stat.F,stat.MF)+
								"\nRF\t"+stat.RF+
								"\nPARFR\t"+PERCENT(stat.RF,stat.RMF)+
								"\nCRF\t"+stat.CRF+
								"\nAVGRF\t"+AVERAGE(stat.CRF,stat.RF)+
								"\n"

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