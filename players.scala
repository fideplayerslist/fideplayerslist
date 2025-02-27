import javafx.application._
import javafx.application._
import javafx.stage._
import javafx.scene._
import javafx.scene.layout._
import javafx.scene.control._
import javafx.scene.canvas._
import javafx.scene.input._
import javafx.scene.paint._
import javafx.scene.text._
import javafx.event._
import javafx.geometry._

import java.io._
import java.lang.System

import scala.io.Source
import scala.xml.pull._

import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer

class PlayersClass extends Application {

	val REFERENCE_YEAR=2015
	
	val FILTERS=List("x","m","a","ma")
	val MINNUMPS=List("0","50","100","200","300","400","500","750","1000")
	val TRANSLATE_FILTERS=Map("x"->"none","m"->"middle age","a"->"active","ma"->"middle age, active")
	val TRANSLATE_FILTERS_TITLE=Map("x"->"among all players","m"->"among middle age players","a"->"among active players","ma"->"among middle age, active players")
	
	class Timer
	{
		var t0=System.nanoTime()
		def elapsed:Double = (System.nanoTime() - t0)/1.0e9
	}
	
	def stripfloat(what: String):String =
	{
		what.replaceAll("\\.0$","")
	}

	def isValidFloat(what: String):Boolean =
	{
		if((what=="NA")||(what=="")) return false
		return true
	}
	
	def getIth(line: String, index: Int):String =
	{
		val fields=line.split("\t")
		if(index>(fields.length-1)) return ""
		fields(index)
	}
	
	def calc_linear(XYS: Map[Float,Float]): List[Float] =
	{
		var Sx:Float=0.0.toFloat
		var Sy:Float=0.0.toFloat
		var Sxx:Float=0.0.toFloat
		var Sxy:Float=0.0.toFloat
		var Syy:Float=0.0.toFloat
		var n=0
		for((k,v)<-XYS)
		{
			val x=k
			val y=v
			Sx=Sx+x
			Sy=Sy+y
			Sxx=Sxx+x*x
			Sxy=Sxy+x*y
			Syy=Syy+y*y
			n=n+1
		}
		
		val Beta=(n*Sxy-Sx*Sy)/((n*Sxx)-(Sx*Sx))
		val Alpha=(Sy/n)-(Beta*Sx/n)
	
		List(Alpha,Beta)
	}
	
	val KEY_TRANSLATIONS=Map(
		"PARF"->"Female participation %",
		"PARFR"->"Female participation % rated",
		"AVGRM"->"Average male rating",
		"AVGRF"->"Average female rating"
		)

	class Chart(set_canvas_width: Float, set_canvas_height: Float)
	{
	
		val MAXSERIES=3
	
		val CANVAS_WIDTH:Float=set_canvas_width
		val CANVAS_HEIGHT:Float=set_canvas_height
		val canvas=new Canvas(CANVAS_WIDTH,CANVAS_HEIGHT)
		val gc=canvas.getGraphicsContext2D()
		
		val COLORS=List(Color.rgb(255,0,0),Color.rgb(0,0,255),Color.rgb(0,255,0))
		val GRID_COLOR=Color.rgb(192,192,192)
		
		val INFINITE:Float=1E20.toFloat
		
		val PADDING:Float=20.0.toFloat
		val TOP_MARGIN:Float=60.0.toFloat
		val BOTTOM_MARGIN:Float=120.0.toFloat
		val LEFT_MARGIN:Float=100.0.toFloat
		val RIGHT_MARGIN:Float=400.0.toFloat
		val CHART_X0:Float=LEFT_MARGIN
		val CHART_Y0:Float=TOP_MARGIN
		val CHART_WIDTH:Float=CANVAS_WIDTH-(LEFT_MARGIN+RIGHT_MARGIN)
		val CHART_HEIGHT:Float=CANVAS_HEIGHT-(TOP_MARGIN+BOTTOM_MARGIN)
		val BOX_WIDTH:Float=CANVAS_WIDTH/100.0.toFloat
		val CHART_Y1:Float=TOP_MARGIN+CHART_HEIGHT
		val CHART_X1:Float=LEFT_MARGIN+CHART_WIDTH
		val LEGEND_X0:Float=CANVAS_WIDTH-RIGHT_MARGIN+3*PADDING
		
		val LEGEND_Y0:Float=CHART_Y0+PADDING
		val LEGEND_STEP:Float=CANVAS_HEIGHT/10.0.toFloat
		
		val TITLE_Y0:Float=PADDING
		val TITLE_X0:Float=LEFT_MARGIN+PADDING
		
		val LEGEND_FONT_SIZE:Float=16.0.toFloat
		val TITLE_FONT_SIZE:Float=22.0.toFloat
		
		val GRID_STEPS=List(5,10,20,25,75)
		
		val nonearray:Array[(Float) => Float]=Array(none,none,none)
		val okarray:Array[(Float) => Boolean]=Array(ok,ok,ok)
		
		def topath(pname: String):String =
		{
			"keystats/"+pname+".byall."+currentfilter+".txt"
		}
		
		def makeSimple( title: String, pname: String, xkey: String, ykeys: Array[String])
		{
			make(title,topath(pname),xkey,ykeys,none,nonearray,ok,okarray)
		}
		
		def makeSimpleAge( title: String, ykeys: Array[String])
		{
			make(title,topath("birthday"),"birthday",ykeys,birthday_to_age,nonearray,birthday_ok,okarray)
		}
		
