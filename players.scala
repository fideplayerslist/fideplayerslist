import javafx.application._
import javafx.scene.Scene
import javafx.scene.layout._
import javafx.stage._
import javafx.scene.control._
import javafx.scene.input._
import javafx.event._
import javafx.geometry._

import java.io._
import java.lang.System

import scala.io.Source
import scala.xml.pull._

import scala.collection.immutable.ListMap

class PlayersClass extends Application {

	var infoLabel=new Label("")
	var infoTextArea=new TextArea()
	var root=new VBox()
	var modal_root=new VBox()
	var modal_stage=new Stage()

	val 
	MAXCNT=1000000
	
	var cnt=0

	var phase=1
	
	var abs_t0=0.0
	
	val collected_keys=Array("birthday","country","flag","sex","title")
	
	def PERCENT(counter: Int, denominator: Int): String=
	{
		if(denominator==0) return "NA"
		"%.2f".format(counter/denominator.toFloat*100f)
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
	
	def process()
	{
	
		val xml = new XMLEventReader(Source.fromFile("players_list_xml.xml"))
		
		var t0=System.nanoTime()
		abs_t0=System.nanoTime()
		
		cnt=0
		
		content_length=0
		
		update_textarea("")
		updateRaw("")
		
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
				
				if(interrupted)
				{
					infoBox("Warning","Processing interrupted.")
				}
				
			}
	
			loop()
			
			if(writer!=null)
			{
				writer.close()
			}
			
		}

		parse(xml)

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
		
	}
	
	var interrupted=false
	
	def do_thread(callback: () => Unit)
	{
		interrupted=false
		val t=new Thread(new Runnable{def run{
			callback()
		}})
		t.start
		create_modal()
		interrupted=true
	}
	
	def create_stats()
	{
		for(key<-collected_keys)
		{
			updateRaw("Creating stats for "+key+".")
		
			val statsdir=key+"stats"
			mkdir(statsdir)
			deleteFilesInDir(statsdir)
			
			for(subkey_filename<-getListOfFileNames(key))
			{
				val subkey=subkey_filename.split("\\.")(0)
				
				//if(subkey!="")
				{
			
					update_textarea("Creating stats: "+subkey)
					
					var ALL=0
					var M=0
					var F=0
					var MF=0
					
					val fpath=key+"/"+subkey+".txt"
					val lines=Source.fromFile(fpath).getLines().toArray
			
					val headers=strip(lines.head).split("\t");
					
					for(line<-lines.tail)
					{
						ALL=ALL+1
					
						val record=(headers zip strip(line).split("\t")).toMap
				
						if(record.contains("sex"))
						{
							if(record("sex")=="M")
							{
								M=M+1
								MF=MF+1
							}
							if(record("sex")=="F")
							{
								F=F+1
								MF=MF+1
							}
						}
					}
					
					val PARF=PERCENT(F,MF)
					
					val content="key\tvalue\n"+"M\t"+M+"\n"+"F\t"+F+"\n"+"MF\t"+MF+"\n"+"PARF\t"+PARF+"\n"
					
					save_txt(statsdir+"/"+subkey+".txt",content)
				
				}
			}
			
		}
		
		updateRaw("Create stats done.")
		
	}
	
	def collect_keys()
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
		
		update_textarea("")
		updateRaw("")
		
		for(line <- Source.fromFile("players.txt").getLines())
		{
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
	}
	
	def startup_thread_func()
	{
		phase=1
		process()
		if(interrupted) return
		phase=2
		process()
		if(interrupted) return
		collect_keys()
		if(interrupted) return
	}

	override def start(primaryStage: Stage)
	{
		primaryStage.setTitle("FIDE Players")
		
		val startButton0=new Button("STARTUP")
		val startButton1=new Button("Process XML - Phase 1 - count keys")
		val startButton2=new Button("Process XML - Phase 2 - create players.txt")
		val startButton3=new Button("Collect keys from players.txt")
		val startButton4=new Button("Create stats")
		val testButton=new Button("Test")
		
		startButton0.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				do_thread(startup_thread_func)
			}
		});
		
		startButton1.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				phase=1
				do_thread(process)
			}
		});
		
		startButton2.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				phase=2
				do_thread(process)
			}
		});
		
		startButton3.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				do_thread(collect_keys)
			}
		});
		
		startButton4.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				do_thread(create_stats)
			}
		});
		
		testButton.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				//runtest
			}
		});
		
		root.getChildren.add(startButton0)
		root.getChildren.add(startButton1)
		root.getChildren.add(startButton2)
		root.getChildren.add(startButton3)
		root.getChildren.add(startButton4)
		root.getChildren.add(testButton)

		primaryStage.setScene(new Scene(root, 300, 300))
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