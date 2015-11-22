package youngtalents

import java.io._

import scala.io.Source

import utils.Dir._
import utils.Parse._
import globals.Globals._

import record._

class YoungTalents
{
	def create():String =
	{

		var exprs=Map[Int,Map[String,String]]()

		println("collecting expected ratings")

		for(age<-(1 to 100))
		{
			val birthday=REFERENCE_YEAR-age

			val bpath="stats/stats/birthday/x/"+birthday+".txt"

			if(new File(bpath).exists)
			{

				var stats=parseRecord(bpath)

				val AVGRM=stats("AVGRM")
				val AVGRF=stats("AVGRF")

				val expg=Map("M"->AVGRM,"F"->AVGRF)

				exprs+=(age->expg)

				println("age %3d avgrm %10s avgrf %10s".format(age,AVGRM,AVGRF))

			}
			else
			{
				println("age %d - NA".format(age))
			}

		}

		var talents=scala.collection.mutable.ArrayBuffer[String]()

		var cnt=0

		println("collecting players")

		for(line<-Source.fromFile("players.txt").getLines())
		{

			val record=new Record("\t"+line)
			if(record.hasAge&&record.hasRating&&record.hasSex&&record.active)
			{
				if((record.age>=1)&&(record.age<=100))
				{

					val expr=myToFloat(exprs(record.age)(record.sex))
					val surplus=(record.rating-expr).toInt

					if(surplus>0)
					{

						val tline="%10d\t%s\t%s\t%s\t%d\t%d".format(surplus,record.name,record.country,record.sex,expr.toInt,record.rating)

						talents+=tline

						cnt=cnt+1

						if((cnt%10000)==0)
						{
							println("player count %d".format(cnt))
						}

					}

				}
			}
		}

		println("total number of players : %d".format(cnt))

		println("sorting talent list")

		val sorted=talents.sorted.reverse

		var i=0

		val numbered=for(line<-sorted) yield { i=i+1; "%d\t%s".format(i,line) }

		val sliced=numbered.slice(0,1000)

		val content="rank\trating surplus\tname\tcountry\tsex\texpected rating\tactual rating\n"+sliced.mkString("\n")+"\n"

		val tpath="stats/youngtalents.txt"

		println("saving talent list")

		saveTxt(tpath,content)
		htmlify(tpath)

		"Done."
	}
}