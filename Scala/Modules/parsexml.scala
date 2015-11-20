package parsexml

import java.io._
import java.lang.System

import scala.io.Source
import scala.xml.pull._

import scala.collection.mutable.ArrayBuffer

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


					key=""
					value=""
				}

			}

			if((cnt%200000)==0)
			{
				println("counted %d , %s".format(cnt,(new HeapSize).heapsize))

				for((k,v)<-keycounts)
				{
					println("%-20s : %d".format(k,v))
				}
			}

		}

		println("total counted %d , %s".format(cnt,(new HeapSize).heapsize))

		val writer=new PrintWriter(new File("keycounts.txt"))

		for((k,v)<-keycounts)
		{
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