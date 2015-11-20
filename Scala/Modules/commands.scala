package commands

import scala.io.StdIn.readLine

import parsexml._
import countkeys._

class CommandInterpreter
{

	var result=""
	
	var finished=false
	
	val explanations= Map[String,String](
		"l" -> "list commands",
		"xml" -> "parse XML",
		"ck" -> "count keys",
		"x" -> "exit"
		)
		
	def listcommands() { result=(for((k,v)<-explanations) yield "%-8s : %s".format(k,explanations(k))).mkString("\n") }
	def parsexml() { result=new ParseXML("players_list_xml.xml").parse }
	def count_keys() { result=new CountKeys("players.txt").count }
	def exit() { finished=true }
	
	val commands = Map[String,()=>Unit](
		"l" -> listcommands,
		"xml" -> parsexml,
		"ck" -> count_keys,
		"x" -> exit
		)
	
	do
	{

		val input=readLine((if(result!="") result+"\n" else "")+"enter command> ")
		
		result=""
		
		if(commands.contains(input))
		{
			commands(input)()
		}
		else
		{
			result="unknown command"
		}
		
	
	}while(!finished)
	
}