		var c_title=""
		var c_path=""
		var c_xkey=""
		var c_ykeys=Array[String]()
		var c_xfunc:(Float)=>Float=none
		var c_yfuncs:Array[(Float)=>Float]=nonearray
		var c_okxfunc:(Float)=>Boolean=ok
		var c_okyfuncs:Array[(Float)=>Boolean]=okarray
		var c_valid=false
		
		def remake()
		{
			if(!c_valid) return
			c_path=c_path.replaceAll("byall\\.[^\\.]*","byall."+currentfilter)
			make(c_title,c_path,c_xkey,c_ykeys,c_xfunc,c_yfuncs,c_okxfunc,c_okyfuncs)
		}
		
		def make( title: String, path: String , xkey: String , ykeys: Array[String] , xfunc: (Float) => Float , yfuncs: Array[(Float) => Float], okxfunc: (Float) => Boolean , okyfuncs: Array[(Float) => Boolean] )
		{
		
			c_title=title
			c_path=path
			c_xkey=xkey
			c_ykeys=ykeys
			c_xfunc=xfunc
			c_yfuncs=yfuncs
			c_okxfunc=okxfunc
			c_okyfuncs=okyfuncs
			
			c_valid=true
		
			gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT)
		
			var MAXX:Float=(-INFINITE)
			var MINX:Float=INFINITE
			var MAXY:Float=(-INFINITE)
			var MINY:Float=INFINITE
		
			var XYSS=Array[Map[Float,Float]]()
			
			var i=0
			for(ykey<-ykeys)
			{
				var XYS=Map[Float,Float]()
				for(record<-parseTxtSmart(path))
				{
					if(record.contains(xkey)&&record.contains(ykey)&&record.contains("RMF"))
					{
						if(record("RMF").toInt>currentminnump.toInt)
						{
							val X=record(xkey)
							val Y=record(ykey)
							if(isValidFloat(X)&&isValidFloat(Y))
							{
								val XVAL:Float=xfunc(myToFloat(X))
								val YVAL:Float=yfuncs(i)(myToFloat(Y))
								if(okxfunc(XVAL)&&okyfuncs(i)(YVAL))
								{
									XYS+=(XVAL->YVAL)
									if(XVAL>MAXX) MAXX=XVAL
									if(XVAL<MINX) MINX=XVAL
									if(YVAL>MAXY) MAXY=YVAL
									if(YVAL<MINY) MINY=YVAL
								}
							}
						}
					}
				}
				XYSS=XYSS:+XYS
				i=i+1
			}
			
			var RANGEX:Float=MAXX-MINX
			var RANGEY:Float=MAXY-MINY
			
			if(RANGEX<=0)
			{
				println("X range too small.")
				return
			}
			
			var FACTORX:Float=CHART_WIDTH/RANGEX
			
			if(RANGEY<=0)
			{
				println("Y range too small.")
				return
			}
			
			var FACTORY:Float=CHART_HEIGHT/RANGEY
			
			//println("MINX "+MINX+" MAXX "+MAXX+" MINY "+MINY+" MAXY "+MAXY+" RANGEX "+RANGEX+" RANGEY "+RANGEY+" FACTORX "+FACTORX+" FACTORY "+FACTORY+" CHART_X0 "+CHART_X0+" CHART_Y0 "+CHART_Y0+" CHART_WIDTH "+CHART_WIDTH+" CHART_HEIGHT "+CHART_HEIGHT)
			
			//draw grid
			def calc_step(range: Float):Float =
			{
				var grid_step_i=0
				var mult:Float=1.0.toFloat
				var done=false
				var current_step:Float=1.0.toFloat
				do
				{
					done=true
					current_step=GRID_STEPS(grid_step_i)*mult
					val current_no=range/current_step
					if(current_no>15)
					{
						if(grid_step_i<(GRID_STEPS.length-1))
						{
							grid_step_i=grid_step_i+1
						}
						else
						{
							grid_step_i=0
							mult=mult*10
						}
						done=false
					}
					if(current_no<5)
					{
						if(grid_step_i>0)
						{
							grid_step_i=grid_step_i-1
						}
						else
						{
							grid_step_i=GRID_STEPS.length-1
							mult=mult/10
						}
						done=false
					}
				}while(!done)
				
				current_step
			}
			
			val stepx:Float=calc_step(RANGEX)
			val ix0:Float=(MINX/stepx).floor+1
			val stepy:Float=calc_step(RANGEY)
			val iy0:Float=(MINY/stepy).floor+1
			
			//println("ix "+ix+" stepx "+stepx+" iy "+iy+" stepy "+stepy)
			
			//draw scale grid
			var alt:Float=0.0.toFloat
			var ix:Float=ix0
			var iy:Float=iy0
			while(((ix)*stepx)<MAXX)
			{
				val x=ix*stepx
				val cx=calcx(x)
				drawline(cx,CHART_Y0-BOX_WIDTH,cx,CHART_Y1+BOX_WIDTH,GRID_COLOR)
				alt=1-alt
				ix=ix+1
			}
			
			while(((iy)*stepy)<MAXY)
			{
				val y=iy*stepy
				val cy=calcy(y)
				drawline(CHART_X0-BOX_WIDTH,cy,CHART_X1-BOX_WIDTH,cy,GRID_COLOR)
				iy=iy+1
			}
			
			//draw chart
			
			def calcx(x: Float):Float =
			{
				CHART_X0+(x-MINX)*FACTORX
			}
			
			def calcy(y: Float):Float =
			{
				CHART_Y0+CHART_HEIGHT-((y-MINY)*FACTORY)
			}
			
