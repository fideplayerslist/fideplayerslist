package titled

import globals.Globals._

import utils.Dir._

import scala.io.Source

import record._

class Titled
{
	def percountry():String =
	{

		println("counting titled players per country : %s".format(collected_titles.mkString(" , ")))

		var titles=scala.collection.mutable.ListMap[String,Map[String,Int]]()

		for(title<-collected_titles)
		{
			println("collecting title %s".format(title))

			val path="stats/keycounts/title/"+title+".txt"

			for(line<-Source.fromFile(path).getLines())
			{
				val record=new Record(line)

				if(record.hasCountry)
				{
					var points=title_values(title)
					if(titles.contains(record.country))
					{
						val cmap=titles(record.country)
						var ccount=1
						var allcount=1
						if(cmap.contains(title))
						{
							ccount=cmap(title)+1
						}
						if(cmap.contains("ALL"))
						{
							allcount=cmap("ALL")+1
						}
						if(cmap.contains("POINTS"))
						{
							points=cmap("POINTS")+points
						}
						titles(record.country)+=(title->ccount,"ALL"->allcount,"POINTS"->points)
					}
					else
					{
						titles+=(record.country->Map(title->1,"ALL"->1,"POINTS"->points))
					}
				}
			}
		}

		val sorted_countries=titles.keys.toArray.sortBy[Int]
		{
			x => ( if(titles(x).contains("POINTS")) titles(x)("POINTS") else 0 )
		}

		var rank=0
		var content="rank\tcountry\tpoints\tall titled\t"+collected_titles.mkString("\t")+"\n"
		for(country<-sorted_countries.reverse)
		{
			rank=rank+1
			var line="%d\t%s\t%d\t%d".format(rank,country,titles(country)("POINTS"),titles(country)("ALL"))
			for(title<-collected_titles)
			{
				var count=0
				val cv=titles(country)
				if(cv.contains(title))
				{
					count=cv(title)
				}
				line=line+("\t%s".format(if(count>0) count else ""))
			}
			content=content+line+"\n"
		}

		val path="stats/titledpercountry.txt"
		val hpath="stats/titledpercountry.html"

		val content_expl="title values used to calculate points : "+(for(title<-collected_titles) yield ("%s - %d pts".format(title,title_values(title)))).mkString(" , ")+"\n"

		saveTxt(path,content)
		htmlify(path)

		saveTxt(hpath,content_expl+readTxt(hpath))

		println(content)

		"Done."

	}
}