import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.scene.control.Label

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
							cnt=cnt+1
							
							println("Start element: " + label)
							loop(label :: currNode)
						
						case EvElemEnd(_, label) =>
						
							println("End element: " + label)
							
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

		val root = new StackPane
		root.getChildren.add(new Label("FIDE players"))

		primaryStage.setScene(new Scene(root, 300, 300))
		primaryStage.show()
		
		process()
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