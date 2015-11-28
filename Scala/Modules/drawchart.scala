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

import globals.Globals._

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

		def birthday_to_age(b:Double):Double=REFERENCE_YEAR-b

		def age_ok(a:Double):Boolean=((a>=10)&&(a<=80))

		def rating_ok(r:Double):Boolean=((r>=1000)&&(r<=3000))

		def greater_than_zero(x:Double):Boolean=(x>0)

		var chart=new MyChart(
			DATA_SOURCE_TO_PATH_FUNC=(x => "stats/keystats/"+x+"/x/byall.txt"),
			CHART_WIDTH=750
			)

		def draw_participation()
		{
			chart.draw(

				set_title="Participation in the function of age",
				set_xlegend="Age [ years ]",
				set_ylegend="Participation [ % ]",

				set_data_source="birthday",

				set_x_series=Series(
					FIELD="birthday",
					APPLY_FUNC=birthday_to_age,
					OK_FUNC=age_ok
					),

				set_y_series=List(
					Series(
						FIELD="PARF"
						),
					Series(
						FIELD="PARFR",
						COLOR=Color.rgb(0,0,255)
						)
					)

			)
		}

		def draw_distribution()
		{
			chart.draw(

				set_title="Rating distribution",
				set_xlegend="Rating",
				set_ylegend="Frequency",

				set_data_source="rr",

				set_x_series=Series(
					FIELD="AVGR",
					OK_FUNC=rating_ok
					),

				set_y_series=List(
					Series(
						FIELD="M",
						OK_FUNC=greater_than_zero,
						COLOR=Color.rgb(0,0,255)
						),
					Series(
						FIELD="F",
						OK_FUNC=greater_than_zero
						)
					),

				set_do_trend=false

			)
		}

		def clear()
		{
			chart.clear()
		}

		val buttons:Array[Button]=Array(
			new MyButton("Draw participation",draw_participation),
			new MyButton("Draw distribution",draw_distribution),
			new MyButton("Clear",clear)
		)

		for(button<-buttons) root.getChildren.add(button)

		root.getChildren.add(chart.canvas_group)

		primaryStage.setScene(new Scene(root))

		primaryStage.show()

	}

}