			def drawbox(x: Float, y: Float, c: Color)
			{
				gc.setFill(c)
				gc.fillRect(x-BOX_WIDTH/2,y-BOX_WIDTH/2,BOX_WIDTH,BOX_WIDTH)
			}
			
			def drawline(x0: Float, y0: Float, x1: Float, y1: Float, c: Color)
			{
				gc.setLineWidth(3)
				gc.setStroke(c)
				gc.strokeLine(x0,y0,x1,y1)
			}
			
			def drawtext(x: Float, y:Float, what: String, size: Float)
			{
				gc.setFont(new Font(size))
				gc.setFill(Color.rgb(0,0,0))
				gc.fillText(what,x,y+size/2)
			}
			
			def clearmargins()
			{
				gc.clearRect(0,0,CANVAS_WIDTH,CHART_Y0-BOX_WIDTH)
				gc.clearRect(0,CHART_Y1+PADDING,CANVAS_WIDTH,BOTTOM_MARGIN)
			}
			
			var Betas=Array[Float]()
			i=0
			for(XYS<-XYSS)
			{
				//println("drawint series "+i)
				for((k,v)<-XYS)
				{
					val cx=calcx(k)
					val cy=calcy(v)
					
					//println("x "+k+" y "+v+" cx "+cx+" cy "+cy)
					
					drawbox(cx,cy,COLORS(i))
				}
				
				//draw linear
				
				val linear=calc_linear(XYS)
				
				val Alpha=linear(0)
				val Beta=linear(1)
				
				Betas=Betas:+Beta
				
				val x0=MINX
				val cx0=calcx(x0)
				val y0=Alpha+x0*Beta
				val cy0=calcy(y0)
				val x1=MAXX
				val cx1=calcx(x1)
				val y1=Alpha+x1*Beta
				val cy1=calcy(y1)
				
				drawline(cx0,cy0,cx1,cy1,COLORS(i))
				
				i=i+1
			}
			
			//trendline cleanup
			clearmargins()
			
			//draw scale text
			alt=0.0.toFloat
			ix=ix0
			iy=iy0
			while(((ix)*stepx)<MAXX)
			{
				val x=ix*stepx
				val cx=calcx(x)
				drawtext(cx-PADDING/2,CHART_Y1+2.5.toFloat*PADDING+alt*PADDING,stripfloat(""+x),14)
				alt=1-alt
				ix=ix+1
			}
			
			while(((iy)*stepy)<MAXY)
			{
				val y=iy*stepy
				val cy=calcy(y)
				drawtext(CHART_X0-3.5.toFloat*PADDING,cy-BOX_WIDTH/4.2.toFloat,stripfloat(""+y),14)
				iy=iy+1
			}
			
			//draw legend
			for(i <- 0 to XYSS.length-1)
			{
				var cy=LEGEND_Y0+2*LEGEND_STEP*i
				drawbox(LEGEND_X0,cy,COLORS(i))
				drawtext(LEGEND_X0+2*BOX_WIDTH,cy-BOX_WIDTH/3,KEY_TRANSLATIONS(ykeys(i)),LEGEND_FONT_SIZE)
				cy=LEGEND_Y0+2*LEGEND_STEP*i+LEGEND_STEP
				drawline(LEGEND_X0,cy,LEGEND_X0+PADDING,cy,COLORS(i))
				drawtext(LEGEND_X0+3*BOX_WIDTH,cy-BOX_WIDTH/3,"linear, beta "+Betas(i),LEGEND_FONT_SIZE)
			}
			
