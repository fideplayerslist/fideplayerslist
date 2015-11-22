package commands

import scala.io.StdIn.readLine

import parsexml._
import countkeys._
import stats._
import keystats._
import youngtalents._

import scala.collection.immutable.ListMap

import utils.Timer

class CommandInterpreter
{

	var result=""
	
	var finished=false

	class Command(set_explanation:String, set_func:()=>Unit){val explanation=set_explanation;val func=set_func}

	def listcommands_func() { result=(for((k,v)<-commands) yield "%-8s : %s".format(k,v.explanation)).mkString("\n") }
	def startup_func()
	{

		val timer=new Timer

		parsexml_func()
		countkeys_func()
		create_stats_func()
		key_stats_func()
		young_talents_func()

		println("startup ok, elapsed %.0f".format(timer.elapsed))


		result="Done."
	}
	def parsexml_func() { result=new ParseXML("players_list_xml.xml").parse }
	def countkeys_simple_func() { result=new CountKeys("players.txt").count(true) }
	def countkeys_func() { result=new CountKeys("players.txt").count(false) }
	def create_stats_func() { result=new Stats().create() }
	def key_stats_func() { result=new KeyStats().create() }
	def young_talents_func() { result=new YoungTalents().create() }
	def exit_func() { finished=true }

	val commands=ListMap[String,Command](
		"startup"->new Command("xml + ck + s + k + yt",startup_func),
		"xml"->new Command("parse XML",parsexml_func),
		"cks"->new Command("count keys simple",countkeys_simple_func),
		"ck"->new Command("count keys",countkeys_func),
		"s"->new Command("create stats",create_stats_func),
		"k"->new Command("key stats",key_stats_func),
		"yt"->new Command("young talents",young_talents_func),
		"l"->new Command("list commands",listcommands_func),
		"x"->new Command("exit",exit_func)
	)
	
	do
	{

		val input=readLine((if(result!="") result+"\n" else "")+"enter command> ")
		
		result=""
		
		if(commands.contains(input))
		{
			commands(input).func()
		}
		else
		{
			result="unknown command"
		}
		
	
	}while(!finished)
	
}