package countkeys

import java.io._

import scala.io.Source

import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer

import utils.Timer
import utils.Dir._
import utils.HeapSize._

import globals.Globals._

class CountKeys(path: String)
{

	def count():String =
	{

		val lines=(readTxtLines("players.txt"))

		var keycounts=Map[String,Int]()

		var detailed_keycounts=Map[String,Map[String,Int]]()

		var cnt=0

		for(line<-lines)
		{

			cnt=cnt+1

			val fields=line.split("\t")

			var key:String=""
			var value:String=""	

			for(field<-fields)
			{
				if(key=="")
				{
					key=field
				}
				else
				{
					value=field

					if(keycounts.contains(key))
					{
						keycounts+=(key->(keycounts(key)+1))
					}
					else
					{
						keycounts+=(key->1)
					}

					if((key!="name")&&(key!="fideid"))
					{

						var keycount=Map[String,Int]()

						if(detailed_keycounts.contains(key))
						{
							keycount=detailed_keycounts(key)
						}

						if(keycount.contains(value))
						{
							keycount+=(value->(keycount(value)+1))
						}
						else
						{
							keycount+=(value->1)
						}

						detailed_keycounts+=(key->keycount)
					}


					key=""
					value=""
				}

			}

			if((cnt%100000)==0)
			{
				println("counted %d , %s".format(cnt,heapsize))

				for((k,v)<-keycounts)
				{
					println("%-20s : %d".format(k,v))
				}
			}

		}

		println("total counted %d , %s".format(cnt,heapsize))

		mkdir("stats")

		deleteFilesInDir("stats")

		mkdir("stats/keycounts")

		deleteFilesInDir("stats/keycounts")

		val writer=new PrintWriter(new File("keycounts.txt"))

		for((k,v)<-keycounts)
		{

			deleteFilesInDir("stats/keycounts/"+k)

			(new File("stats/keycounts/"+k)).delete

			if(detailed_keycounts.contains(k))
			{

				val dwriter=new PrintWriter(new File("stats/keycounts/"+k+".txt"))

				val sorted_detailed_keycount=ListMap(detailed_keycounts(k).toSeq.sortWith(_._1 < _._1):_*)

				for((dk,dv)<-sorted_detailed_keycount)
				{
					dwriter.write("%s\t%d\n".format(dk,dv))
				}

				dwriter.close

				if((detailed_keycounts(k).keys.toList.length<5000)&&(collected_keys.contains(k)))
				{

					mkdir("stats/keycounts/"+k)

					deleteFilesInDir("stats/keycounts/"+k)

					println("collecting "+k)

					def collectkey(key: String)
					{
				
						var valuebuffs=Map[String,ArrayBuffer[String]]()
						
						for(line<-lines)
						{

							var fkey=""
							var fvalue=""

							val fields=line.split("\t")

							var value_for_key=""
							var rating=""

							for(field<-fields)
							{
								if(fkey=="")
								{
									fkey=field
								}
								else
								{
									fvalue=field
									if(fkey==key)
									{
										value_for_key=fvalue
									}
									else if(fkey=="rating")
									{
										rating=fvalue
									}
									fkey=""
									fvalue=""
								}
							}
							
							val extline="%10s".format(rating)+"\t"+line
							if(valuebuffs.contains(value_for_key))
							{
								valuebuffs(value_for_key)+=extline
							}
							else
							{
								valuebuffs+=(value_for_key->ArrayBuffer[String](extline))
							}
						}
						
						deleteFilesInDir("stats/keycounts/"+key)
						
						for((k,v)<-valuebuffs)
						{

							println("value %s has %d players".format(k,v.length))

							val path="stats/keycounts/"+key+"/"+k+".txt"

							//sorted
						
							val content=v.sorted.reverse.mkString("\n")+"\n"
							println("writing sorted list for value: %s , %s".format(k,heapsize))
							saveTxt(path,content)

							//unsorted

							/*println("writing list for value: %s , %s".format(k,heapsize))

							val writer=new PrintWriter(new File(path))
	
							for(line<-v)
							{
								writer.write(line+"\n")
							}

							writer.close()*/
							
						}
						
					}
					
					collectkey(k)

				}

			}

			println("%-20s : %d".format(k,v))
			writer.write("%s\t%d\n".format(k,v))
		}

		writer.close

		"Done."
	}

}