package drawchart

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

import mychart._

class DrawChartClass extends Application
{

	class MyButton( text: String , callback: () => Unit ) extends Button( text )
	{
		setOnAction(new EventHandler[ActionEvent]{
			override def handle(e: ActionEvent)
			{
				callback()
			}
		});
	}

	override def start(primaryStage: Stage)
	{

		primaryStage.setTitle("FIDE Players Charts")

		primaryStage.setX(30)
		primaryStage.setY(30)

		var root=new FlowPane()

		var chart=new MyChart()

		def draw()
		{
			chart.draw(
				title="Chart title",
				xlegend="X axis",
				ylegend="Y axis"
			)
		}

		def clear()
		{
			chart.clear()
		}

		val buttons:Array[Button]=Array(
			new MyButton("Draw",draw),
			new MyButton("Clear",clear)
		)

		for(button<-buttons) root.getChildren.add(button)

		root.getChildren.add(chart.canvas_group)

		primaryStage.setScene(new Scene(root))

		primaryStage.show()

	}

}