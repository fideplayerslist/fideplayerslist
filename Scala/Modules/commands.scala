package commands

import scala.io.StdIn.readLine

import parsexml._
import countkeys._

import scala.collection.immutable.ListMap

class CommandInterpreter
{

	var result=""
	
	var finished=false
	
	val explanations= ListMap[String,String](
		"startup" -> "xml + ck",
		"xml" -> "parse XML",
		"ck" -> "count keys",
		"l" -> "list commands",
		"x" -> "exit"
		)
		
	def listcommands_func() { result=(for((k,v)<-explanations) yield "%-8s : %s".format(k,explanations(k))).mkString("\n") }
	def startup_func() { parsexml_func(); countkeys_func() }
	def parsexml_func() { result=new ParseXML("players_list_xml.xml").parse }
	def countkeys_func() { result=new CountKeys("players.txt").count }
	def exit_func() { finished=true }
	
	val commands = Map[String,()=>Unit](
		"l" -> listcommands_func,
		"startup" -> startup_func,
		"xml" -> parsexml_func,
		"ck" -> countkeys_func,
		"x" -> exit_func
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