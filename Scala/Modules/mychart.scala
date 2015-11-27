package mychart

import javafx.stage._
import javafx.scene._
import javafx.scene.layout._
import javafx.scene.control._
import javafx.scene.canvas._
import javafx.scene.input._
import javafx.scene.paint._
import javafx.scene.text._
import javafx.scene.transform._
import javafx.event._
import javafx.geometry._

class MyChart(
	val TITLE_FONT_SIZE:Int=20,
	val TITLE_HEIGHT:Int=50,
	val TITLE_BACKGROUND:Color=Color.rgb(192,192,255),
	val AXIS_LEGEND_FONT_SIZE:Int=18,
	val AXIS_Y_LEGEND_WIDTH:Int=50,
	val AXIS_Y_LEGEND_BACKGROUND:Color=Color.rgb(192,192,192),
	val AXIS_Y_SCALE_WIDTH:Int=150,
	val AXIS_Y_SCALE_BACKGROUND:Color=Color.rgb(255,192,192),
	val CHART_WIDTH:Int=600,
	val CHART_HEIGHT:Int=350,
	val CHART_BACKGROUND:Color=Color.rgb(255,255,192),
	val AXIS_X_SCALE_HEIGHT:Int=100,
	val AXIS_X_SCALE_BACKGROUND:Color=Color.rgb(255,192,192),
	val AXIS_X_LEGEND_HEIGHT:Int=50,
	val AXIS_X_LEGEND_BACKGROUND:Color=Color.rgb(192,192,192),
	val LEGEND_WIDTH:Int=400,
	val LEGEND_BACKGROUND:Color=Color.rgb(192,192,192),
	val PADDING:Int=15
)
{
	val CANVAS_WIDTH=AXIS_Y_LEGEND_WIDTH+AXIS_Y_SCALE_WIDTH+CHART_WIDTH+LEGEND_WIDTH
	val CANVAS_HEIGHT=TITLE_HEIGHT+CHART_HEIGHT+AXIS_X_SCALE_HEIGHT+AXIS_X_LEGEND_HEIGHT

	val CHART_X0=AXIS_Y_LEGEND_WIDTH+AXIS_Y_SCALE_WIDTH
	val CHART_Y0=TITLE_HEIGHT
	val CHART_X1=CHART_X0+CHART_WIDTH
	val CHART_Y1=CHART_Y0+CHART_HEIGHT

	val AXIS_Y_LEGEND_X0=0
	val AXIS_Y_LEGEND_Y0=CHART_Y0
	val AXIS_Y_LEGEND_HEIGHT=CHART_HEIGHT

	val AXIS_Y_SCALE_X0=AXIS_Y_LEGEND_WIDTH	
	val AXIS_Y_SCALE_Y0=CHART_Y0
	val AXIS_Y_SCALE_HEIGHT=CHART_HEIGHT

	val AXIS_X_SCALE_X0=CHART_X0
	val AXIS_X_SCALE_Y0=CHART_Y1
	val AXIS_X_SCALE_WIDTH=CHART_WIDTH

	val AXIS_X_LEGEND_X0=CHART_X0
	val AXIS_X_LEGEND_Y0=AXIS_X_SCALE_Y0+AXIS_X_SCALE_HEIGHT
	val AXIS_X_LEGEND_WIDTH=CHART_WIDTH

	val TITLE_X0=CHART_X0
	val TITLE_Y0=0
	val TITLE_WIDTH=CHART_WIDTH

	val LEGEND_X0=CHART_X1
	val LEGEND_Y0=CHART_Y0
	val LEGEND_HEIGHT=CHART_HEIGHT

	val canvas_group=new Group()

	val canvas=new Canvas(CANVAS_WIDTH,CANVAS_HEIGHT)
	val gc=canvas.getGraphicsContext2D()

	canvas_group.getChildren().add(canvas)

	def calc_middle(c0:Int,size:Int,fontsize:Int):Int = c0+(size-fontsize)/2

	def drawtext(x: Float, y:Float, what: String, size: Float = 12)
	{
		gc.setFont(new Font(size))
		gc.setFill(Color.rgb(0,0,0))
		gc.fillText(what,x,y+size)
	}

	def remove_childs()
	{
		val numchilds=canvas_group.getChildren().size()

 		if(numchilds>1)
 		{
 			canvas_group.getChildren().remove(1,numchilds)
 		}
	}

	def drawylegendtext(what:String)
	{
		val text:Text = new Text(what)

		val text_x0=calc_middle(AXIS_Y_LEGEND_X0,AXIS_Y_LEGEND_WIDTH,AXIS_LEGEND_FONT_SIZE)+AXIS_LEGEND_FONT_SIZE
		val text_y0=CHART_Y1-PADDING

		text.setX(text_x0);
		text.setY(text_y0);

		text.setFont(new Font(AXIS_LEGEND_FONT_SIZE));

 		text.getTransforms().add(new Rotate(270, text_x0, text_y0));

 		remove_childs()

 		canvas_group.getChildren().add(text)
	}

	var title=""
	var xlegend=""
	var ylegend=""

	def drawsurround()
	{
		gc.setFill(AXIS_Y_LEGEND_BACKGROUND)
		gc.fillRect(AXIS_Y_LEGEND_X0,AXIS_Y_LEGEND_Y0,AXIS_Y_LEGEND_WIDTH,AXIS_Y_LEGEND_HEIGHT)
		gc.setFill(AXIS_Y_SCALE_BACKGROUND)
		gc.fillRect(AXIS_Y_SCALE_X0,AXIS_Y_SCALE_Y0,AXIS_Y_SCALE_WIDTH,AXIS_Y_SCALE_HEIGHT)

		gc.setFill(AXIS_X_LEGEND_BACKGROUND)
		gc.fillRect(AXIS_X_LEGEND_X0,AXIS_X_LEGEND_Y0,AXIS_X_LEGEND_WIDTH,AXIS_X_LEGEND_HEIGHT)
		gc.setFill(AXIS_X_SCALE_BACKGROUND)
		gc.fillRect(AXIS_X_SCALE_X0,AXIS_X_SCALE_Y0,AXIS_X_SCALE_WIDTH,AXIS_X_SCALE_HEIGHT)

		gc.setFill(CHART_BACKGROUND)
		gc.fillRect(CHART_X0,CHART_Y0,CHART_WIDTH,CHART_HEIGHT)

		gc.setFill(TITLE_BACKGROUND)
		gc.fillRect(TITLE_X0,TITLE_Y0,TITLE_WIDTH,TITLE_HEIGHT)

		gc.setFill(LEGEND_BACKGROUND)
		gc.fillRect(LEGEND_X0,LEGEND_Y0,LEGEND_WIDTH,LEGEND_HEIGHT)

		drawtext(CHART_X0+PADDING,calc_middle(TITLE_Y0,TITLE_HEIGHT,TITLE_FONT_SIZE),title,TITLE_FONT_SIZE)

		drawtext(CHART_X0+PADDING,calc_middle(AXIS_X_LEGEND_Y0,AXIS_X_LEGEND_HEIGHT,AXIS_LEGEND_FONT_SIZE),xlegend,AXIS_LEGEND_FONT_SIZE)

		drawylegendtext(ylegend)
	}

	def draw(
		set_title:String="",
		set_xlegend:String="",
		set_ylegend:String=""
	)
	{

		title=set_title
		xlegend=set_xlegend
		ylegend=set_ylegend

		drawsurround()

	}

	def clear()
	{
		gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT)	
		remove_childs()
	}

	
}