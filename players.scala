import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout._
import javafx.stage.Stage
import javafx.scene.control._
import javafx.scene.input._
import javafx.event._


import scala.io.Source
import scala.xml.pull._

class PlayersClass extends Application {

	def process()
	{
	
		val xml = new XMLEventReader(Source.fromFile("players_list_xml.xml"))
		
		def parse(xml: XMLEventReader)
		{
		
			val MAXCNT=1000
		
			var cnt=0
		
			def loop(currNode: List[String])
			{
			
				if((xml.hasNext) && (cnt<MAXCNT))
				{
					xml.next match
					{
						case EvElemStart(_, label, _, _) =>
						
							println("Element index: " + cnt)
							
							println("Start element: " + label)
							loop(label :: currNode)
						
						case EvElemEnd(_, label) =>
						
							println("End element: " + label)
							
							cnt=cnt+1
							
							loop(currNode.tail)
						
						case EvText(text) =>
						
							println("Text: " + text)
							loop(currNode)
						
						case _ => loop(currNode)
					}
				}
				
			}
	
			loop(List.empty)
			
		}

		parse(xml)
		
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