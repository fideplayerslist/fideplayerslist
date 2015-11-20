package parsexml

import java.io._
import java.lang.System

import scala.io.Source
import scala.xml.pull._

import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer

class Dir
{

	def mkdirs(path: List[String]) = // return true if path was created
		path.tail.foldLeft(new File(path.head)){(a,b) => a.mkdir; new File(a,b)}.mkdir
		
	def mkdir(path: String) = // create single dir
		mkdirs(List(path))

	def getListOfFiles(dir: String):List[File] =
	{
		val d = new File(dir)
		if (d.exists && d.isDirectory)
		{
			d.listFiles.filter(_.isFile).toList
		}
		else
		{
			List[File]()
		}
	}
	
	def getListOfFileNames(dir: String):List[String] =
		for(f<-getListOfFiles(dir)) yield f.getName
		
	def deleteFilesInDir(dir: String)
	{
		for(f<-getListOfFiles(dir)) f.delete
	}

}

class Timer
{
	var t0=System.nanoTime()
	def elapsed:Double = (System.nanoTime() - t0)/1.0e9
}

class HeapSize
{
	def heapsize = "heap size "+Runtime.getRuntime().totalMemory()/1000000
}

class ReadTxtLines(path: String)
{
	def lines:Array[String]=
	{
		val timer=new Timer
		println("reading lines of %s , %s".format(path,(new HeapSize).heapsize))
		val lines=Source.fromFile(path).getLines().toArray
		println("number of lines %d , elapsed %f , %s".format(lines.length,timer.elapsed,(new HeapSize).heapsize))
		lines
	}
}

class CountKeys(path: String)
{

	def count():String =
	{

		val lines=(new ReadTxtLines("players.txt")).lines

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
				println("counted %d , %s".format(cnt,(new HeapSize).heapsize))

				for((k,v)<-keycounts)
				{
					println("%-20s : %d".format(k,v))
				}
			}

		}

		println("total counted %d , %s".format(cnt,(new HeapSize).heapsize))

		val dir=new Dir

		dir.mkdir("stats")

		dir.deleteFilesInDir("stats")

		dir.mkdir("stats/keycounts")

		dir.deleteFilesInDir("stats/keycounts")

		val writer=new PrintWriter(new File("keycounts.txt"))

		for((k,v)<-keycounts)
		{

			(new File("stats/keycounts/"+k)).delete

			if(detailed_keycounts.contains(k))
			{

				dir.mkdir("stats/keycounts/"+k)

				dir.deleteFilesInDir("stats/keycounts/"+k)

				val dwriter=new PrintWriter(new File("stats/keycounts/"+k+".txt"))

				val sorted_detailed_keycount=ListMap(detailed_keycounts(k).toSeq.sortWith(_._1 < _._1):_*)

				for((dk,dv)<-sorted_detailed_keycount)
				{
					dwriter.write("%s\t%d\n".format(dk,dv))
				}

				dwriter.close

			}

			println("%-20s : %d".format(k,v))
			writer.write("%s\t%d\n".format(k,v))
		}

		writer.close

		"Done."
	}

}

class ParseXML(path: String)
{

	// maximum number of records to be processed
	val MAXCNT=1000000

	def parse():String =
	{

		val xml = new XMLEventReader(Source.fromFile(path))

		var current_tag=""
		var current_value=""

		var cnt=0

		println("Parse XML "+path)

		var current_record=""

		val timer=new Timer

		val writer=new PrintWriter(new File("players.txt"))

		while((xml.hasNext) && (cnt<MAXCNT))
		{

			xml.next match
			{
			
				case EvElemStart(_, label, _, _) =>
				
					current_tag=label
				
				case EvElemEnd(_, label) =>

					if(label=="player")
					{

						cnt=cnt+1

						val content=current_record+"\n"

						writer.write(content)

						current_record=""

						if(cnt%1000==0)
						{
							val elapsed=timer.elapsed
							val rate=cnt/elapsed
							println("processed %8d , elapsed %5.0f , rate %5.0f , %s ".format(cnt,elapsed,rate,(new HeapSize).heapsize))
						}

					}
					else
					{
						if((current_tag!="")&&(current_value!=""))
						{
							if(current_record=="")
							{
								current_record=current_tag+"\t"+current_value
							}
							else
							{
								current_record=current_record+"\t"+current_tag+"\t"+current_value
							}
						}
					}

					current_tag=""
					current_value=""

				case EvText(text) =>
						
					if((text!="")&&(current_tag!=""))
					{
						current_value=text
					}
				
				case _ => 

					//default

			}

		}

		println("ok, total number of records "+cnt)
		
		writer.close()

		"Done."

	}
	
}