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

class PlayersClass extends Application {

	var infoLabel=new Label("")
	var root=new FlowPane()
	var modal_root=new FlowPane()
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
	
	def create_modal()
	{
		infoLabel=new Label("")
		modal_root=new FlowPane()
		modal_root.setPadding(new Insets(10, 10, 10, 10))
		modal_root.setStyle("-fx-font-size:18px;")
		modal_stage=new Stage()
		modal_stage.initModality(Modality.APPLICATION_MODAL)
		modal_root.getChildren().add(infoLabel)
		val modal_scene=new Scene(modal_root,800,50)
		modal_stage.setScene(modal_scene)
		modal_stage.showAndWait()
	}

	def process()
	{
	
		val xml = new XMLEventReader(Source.fromFile("players_list_xml.xml"))
		
		var t0=System.nanoTime()
		abs_t0=System.nanoTime()
		
		cnt=0
		
		def parse(xml: XMLEventReader)
		{
			
			var writer:PrintWriter=null
			
			var current_tag=""
			var current_value=""
			var current_line:Array[String]=Array[String]()
			
			if(phase==1)
			{
				writer=new PrintWriter(new File("players.txt"))
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
								writer.write(current_line.mkString("\t")+"\n")
								current_tag=""
								current_line=Array[String]()
								current_value=""
								val t=System.nanoTime()
								if(t-t0>5e8)
								{
									t0=t
									val info="Processed: " + cnt + "."
									update(info)
								}
								cnt=cnt+1
							}
							else
							{
								current_line=current_line:+(current_value)
								current_tag=""
								current_value=""
							}
						
						case EvText(text) =>
						
							if((text!="")&&(current_tag!=""))
							{
								current_value=text
							}
						
						case _ => //loop(currNode)
					}
				}
				
				if(interrupted)
				{
					println("Warning: Processing interrupted.")
				}
				
			}
	
			loop()
			
			if(writer!=null)
			{
				writer.close()
			}
			
		}

		parse(xml)
		
		update("Done. A total of "+cnt+" records processed.")
		
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

	override def start(primaryStage: Stage)
	{
		primaryStage.setTitle("FIDE Players")
		
		val startButton=new Button("Process XML")
		
		startButton.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				process_thread()
			}
		});
		
		root.getChildren.add(startButton)

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