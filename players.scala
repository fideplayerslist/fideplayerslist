import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout._
import javafx.stage.Stage
import javafx.scene.control._
import javafx.scene.input._
import javafx.event._

import java.io._

import scala.io.Source
import scala.xml.pull._

class PlayersClass extends Application {

	var phase=1

	def process()
	{
	
		val xml = new XMLEventReader(Source.fromFile("players_list_xml.xml"))
		
		var cnt=0
		
		def parse(xml: XMLEventReader)
		{
		
			val MAXCNT=1000000
			
			var writer:PrintWriter=null
			
			var current_tag=""
			var current_value=""
			var current_line:Array[String]=Array[String]()
			
			if(phase==1)
			{
				writer=new PrintWriter(new File("players.txt"))
			}
		
			def loop(currNode: List[String])
			{
			
				if((xml.hasNext) && (cnt<MAXCNT))
				{
					xml.next match
					{
						case EvElemStart(_, label, _, _) =>
						
							current_tag=label
						
							loop(label :: currNode)
						
						case EvElemEnd(_, label) =>
						
							if(label=="player")
							{
								writer.write(current_line.mkString("\t")+"\n")
								current_tag=""
								current_line=Array[String]()
								current_value=""
								if((cnt%10000)==0)
								{
									println("Element index: " + cnt)
								}
								cnt=cnt+1
							}
							else
							{
								current_line=current_line:+(current_value)
								current_tag=""
								current_value=""
							}
						
							loop(currNode.tail)
						
						case EvText(text) =>
						
							if((text!="")&&(current_tag!=""))
							{
								current_value=text
							}
						
							loop(currNode)
						
						case _ => loop(currNode)
					}
				}
				
			}
	
			loop(List.empty)
			
			if(writer!=null)
			{
				writer.close()
			}
			
		}

		parse(xml)
		
		println("Done. A total of "+cnt+" records processed.")
		
	}

	override def start(primaryStage: Stage)
	{
		primaryStage.setTitle("FIDE Players")

		val root = new FlowPane
		
		val startButton=new Button("Process XML")
		
		startButton.setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				process()
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