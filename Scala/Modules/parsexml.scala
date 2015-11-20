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

class ParseXML(path: String)
{

	def heapsize = "heap size "+Runtime.getRuntime().totalMemory()/1000000

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
							println("processed %8d , elapsed %5.0f , rate %5.0f , %s ".format(cnt,elapsed,rate,heapsize))
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