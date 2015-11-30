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

	trait HasId
	{
		var id:Int=0
	}

	class RadioButton
	{

		var buttons=List[Button with HasId]()

		def select(id: Int)
		{
			for(button<-buttons)
			{
				val style=button.getStyle
				if(id==button.id)
				{
					button.setStyle(style.replaceAll("-fx-border-width: [^;]*","-fx-border-width: 8"))
				}
				else
				{
					button.setStyle(style.replaceAll("-fx-border-width: [^;]*","-fx-border-width: 3"))
				}
			}
		}
	}

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

		primaryStage.setX(5)
		primaryStage.setY(5)

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
					"RM"->"Rated male",
					"RF"->"Rated female",
					"T"->"Titled",
					"GM"->"GMs",
					"TP"->"Titled points"
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
						FIELD="PARFR"
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
						OK_FUNC=greater_than_zero
						),
					Series(
						FIELD="F",
						OK_FUNC=greater_than_zero,
						APPLY_FUNC= x => 9*x
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
						OK_FUNC=greater_than_zero
						)
					),

				set_do_trend=false,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_rating_by_number_of_rated()
		{
			chart.draw(

				set_title="Rating by number of rated"+subtitle,
				set_xlegend="Number of rated players",
				set_ylegend="Average rating",

				set_data_source="country",

				set_x_series=Series(
					FIELD="R"
					),

				set_y_series=List(
					Series(
						FIELD="AVGR",
						OK_FUNC=rating_ok
						)
					),

				set_do_trend=true,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_rating_by_participation()
		{
			chart.draw(

				set_title="Rating by participation"+subtitle,
				set_xlegend="Female participation [ % ]",
				set_ylegend="Average rating",

				set_data_source="country",

				set_x_series=Series(
					FIELD="PARFR"
					),

				set_y_series=List(
					Series(
						FIELD="AVGRM",
						OK_FUNC=rating_ok
						),
					Series(
						FIELD="AVGRF",
						OK_FUNC=rating_ok
						)
					),

				set_do_trend=true,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_rating_by_titled_points()
		{
			chart.draw(

				set_title="Rating by titled points"+subtitle,
				set_xlegend="Titled points",
				set_ylegend="Average rating",

				set_data_source="country",

				set_x_series=Series(
					FIELD="TP"
					),

				set_y_series=List(
					Series(
						FIELD="AVGR",
						OK_FUNC=rating_ok
						)
					),

				set_do_trend=true,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_rating_by_titled()
		{
			chart.draw(

				set_title="Rating by titled"+subtitle,
				set_xlegend="Number of titled",
				set_ylegend="Average rating",

				set_data_source="country",

				set_x_series=Series(
					FIELD="T"
					),

				set_y_series=List(
					Series(
						FIELD="AVGR",
						OK_FUNC=rating_ok
						)
					),

				set_do_trend=true,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_rating_by_gms()
		{
			chart.draw(

				set_title="Rating by number of GMs"+subtitle,
				set_xlegend="Number of GMs",
				set_ylegend="Average rating",

				set_data_source="country",

				set_x_series=Series(
					FIELD="GM"
					),

				set_y_series=List(
					Series(
						FIELD="AVGR",
						OK_FUNC=rating_ok
						)
					),

				set_do_trend=true,

				set_limit=Limit("R",current_limit_value)

			)
		}

		def draw_gms_by_titled()
		{
			chart.draw(

				set_title="GMs by titled"+subtitle,
				set_xlegend="Number of titled",
				set_ylegend="Number of GMs",

				set_data_source="country",

				set_x_series=Series(
					FIELD="T"
					),

				set_y_series=List(
					Series(
						FIELD="GM"
						)
					),

				set_do_trend=true,

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

		val BORDER_STYLE="-fx-border-style: solid; -fx-border-width: 3px; -fx-border-radius: 10px; -fx-border-color: #00007f;"

		class MyDrawButton( text: String , callback: () => Unit , val parent: RadioButton , set_id:Int ) extends Button( text ) with HasId
		{
			id=set_id
			setStyle("-fx-background-color:#3fff3f; -fx-padding: 3px; -fx-font-size: 16px; "+BORDER_STYLE)
			setOnAction(new EventHandler[ActionEvent]{
				override def handle(e: ActionEvent)
				{
					current_callback=callback
					getchart
					parent.select(id)
					current_callback()
				}
			});
		}

		class MyFilterButton( filter: String , val parent: RadioButton , set_id:Int ) extends Button( TRANSLATE_FILTERS(filter) ) with HasId
		{
			id=set_id
			setStyle("-fx-background-color:#ffff7f; -fx-padding: 3px; -fx-font-size: 16px; "+BORDER_STYLE)
			setOnAction(new EventHandler[ActionEvent]{
				override def handle(e: ActionEvent)
				{
					current_filter=filter
					getchart
					parent.select(id)
					current_callback()
				}
			});
		}

		class SliderChangeListener extends ChangeListener[Number]
		{
			def changed(ov:ObservableValue[_ <: Number],old_val:Number,new_val:Number)
			{
				val x:Double=new_val.doubleValue()
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

		val draw_radio=new RadioButton

		val draw_buttons=List(
			new MyDrawButton("Participation",draw_participation,draw_radio,1),
			new MyDrawButton("Rating distribution",draw_rating_distribution,draw_radio,2),
			new MyDrawButton("Age distribution of rated",draw_age_distribution_of_rated,draw_radio,3),
			new MyDrawButton("Rating by number of rated",draw_rating_by_number_of_rated,draw_radio,4),
			new MyDrawButton("Rating by participation",draw_rating_by_participation,draw_radio,5),
			new MyDrawButton("Rating by titled points",draw_rating_by_titled_points,draw_radio,6),
			new MyDrawButton("Rating by titled",draw_rating_by_titled,draw_radio,7),
			new MyDrawButton("Rating by GMs",draw_rating_by_gms,draw_radio,8),
			new MyDrawButton("GMs by titled",draw_gms_by_titled,draw_radio,9)
		)

		draw_radio.buttons=draw_buttons

		draw_radio.select(1)

		val filter_radio=new RadioButton

		var i=0
		val filter_buttons:List[Button with HasId]=for(filter<-FILTERS)
			yield { i+=1; new MyFilterButton(filter,filter_radio,i) }

		filter_radio.buttons=filter_buttons

		filter_radio.select(1)

		root.setPadding(new Insets(5,5,5,5))

		root.setHgap(10)
		root.setVgap(3)

		for(button<-draw_buttons) root.getChildren.add(button)

		root.getChildren.add(new MyButton("Clear",clear))

		for(button<-filter_buttons) root.getChildren.add(button)

		root.getChildren.add(slider)

		root.getChildren.add(chart.canvas_group)

		primaryStage.setScene(new Scene(root))

		getchart
		draw_participation()

		primaryStage.show()

	}

}