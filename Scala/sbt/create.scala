object Create extends App
{
	import scala.io.StdIn.readLine

	import java.io._

	def mkdirs(path: List[String]) = // return true if path was created
		path.tail.foldLeft(new File(path.head)){(a,b) => a.mkdir; new File(a,b)}.mkdir
		
	def mkdir(path: String) = // create single dir
		mkdirs(List(path))

	def saveTxt(name: String,content: String)
	{
		val writer=new PrintWriter(new File(name))
		writer.write(content)
		writer.close()
	}

	val project_name=readLine("project name : ")

	for(d<-List("project","src")) mkdirs(List(project_name,d))

	for(d1<-List("main","test")) for(d2<-List("java","resources","scala")) mkdirs(List(project_name,"src",d1,d2))

	val build_sbt=s"""import com.github.retronym.SbtOneJar._

oneJarSettings

libraryDependencies += "commons-lang" % "commons-lang" % "2.6"

name := "$project_name"

version := "1.0"

scalaVersion := "2.11.7"
"""

	val build_properties=s"""sbt.version=0.13.8

"""

	val plugins_sbt=s"""addSbtPlugin("org.scala-sbt.plugins" % "sbt-onejar" % "0.8")

"""

	val main_scala=s"""object $project_name extends App
{
	println("Main $project_name.")
}
"""

	saveTxt(s"$project_name/build.sbt",build_sbt)
	saveTxt(s"$project_name/project/build.properties",build_properties)
	saveTxt(s"$project_name/project/plugins.sbt",plugins_sbt)
	saveTxt(s"$project_name/src/main/scala/$project_name.scala",main_scala)
}