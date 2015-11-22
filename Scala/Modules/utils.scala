package utils

import java.io._
import java.lang.System

import scala.io.Source

class Timer
{
	var t0=System.nanoTime()
	def elapsed:Double = (System.nanoTime() - t0)/1.0e9
}

object HeapSize
{
	def heapsize = "heap size "+Runtime.getRuntime().totalMemory()/1000000
}

object Parse
{

	def strip(content: String):String=
		content.replaceAll("[\r\n]","")

	def myToFloat(what: String):Float =
	{
		if((what=="NA")||(what=="")||(what=="0"))
		{
			return 0.toFloat
		}
		val whats=what.replaceAll("^0+","")
		val floatmatch=""",[0-9]{2}$""".r.unanchored
		whats match
			{
				case floatmatch(_*) => return whats.split(",").mkString.toFloat/100
				case _ =>
			}
		whats.toFloat
	}

	def isInt(what: String):Boolean =
	{
		try
		{
			val i=what.toInt
		}
		catch
		{
			case ex:NumberFormatException=>return false
		}
		return true
	}
}

object Dir
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

	def saveTxt(name: String,content: String)
	{
		val writer=new PrintWriter(new File(name))
		writer.write(content)
		writer.close()
	}

	def readTxtLinesVerbose(path:String):Array[String]=
	{
		val timer=new Timer
		println("reading lines of %s , %s".format(path,HeapSize.heapsize))
		val lines=Source.fromFile(path).getLines().toArray
		println("number of lines %d , elapsed %f , %s".format(lines.length,timer.elapsed,HeapSize.heapsize))
		lines
	}

	def readTxtLines(path:String):Array[String]=
	{
		Source.fromFile(path).getLines().toArray
	}

	def htmlify(path: String)
	{
		val hpath=path.replaceAll("\\.txt$",".html")
		
		val lines=readTxtLines(path)
		
		var html="<table border=1>"
		
		html=html+(for(line<-lines) yield "<tr><td>"+line.split("\t").mkString("</td><td>")+"</td></tr>\n").mkString
		
		html=html+"</table>"
		
		saveTxt(hpath,html)
	}

	def parseTxtSmart(path: String):Array[Map[String,String]]=
	{
		val lines=Source.fromFile(path).getLines().toArray
		
		val headers=Parse.strip(lines.head).split("\t");
		
		for(line<-lines.tail) yield
			(headers zip Parse.strip(line).split("\t")).toMap
	}

}