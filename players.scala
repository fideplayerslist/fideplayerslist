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

	val MAXCNT=1000000
	
	var cnt=0

	var phase=1
	
	var abs_t0=0.0
	
	def update(info: String)
	{
		val elapsed=(System.nanoTime()-abs_t0)/1e9
		val info_updated=info+" Elapsed %.0f sec , speed %.2f records / sec.".format(elapsed,cnt/elapsed)
		println(info_updated)
		Platform.runLater(new Runnable{def run{
			infoLabel.setText(info_updated)
		}})
	}
	
	def update_textarea(info: String)
	{
		Platform.runLater(new Runnable{def run{
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
	
	var keycounts:Map[String,Int]=Map[String,Int]()
	var content_length=0
	
	def keycounts_info(sep: String):String=
	{
		var keycounts_info=""
		var i=0
		
		keycounts=ListMap(keycounts.toSeq.sortWith(_._1 < _._1):_*)
		
		for ((k,v) <- keycounts)
		{
			val missing=cnt-v
			keycounts_info+=i+sep+k+sep+v+sep+(if(missing>0) "missing"+sep+missing else sep)+"\n"
			i=i+1
		}
		
		return keycounts_info
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
		
		def parse(xml: XMLEventReader)
		{
			
			var writer:PrintWriter=null
			
			var current_tag=""
			var current_value=""
			var current_line:Map[String,String]=Map[String,String]()
			var ordered_keys:Array[String]=Array[String]()
			
			if(phase==2)
			{
				writer=new PrintWriter(new File("players.txt"))
				
				for(line <- Source.fromFile("keycounts.txt").getLines())
				{
					val name=line.split("\t")(1)
					
					ordered_keys=ordered_keys:+(name)
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
									
									val info="Phase %d , processed: %d.".format(phase,cnt)
									
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
							else
							{
							
								if(phase==1)
								{
									if(current_value!="")
									{
										keycounts+=(current_tag->(keycounts.getOrElse(current_tag,0)+1))
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
		
		update("Phase %d done. A total of %d records processed."format(phase,cnt))
		
		if(phase==1)
		{
			update_textarea(keycounts_info(" "))
		
			save_txt("keycounts.txt",keycounts_info("\t"))
		}
		
		if(phase==2)
		{
			update_textarea("Written to players.txt %d characters.".format(content_length))
		}
		
	}
	
	var interrupted=false
	
	def process_thread()
	{
		interrupted=false
		val t=new Thread(new Runnable{def run{
			process()
		}})
		t.start
		create_modal()
		interrupted=true
	}
	
	def startup_thread()
	{
		interrupted=false
		val t=new Thread(new Runnable{def run{
			phase=1
			process()
			if(interrupted) return
			phase=2
			process()
			if(interrupted) return
		}})
		t.start
		create_modal()
		interrupted=true
	}

	override def start(primaryStage: Stage)
	{
		primaryStage.setTitle("FIDE Players")
		
		val startButton0=new Button("STARTUP")
		val startButton1=new Button("Process XML - Phase 1 - count keys")
		val startButton2=new Button("Process XML - Phase 2 - create players.txt")
		
		startButton1.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				phase=1
				process_thread()
			}
		});
		
		startButton2.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				phase=2
				process_thread()
			}
		});
		
		startButton0.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				startup_thread()
			}
		});
		
		root.getChildren.add(startButton0)
		root.getChildren.add(startButton1)
		root.getChildren.add(startButton2)

		primaryStage.setScene(new Scene(root, 300, 300))
		primaryStage.show()

	}
	
}

object Players
{
	def main(args: Array[String])
	{
		Application.launch(classOf[PlayersClass], args: _*)
		/*val p=new PlayersClass()
		p.process()*/
	}
}