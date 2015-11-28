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

import javafx.beans.value._

import mychart._

import globals.Globals._

import utils.Log._

class DrawChartClass extends Application
{

	do_log=false

	var root=new FlowPane()

	val slider=new Slider()

	val FILTERS=List("x","m","a","ma")
	val TRANSLATE_FILTERS=Map("x"->"none","m"->"middle age","a"->"active","ma"->"middle age, active")
	val TRANSLATE_FILTERS_TITLE=Map("x"->"among all players","m"->"among middle age players","a"->"among active players","ma"->"among middle age, active players")

	var current_filter="x"

	var current_limit_value=0.0

	override def start(primaryStage: Stage)
	{

		primaryStage.setTitle("FIDE Players Charts")

		primaryStage.setX(30)
		primaryStage.setY(30)

		def birthday_to_age(b:Double):Double=REFERENCE_YEAR-b

		def age_ok(a:Double):Boolean=((a>=10)&&(a<=80))

		def rating_ok(r:Double):Boolean=((r>=1000)&&(r<=3000))

		def greater_than_zero(x:Double):Boolean=(x>0)

		var chart=new MyChart()
		def getchart
		{
			val size=root.getChildren.size()
			root.getChildren.remove(size-1,size)
			chart=new MyChart(
			DATA_SOURCE_TO_PATH_FUNC=(x => "stats/keystats/"+x+"/"+current_filter+"/byall.txt"),
			KEY_TRANSLATIONS=Map(
					"PARF"->"Female participation %",
					"PARFR"->"Female participation % rated",
					"AVGR"->"Average rating",
					"M"->"Male",
					"F"->"Female",
					"AVGRM"->"Average male rating",
					"AVGRF"->"Average female rating",
					"RM"->"Rated males",
					"RF"->"Rated females"
				)
			)
			root.getChildren.add(chart.canvas_group)
		}

		def subtitle=" , "+TRANSLATE_FILTERS_TITLE(current_filter)+" , > "+current_limit_value.toInt+" players"

		def draw_participation()
		{
			chart.draw(

				set_title="Participation in the function of age"+subtitle,
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
					),

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_rating_distribution()
		{
			chart.draw(

				set_title="Rating distribution"+subtitle,
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

				set_do_trend=false,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_age_distribution_of_rated()
		{
			chart.draw(

				set_title="Age distribution of rated players"+subtitle,
				set_xlegend="Age [ years ]",
				set_ylegend="Frequency",

				set_data_source="birthday",

				set_x_series=Series(
					FIELD="birthday",
					APPLY_FUNC=birthday_to_age,
					OK_FUNC=age_ok
					),

				set_y_series=List(
					Series(
						FIELD="RM",
						OK_FUNC=greater_than_zero
						),
					Series(
						FIELD="RF",
						OK_FUNC=greater_than_zero,
						COLOR=Color.rgb(0,0,255)
						)
					),

				set_do_trend=false,

				set_limit=Limit("R",current_limit_value)

			)
		}

		var current_callback:()=>Unit=draw_participation

		class MyButton( text: String , callback: () => Unit ) extends Button( text )
		{
			setOnAction(new EventHandler[ActionEvent]{
				override def handle(e: ActionEvent)
				{
					callback()
				}
			});
		}

		class MyDrawButton( text: String , callback: () => Unit ) extends Button( text )
		{
			setOnAction(new EventHandler[ActionEvent]{
				override def handle(e: ActionEvent)
				{
					current_callback=callback
					getchart
					current_callback()
				}
			});
		}

		class MyFilterButton( filter: String ) extends Button( "Filter "+TRANSLATE_FILTERS(filter) )
		{
			setOnAction(new EventHandler[ActionEvent]{
				override def handle(e: ActionEvent)
				{
					current_filter=filter
					getchart
					current_callback()
				}
			});
		}

		class SliderChangeListener extends ChangeListener[Number]
		{
			def changed(ov:ObservableValue[_ <: Number],old_val:Number,new_val:Number)
			{
				val x:Double=(""+new_val).toDouble
				current_limit_value=x
				getchart
				current_callback()
			}
		}

		slider.valueProperty().addListener(new SliderChangeListener)

		slider.setMin(0.0)
		slider.setMax(5000.0)
		slider.setMinWidth(1200)
		slider.setShowTickLabels(true)
		slider.setShowTickMarks(true)
		slider.setMajorTickUnit(50.0)

		def clear()
		{
			chart.clear()
		}

		val buttons=scala.collection.mutable.ArrayBuffer[Button](
			new MyDrawButton("Draw participation",draw_participation),
			new MyDrawButton("Draw rating distribution",draw_rating_distribution),
			new MyDrawButton("Draw age distribution of rated",draw_age_distribution_of_rated),
			new MyButton("Clear",clear)
		)

		for(filter<-FILTERS)
		{
			buttons+=new MyFilterButton(filter)
		}

		for(button<-buttons) root.getChildren.add(button)

		root.getChildren.add(slider)

		root.getChildren.add(chart.canvas_group)

		primaryStage.setScene(new Scene(root))

		primaryStage.show()

	}

}