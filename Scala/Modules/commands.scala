package commands

import scala.io.StdIn.readLine

import parsexml.ParseXML

class CommandInterpreter
{

	var result="welcome"
	
	var finished=false
	
	val explanations= Map[String,String](
		"l" -> "list commands",
		"xml" -> "parse XML",
		"x" -> "exit"
		)
	
	val commands = Map[String,()=>Unit](
		"l" -> (()=>(result=(for((k,v)<-explanations) yield k+" : "+explanations(k)).mkString("\n"))),
		"xml" -> (()=>(new ParseXML("players.txt"))),
		"x" -> (()=>(finished=true))
		)
	
	do
	{

		val input=readLine(result+"\nenter command> ")
		
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