			//draw title
			val titlef=title+" , "+TRANSLATE_FILTERS_TITLE(currentfilter)+" , > "+currentminnump+" rated players"
			drawtext(TITLE_X0,TITLE_Y0,titlef,TITLE_FONT_SIZE)
			
		}
		
	}

	var infoLabel=new Label("")
	var infoTextArea=new TextArea()
	var root=new FlowPane()
	var modal_root=new VBox()
	var modal_stage=new Stage()

	val 
	MAXCNT=1000000
	
	var cnt=0

	var phase=1
	
	var abs_t0=0.0
	
	val collected_keys=Array("birthday","country","flag","sex","title")
	val sensitive_keys=Array("flag","sex")
	
	val keystat_fields=Array("ALL","MF","M","F","PARF","RM","RF","RMF","R","PARFR","AVGRM","AVGRF","AVGRMF","AVGR")
	val keystat_indices=(keystat_fields zip (1 to keystat_fields.length)).toMap
	
	val chart=new Chart(1200,550)
	
	def htmlify(path: String)
	{
		val hpath=path.replaceAll("\\.txt$",".html")
		
		//println("htmlify "+path+" to "+hpath)
		
		val lines=parseTxtSmartA(path)
		
		var html="<table border=1>"
		
		html=html+(for(line<-lines) yield "<tr><td>"+line.mkString("</td><td>")+"</td></tr>\n").mkString
		
		html=html+"</table>"
		
		save_txt(hpath,html)
	}
	
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
	
	def PERCENT(counter: Float, denominator: Float): String =
	{
		if(denominator==0) return "NA"
		"%.2f".format(counter/denominator.toFloat*100f)
	}
	
	def AVERAGE(counter: Float, denominator: Float): String =
	{
		if(denominator==0) return "NA"
		"%.2f".format(counter/denominator.toFloat)
	}
	
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
	
	def mkdirs(path: List[String]) = // return true if path was created
		path.tail.foldLeft(new File(path.head)){(a,b) => a.mkdir; new File(a,b)}.mkdir
		
	def mkdir(path: String) = // create single dir
		mkdirs(List(path))
		
	def updateRaw(info: String)
	{
		println(info)
		Platform.runLater(new Runnable{def run{
			infoLabel.setText(info)
		}})
	}
	
	def update(info: String)
	{
		val elapsed=(System.nanoTime()-abs_t0)/1e9
		val info_updated=info+" Elapsed %.0f sec , speed %.2f records / sec.".format(elapsed,cnt/elapsed)
		updateRaw(info_updated)
	}
	
	def update_textarea(info: String)
	{
		Platform.runLater(new Runnable{def run{
			println(info)
			infoTextArea.setText(info)
		}})
	}
	
	def infoBox(titleBar:  String,infoMessage: String)
	{
		javax.swing.JOptionPane.showMessageDialog(null, infoMessage, titleBar, javax.swing.JOptionPane.INFORMATION_MESSAGE);
		println(titleBar+". "+infoMessage)
	}
	
	def create_modal()
	{
		infoLabel=new Label("")
		infoTextArea=new TextArea()
		infoTextArea.setMinWidth(300);
		infoTextArea.setMinHeight(500);
		infoTextArea.setStyle("-fx-display-caret:false;-fx-font-size:16px")
		modal_root=new VBox()
		modal_root.setPadding(new Insets(10, 10, 10, 10))
		modal_root.setStyle("-fx-font-size:18px;")
		modal_stage=new Stage()
		modal_stage.initModality(Modality.APPLICATION_MODAL)
		modal_root.getChildren().add(infoLabel)
		modal_root.getChildren().add(infoTextArea)
		val modal_scene=new Scene(modal_root,800,550)
		modal_stage.setScene(modal_scene)
		modal_stage.showAndWait()
	}
	
	type IntHash=Map[String,Int]
	def IntHash()=Map[String,Int]()
	
	var keycounts:IntHash=IntHash()
	
	type IntHash2=Map[String,IntHash]
	def IntHash2()=Map[String,IntHash]()
	
	var keyfreqs:IntHash2=IntHash2()
	
	var content_length=0
	
	def keycounts_info(sep: String):String=
	{
		var keycounts_info="index"+sep+"key"+sep+"count"+sep+"status"+sep+"number of missing"+"\n"
		var i=0
		
		keycounts=ListMap(keycounts.toSeq.sortWith(_._1 < _._1):_*)
		
		for ((k,v) <- keycounts)
		{
			val missing=cnt-v
			keycounts_info+=i+sep+k+sep+v+sep+(if(missing>0) "missing"+sep+missing else "complete"+sep)+"\n"
			i=i+1
		}
		
		return keycounts_info
	}
	
	def strip(content: String):String=
		content.replaceAll("[\r\n]","")
		
	type Record=Map[String,String]
	def Record()=Map[String,String]()
	
	type RecordList=Array[Record]
	def RecordList()=Array[Record]()
	
	type ArrayList=Array[Array[String]]
	def ArrayList()=Array[Array[String]]()
	
	def parseTxtRating(path: String):Array[String] =
	{
		val lines=Source.fromFile(path).getLines().toArray
		val header=lines.head.split("\t")
		val ratingColumn=header.indexOf("rating")
		def getrating(line: String):Float =
		{
			val values=line.split("\t")
			if((values.length-1)<ratingColumn) return 0.0.toFloat
			myToFloat(values(ratingColumn))
		}
		( (header.mkString("\t")+"\n") + (((for(line<-lines.tail) yield ( if(getrating(line)>2000) line+"\n" else "" )) ).mkString) ).split("\n")
	}
	
	def parseTxtSimple(path: String):Array[String]=
	{
		Source.fromFile(path).getLines().toArray
	}
	
	def parseTxtSmartA(path: String):ArrayList=
	{
		val lines=Source.fromFile(path).getLines().toArray
		
		for(line<-lines) yield
			strip(line).split("\t")
	}
	
	def parseTxtSmart(path: String):RecordList=
	{
		val lines=Source.fromFile(path).getLines().toArray
		
		val headers=strip(lines.head).split("\t");
		
		for(line<-lines.tail) yield
			(headers zip strip(line).split("\t")).toMap
	}
	
	def parseTxt(path: String):RecordList=
	{
		var first=true
		var headers=Array[String]()
		
		var recordlist=RecordList()
		
		for(line <- Source.fromFile(path).getLines())
		{
			val fields=strip(line).split("\t")
			if(first)
			{
				headers=fields
				first=false
			}
			else
			{
				val record:Record=(headers zip fields).toMap
				recordlist=recordlist:+record
			}
		}
		recordlist
	}
	
	def serializeIntHash(ih: IntHash):String=
	{
		var ser="key\tcount\n"
		for((k,v)<-ih)
		{
			ser=ser+k+"\t"+v+"\n"
		}
		return ser
	}
	
	def sortedSerializeIntHash(ih: IntHash):String=
	{
		val ih_sorted=ListMap(ih.toSeq.sortWith(_._1 < _._1):_*)
		
		return serializeIntHash(ih_sorted)
	}
	
	def save_txt(name: String,content: String)
	{
		val writer=new PrintWriter(new File(name))
		writer.write(content)
		writer.close()
	}
	
	def record_to_ordered_array(record: Map[String,String],ordered_keys: Array[String]):Array[String]=
	{
		var line:Array[String]=Array[String]()
		
		for(key<-ordered_keys)
		{
			var field=""
			if(record.contains(key)) field=record(key)
			line=line:+(field)
		}
		
		return line
	}
	
	def process():Boolean =
	{
	
		val xml = new XMLEventReader(Source.fromFile("players_list_xml.xml"))
		
		var t0=System.nanoTime()
		abs_t0=System.nanoTime()
		
		cnt=0
		
		content_length=0
		
		updateRaw("Process XML - Phase %d".format(phase))
		update_textarea("")
		
		def parse(xml: XMLEventReader)
		{
			
			var writer:PrintWriter=null
			
			var current_tag=""
			var current_value=""
			var current_line:Map[String,String]=Map[String,String]()
			var ordered_keys:Array[String]=Array[String]()
			
			if(phase==1)
			{
				mkdirs(List("keyfreqs"))
				deleteFilesInDir("keyfreqs")
			}
			
			if(phase==2)
			{
				writer=new PrintWriter(new File("players.txt"))
				
				for(record <- parseTxt("keycounts.txt"))
				{
					ordered_keys=ordered_keys:+(record("key"))
				}
				
				writer.write(ordered_keys.mkString("\t")+"\n")
			}
		
			def loop()
			{
			
				while((xml.hasNext) && (cnt<MAXCNT) && (!interrupted))
				{
					xml.next match
					{
					
						case EvElemStart(_, label, _, _) =>
						
							current_tag=label
						
						case EvElemEnd(_, label) =>
						
							if(label=="player")
							{
							
								cnt=cnt+1
							
								if(phase==2)
								{
									val linestr=record_to_ordered_array(current_line,ordered_keys).mkString("\t")+"\n"
									
									content_length=content_length+linestr.length
									
									writer.write(linestr)
									
									current_line=Map[String,String]()
								}
								
								current_tag=""
								
								current_value=""
								
								val t=System.nanoTime()
								
								if(t-t0>5e8)
								{
								
									t0=t
									
									val info="Process XML - Phase %d. Processed records: %d.".format(phase,cnt)
									
									update(info)
									
									if(phase==1)
									{
									
										update_textarea(keycounts_info(" "))
									
									}
									
									if(phase==2)
									{
									
										update_textarea("Written to players.txt %d characters.".format(content_length))
									
									}
									
									
								}
								
							}
							else if(current_tag!="")
							{
							
								if(phase==1)
								{
									if(current_value!="")
									{
										keycounts+=(current_tag->(keycounts.getOrElse(current_tag,0)+1))
									}
										
									if((current_tag!="name")&&(current_tag!="fideid"))
									{
										var keyfreq=keyfreqs.getOrElse(current_tag,IntHash())										
										keyfreq+=(current_value->(keyfreq.getOrElse(current_value,0)+1))
										keyfreqs+=(current_tag->keyfreq)
									}
								}
							
								if(phase==2)
								{
									current_line+=(current_tag->current_value)
								}
								
								current_tag=""
								current_value=""
							}
						
						case EvText(text) =>
						
							if((text!="")&&(current_tag!=""))
							{
								current_value=text
							}
						
						case _ => 
						
						
						
					}
				}
				
			}
	
			loop()
			
			if(writer!=null)
			{
				writer.close()
			}
			
		}

		parse(xml)
		
		if(interrupted) return true

		if(phase==1)
		{
			updateRaw("Phase 1 - saving keyfreqs")
		
			update_textarea(keycounts_info(" "))
		
			save_txt("keycounts.txt",keycounts_info("\t"))
			
			var sinfo=""
			
			for((tag,keyfreq)<-keyfreqs)
			{
				sinfo=sinfo+"Saving "+tag+" ... "
				update_textarea(sinfo)
				save_txt("keyfreqs/"+tag+".txt",sortedSerializeIntHash(keyfreq))
				sinfo=sinfo+"Saving keyfreqs done.\n"
				update_textarea(sinfo)
			}
		}
		
		if(phase==2)
		{
			update_textarea("Written to players.txt %d characters.".format(content_length))
		}
		
		update("Phase %d done. A total of %d records processed.".format(phase,cnt))
		
		return false
		
	}
	
	var interrupted=false
	
	def do_thread(callback: () => Unit)
	{
		interrupted=false
		val t=new Thread(new Runnable{def run{
			callback()
			if(interrupted)
			{
				infoBox("Warning","Processing interrupted.")
			}
		}})
		t.start
		create_modal()
		interrupted=true
	}
	
	def create_rating_lists_for_key_old(key: String):Boolean =
	{
	
		val rdir="ratinglist"+key
		mkdir(rdir)
		deleteFilesInDir(rdir)
		
		val names=getListOfFileNames(key)
		
		updateRaw("Creating rating lists for "+key+".")
		
		for(name<-names)
		{
		
			if(interrupted) return true
			
			//if((name!=".txt")&&(name!="M.txt"))
			{
		
				update_textarea("Creating rating list: "+name)
			
				val lines=parseTxtRating(key+"/"+name)
				
				val header=strip(lines.head).split("\t")
				
				def compfunc(leftE:String,rightE:String):Boolean =
				{
					val leftEr=(header zip strip(leftE).split("\t")).toMap
					val rightEr=(header zip strip(rightE).split("\t")).toMap
					var ratingLeftE:Float=0.0.toFloat
					var ratingRightE:Float=0.0.toFloat
					if(leftEr.contains("rating")) ratingLeftE=myToFloat(leftEr("rating"))
					if(rightEr.contains("rating")) ratingRightE=myToFloat(rightEr("rating"))
					ratingLeftE > ratingRightE
				}
				
				val lines_sorted:Array[String]=lines.tail.sortWith((leftE,rightE) => compfunc(leftE,rightE)).toArray
				
				val lines_as_string:Array[String]=(header.mkString("\t")+"\n")+:(for(line<-lines_sorted) yield strip(line)+"\n")
				
				val tpath=rdir+"/"+name
				save_txt(tpath,lines_as_string.mkString)
				
				htmlify(tpath)
				
			}
			
		}
		
		return false
		
	}
	
	def create_rating_lists_for_key(key: String):Boolean =
	{
	
		val rdir="ratinglist"+key
		mkdir(rdir)
		deleteFilesInDir(rdir)
		
		val names=getListOfFileNames(key)
		
		updateRaw("Creating rating lists for "+key+".")
		
		for(name<-names)
		{
		
			if(interrupted) return true
			
			//if((name!=".txt")&&(name!="M.txt"))
			{
		
				update_textarea("Creating rating list: "+name)
			
				val lines=parseTxtRating(key+"/"+name)
				
				val header=strip(lines.head).split("\t")
				
				val ratingColumn = header.indexOf("rating")
				
				val values = lines.tail.map { x => x.split("\t") }
				
				val lines_sorted = (values.sortBy[Float] { x => ( if((x.length-1)>=ratingColumn) -myToFloat(x(ratingColumn)) else 0.0.toFloat ) }).map(x => x.mkString("\t"))
				
				val lines_as_string=(header.mkString("\t")+"\n")+lines_sorted.mkString("\n")+"\n"
				
				val tpath=rdir+"/"+name
				save_txt(tpath,lines_as_string)
				
				htmlify(tpath)
				
			}
			
		}
		
		return false
		
	}
	
	def create_rating_lists():Boolean =
	{
	
		for(key<-collected_keys)
		{
		
			if(interrupted) return true
			
			/*if(sensitive_keys.contains(key))
			{
				println("using old method")
				create_rating_lists_for_key_old(key)
			}
			else*/
			{
				create_rating_lists_for_key(key)
			}
			
		}
		
		updateRaw("Creating rating lists done.")
		
		return false
		
	}
	
	def key_stats():Boolean =
	{
	
		mkdir("keystats")
		deleteFilesInDir("keystats")
		
		for(filter<-FILTERS)
		{
		
			for(key<-collected_keys)
			{
				updateRaw("Key stats for "+key+". Filter - "+filter)
				
				val statsdir=key+"stats"+filter
				
				var lines=Array[Array[String]]()
				
				for(subkey_filename<-getListOfFileNames(statsdir))
				{
				
					if(interrupted) return true
				
					val subkey=subkey_filename.split("\\.")(0)
					
					update_textarea("Key stats: "+subkey)
					
					var linearray:Array[String]=(for(record<-parseTxtSmart(statsdir+"/"+subkey_filename)) yield record("value")).toArray
					linearray=subkey+:linearray
					lines=lines:+(linearray)
				}
				
				def create_by_sortindex(sortindex: Int, bywhat: String)
				{
					lines=lines.sortWith((leftE,rightE) => myToFloat(leftE(sortindex)) > myToFloat(rightE(sortindex)))
					
					var content=key+"\t"+keystat_fields.mkString("\t")+"\n"
					
					content=content+(for(line<-lines) yield (line.mkString("\t")+"\n")).mkString("")
					
					val tpath="keystats/"+key+"."+bywhat+"."+filter+".txt"
					
					save_txt(tpath,content)
					
					htmlify(tpath)
				}
				
				if(key=="birthday") create_by_sortindex(0,"by"+key)
				create_by_sortindex(keystat_indices("PARF"),"byparf")
				create_by_sortindex(keystat_indices("PARFR"),"byparfr")
				create_by_sortindex(keystat_indices("AVGRF"),"byavgrf")
				create_by_sortindex(keystat_indices("AVGRM"),"byavgrm")
				create_by_sortindex(keystat_indices("AVGR"),"byavgr")
				create_by_sortindex(keystat_indices("ALL"),"byall")
				create_by_sortindex(keystat_indices("R"),"byrated")
				create_by_sortindex(keystat_indices("RM"),"byratedm")
				create_by_sortindex(keystat_indices("RF"),"byratedf")
				
			}
			
		}
		
		updateRaw("Key stats done.")
		
		return false
		
	}
	
	def create_stats():Boolean =
	{
	
		for(filter<-FILTERS)
		{
		
			for(key<-collected_keys)
			{
				updateRaw("Creating stats for "+key+". Filter - "+TRANSLATE_FILTERS(filter)+".")
				
				val statsdir=key+"stats"+filter
				mkdir(statsdir)
				deleteFilesInDir(statsdir)
				
				for(subkey_filename<-getListOfFileNames(key))
				{
					val subkey=subkey_filename.split("\\.")(0)
				
					update_textarea("Creating stats: "+subkey)
					
					var counts=Map[String,String]("ALL"->"0","M"->"0","F"->"0","MF"->"0","RM"->"0","CRM"->"0","RF"->"0","CRF"->"0","R"->"0","CR"->"0","RMF"->"0","CRMF"->"0")
					
					val fpath=key+"/"+subkey+".txt"
					val lines=Source.fromFile(fpath).getLines().toArray
			
					val headers=strip(lines.head).split("\t");
					
					for(line<-lines.tail)
					{
					
						if(interrupted) return true
					
						val record=(headers zip strip(line).split("\t")).toMap
						
						var ok:Boolean=true
						
						if(filter contains "m")
						{
							if(record.contains("birthday"))
							{
								val age=REFERENCE_YEAR-myToFloat(record("birthday"))
								if((age<20)||(age>40))
								{
									ok=false
								}
							}
							else
							{
								ok=false
							}
						}
						
						if(filter contains "a")
						{
							if(record.contains("flag"))
							{
								if(record("flag") contains "i")
								{
									ok=false
								}
							}
						}
						
						if(ok)
						{
						
							counts+=("ALL"->"%d".format((counts("ALL").toInt+1)))
				
							if(record.contains("sex"))
							{
								if(record("sex")=="M")
								{
									counts+=("M"->"%d".format((counts("M").toInt+1)))
									counts+=("MF"->"%d".format((counts("MF").toInt+1)))
								}
								if(record("sex")=="F")
								{
									counts+=("F"->"%d".format((counts("F").toInt+1)))
									counts+=("MF"->"%d".format((counts("MF").toInt+1)))
								}
							}
							
							if(record.contains("rating"))
							{
								val rating=myToFloat(record("rating")).toInt
								
								if(rating>0)
								{
									counts+=("R"->"%d".format((counts("R").toInt+1)))
									counts+=("CR"->"%d".format((counts("CR").toInt+rating)))
									if(record.contains("sex"))
									{
										if(record("sex")=="M")
										{
											counts+=("RM"->"%d".format((counts("RM").toInt+1)))
											counts+=("RMF"->"%d".format((counts("RMF").toInt+1)))
											counts+=("CRM"->"%d".format((counts("CRM").toInt+rating)))
											counts+=("CRMF"->"%d".format((counts("CRMF").toInt+rating)))
										}
										if(record("sex")=="F")
										{
											counts+=("RF"->"%d".format((counts("RF").toInt+1)))
											counts+=("RMF"->"%d".format((counts("RMF").toInt+1)))
											counts+=("CRF"->"%d".format((counts("CRF").toInt+rating)))
											counts+=("CRMF"->"%d".format((counts("CRMF").toInt+rating)))
										}
									}
								}
							}
							
						}
					}
					
					counts+=("PARF"->PERCENT(counts("F").toInt,counts("MF").toInt))
					counts+=("AVGR"->AVERAGE(counts("CR").toInt,counts("R").toInt))
					counts+=("AVGRM"->AVERAGE(counts("CRM").toInt,counts("RM").toInt))
					counts+=("AVGRF"->AVERAGE(counts("CRF").toInt,counts("RF").toInt))
					counts+=("AVGRMF"->AVERAGE(counts("CRMF").toInt,counts("RMF").toInt))
					
					counts+=("PARFR"->PERCENT(counts("RF").toInt,counts("RMF").toInt))
					
					var content="key\tvalue\n"
					for(field<-keystat_fields)
					{
						content=content+field+"\t"+counts(field)+"\n"
					}
					
					save_txt(statsdir+"/"+subkey+".txt",content)
				
				}
				
			}
			
		}
		
		updateRaw("Create stats done.")
		
		return false
		
	}
	
	def collect_keys():Boolean =
	{
		var t0=System.nanoTime()
		abs_t0=System.nanoTime()
	
		for(key<-collected_keys)
		{
			mkdir(key)
			deleteFilesInDir(key)
		}
		
		cnt=0
		
		var first=true
		var headers=Array[String]()
		var hline=""
		
		updateRaw("Collect keys.")
		update_textarea("")
		
		for(line <- Source.fromFile("players.txt").getLines())
		{
		
			if(interrupted) return true
			
			val fields=strip(line).split("\t")
			if(first)
			{
				headers=fields
				hline=headers.mkString("\t")+"\n"
				first=false
			}
			else
			{
			
				val record:Record=(headers zip fields).toMap
				cnt=cnt+1
				
				val t=System.nanoTime()

				if(t-t0>5e8)
				{
					t0=t
					update("Collect keys. Processed records: "+cnt+".")
				}
				
				for(key<-collected_keys)
				{
					if(record.contains(key))
					{
						val path=key+"/"+record(key)+".txt"
						val cline=strip(line)+"\n"
						
						if(new File(path).exists)
						{
							val fw = new FileWriter(path, true)
							try
							{
								fw.write(cline)
							}
							finally fw.close() 
						}
						else
						{
							save_txt(path,hline+cline)
						}
						
					}
				}
			}
		}
		
		updateRaw("Collecting keys done.")
		
		return false
		
	}
	
	def startup_thread_func():Boolean=
	{
	
		delete_all_files()
		if(interrupted) return true
	
		phase=1
		process()
		if(interrupted) return true
		
		phase=2
		process()
		if(interrupted) return true
		
		collect_keys()
		if(interrupted) return true
		
		create_stats()
		if(interrupted) return true
		
		key_stats()
		if(interrupted) return true
		
		create_rating_lists()
		if(interrupted) return true
		
		return false
		
	}
	
	def delete_all_files():Boolean =
	{
	
		val paths=Array("players.txt","keycounts.txt")
		
		var dirs=Array("keyfreqs","keystats")
		
		for(key<-collected_keys)
		{
			dirs=dirs:+key
			dirs=dirs:+(key+"stats")
		}
		
		updateRaw("Deleting files.")
		
		for(path<-paths)
		{
			update_textarea("Deleting "+path)
			val f=new File(path)
			if(f.exists) f.delete
		}
		
		updateRaw("Deleting directories.")
		
		for(path<-dirs)
		{
			update_textarea("Deleting "+path)
			deleteFilesInDir(path)
			val f=new File(path)
			if(f.exists) f.delete
		}
		
		updateRaw("Deleting files done.")
		
		return false
	}
	
	val filterlabel=new Label("  filter: none")
	filterlabel.setMinWidth(150)
	var currentfilter="x"
	
	class MyFilterButton( filter: String ) extends Button( filter )
	{
		val filtertext=TRANSLATE_FILTERS(filter)
		setText("filter: "+filtertext)
		setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				filterlabel.setText("  filter: "+filtertext)
				currentfilter=filter
				chart.remake
			}
		});
	}
	
	var currentminnump="0"
	val minnumplabel=new Label("  minnump: "+currentminnump)
	minnumplabel.setMinWidth(150)
	
	class MyFilterAllButton( minnump: String ) extends Button( minnump )
	{
		setText("minnump: "+minnump)
		setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				minnumplabel.setText("  minnump: "+minnump)
				currentminnump=minnump
				chart.remake
			}
		});
	}
	
	class MyButton( text: String , callback: () => Unit ) extends Button( text )
	{
		setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				do_thread(callback)
			}
		});
	}
	
	class MyButtonSimple( text: String , callback: () => Unit ) extends Button( text )
	{
		setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				callback()
			}
		});
	}
	
	def ok(y: Float):Boolean =
	{
		true
	}
	
	def birthday_ok(y: Float):Boolean =
	{
		(y>=10) && (y<=80)
	}
	
	def none(y: Float):Float =
	{
		y
	}
	
	def birthday_to_age(birthday: Float):Float =
	{
		REFERENCE_YEAR-birthday
	}
	
	def draw_participation_chart()
	{
		chart.makeSimpleAge(
			"Female participation % in the function of age",
			Array("PARF","PARFR")
		)
	}
	
	def draw_rating_chart()
	{
		chart.makeSimpleAge(
			"Rating in the function of age",
			Array("AVGRM","AVGRF")
		)
	}
	
	def draw_country_chart()
	{
		chart.makeSimple(
			"Rating in the function of participation",
			"country",
			"PARFR",
			Array("AVGRM","AVGRF")
		)
	}
	
	def draw_rating_correlation_chart()
	{
		chart.makeSimple(
			"Correlation between male and female ratings",
			"country",
			"AVGRM",
			Array("AVGRF")
		)
	}

	override def start(primaryStage: Stage)
	{
		primaryStage.setTitle("FIDE Players")
		primaryStage.setX(30)
		primaryStage.setY(30)
		
		def phase1()
		{
			phase=1
			process()
		}
		
		def phase2()
		{
			phase=2
			process()
		}
		
		def runtest()
		{
		
			//runtest
			updateRaw("Test.")
			
			val timer=new Timer
			
			def heapsize = "Heap size "+Runtime.getRuntime().totalMemory()/1000000+"."
			
			updateRaw("Reading players txt. "+heapsize)
			
			update_textarea("")
			
			val lines=parseTxtSimple("players.txt")
			
			updateRaw("Reading players done. "+heapsize)
			
			val header=lines.head
			
			for(key<-collected_keys)
			{
			
				updateRaw("Collecting "+key+". "+heapsize)
				
				def collectkey(key: String)
				{
			
					val keyindex=header.split("\t").indexOf(key)
					val ratingindex=header.split("\t").indexOf("rating")
					
					var valuebuffs=Map[String,ArrayBuffer[String]]()
					
					for(line<-lines.tail)
					{
						val value=getIth(line,keyindex)
						val extline="%10s".format(getIth(line,ratingindex))+"\t"+line
						if(valuebuffs.contains(value))
						{
							valuebuffs(value)+=extline
						}
						else
						{
							valuebuffs+=(value->ArrayBuffer[String](extline))
						}
					}
					
					mkdirs(List("collectedkeys",key))
					deleteFilesInDir("collectedkeys/"+key)
					
					for((k,v)<-valuebuffs)
					{
						update_textarea("Writing value "+k)
						val content="sortrating\t"+header+"\n"+v.sorted.reverse.mkString("\n")+"\n"
						save_txt("collectedkeys/"+key+"/"+k+".txt",content)
					}
					
				}
				
				collectkey(key)
				
			}
			
			updateRaw("Collecting keys done. "+heapsize)
			
		}
		
		var ButtonList=List(
			new MyButton("DELETE ALL FILES",delete_all_files),
			new MyButton("STARTUP",startup_thread_func),
			new MyButton("Process XML - Phase 1 - count keys",phase1),
			new MyButton("Process XML - Phase 2 - create players.txt",phase2),
			new MyButton("Collect keys from players.txt",collect_keys),
			new MyButton("Create stats",create_stats),
			new MyButton("Key stats",key_stats),
			new MyButton("Create rating lists",create_rating_lists),
			new MyButtonSimple("Draw participation chart",draw_participation_chart),
			new MyButtonSimple("Draw rating chart",draw_rating_chart),
			new MyButtonSimple("Draw country chart",draw_country_chart),
			new MyButtonSimple("Draw rating correlation chart",draw_rating_correlation_chart)
		)
		
		for(filter<-FILTERS)
		{
			ButtonList=ButtonList:+new MyFilterButton(filter)
		}
		
		for(button<-ButtonList)
		{
			root.getChildren.add(button)
		}
		
		//root.getChildren.add(filterlabel)
		
		for(minnump<-MINNUMPS)
		{
			root.getChildren.add(new MyFilterAllButton(minnump))
		}
		
		//root.getChildren.add(minnumplabel)
		
		root.getChildren.add(new MyButton("Test",runtest))
		
		root.getChildren.add(chart.canvas)

		primaryStage.setScene(new Scene(root))
		primaryStage.show()

	}
	
}

object Players
{
	def main(args: Array[String])
	{
		Application.launch(classOf[PlayersClass], args: _*)
	}
}