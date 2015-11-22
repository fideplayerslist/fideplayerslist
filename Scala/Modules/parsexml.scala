package parsexml

import java.io._

import scala.io.Source

import scala.xml.pull._

import utils.Timer

import utils.Parse._

import globals.Globals._

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

		println("parsing "+path)

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
							println("processed %8d , elapsed %5.0f , rate %5.0f".format(cnt,elapsed,rate))
						}

					}
					else
					{
						if((current_tag!="")&&(current_value!=""))
						{
							
							if(current_tag=="country")
							{
								current_value=current_value.toUpperCase
							}

							var additional_field=""

							if(current_tag=="rating")
							{
								if(isInt(current_value))
								{
									val rating=current_value.toInt
									var rr=((rating/rating_refinement).toInt)*rating_refinement

									additional_field="rr\t"+rr+"\t"
								}
							}

							val normal_field=current_tag+"\t"+current_value

							val actual_field=additional_field+normal_field

							if(current_record=="")
							{
								current_record=actual_field
							}
							else
							{
								current_record=current_record+"\t"+actual_field
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

		println("parsing %s ok, total number of records %d".format(path,cnt))
		
		writer.close()

		"Done."

	}
	
}