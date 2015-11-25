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
	val TITLE_HEIGHT:Int=50,
	val AXIS_Y_LEGEND_WIDTH:Int=50,
	val AXIS_Y_SCALE_WIDTH:Int=150,
	val CHART_WIDTH:Int=800,
	val CHART_HEIGHT:Int=350,
	val AXIS_X_SCALE_HEIGHT:Int=100,
	val AXIS_X_LEGEND_HEIGHT:Int=50,
	val LEGEND_WIDTH:Int=200,
	val TITLE_FONT_SIZE:Int=20,
	val AXIS_LEGEND_FONTSIZE:Int=18,
	val PADDING:Int=15
)
{
	val CANVAS_WIDTH=AXIS_Y_LEGEND_WIDTH+AXIS_Y_SCALE_WIDTH+CHART_WIDTH+LEGEND_WIDTH
	val CANVAS_HEIGHT=TITLE_HEIGHT+CHART_HEIGHT+AXIS_X_SCALE_HEIGHT+AXIS_X_LEGEND_HEIGHT

	val CHART_X0=AXIS_Y_LEGEND_WIDTH+AXIS_Y_SCALE_WIDTH
	val CHART_X1=CHART_X0+CHART_WIDTH
	val CHART_Y0=TITLE_HEIGHT
	val CHART_Y1=CHART_Y0+CHART_HEIGHT
	val AXIS_X_SCALE_Y0=CHART_Y1
	val AXIS_X_LEGEND_Y0=AXIS_X_SCALE_Y0+AXIS_X_SCALE_HEIGHT

	val canvas_group=new Group()

	val canvas=new Canvas(CANVAS_WIDTH,CANVAS_HEIGHT)
	val gc=canvas.getGraphicsContext2D()

	canvas_group.getChildren().add(canvas)

	def drawtext(x: Float, y:Float, what: String, size: Float = 12)
	{
		gc.setFont(new Font(size))
		gc.setFill(Color.rgb(0,0,0))
		gc.fillText(what,x,y+size/2)
	}

	def remove_childs()
	{
		val numchilds=canvas_group.getChildren().size()

 		if(numchilds>1)
 		{
 			canvas_group.getChildren().remove(1,numchilds)
 		}
	}

	def draw(
		title:String="",
		xlegend:String="",
		ylegend:String=""
	)
	{

		drawtext(CHART_X0,PADDING,title,TITLE_FONT_SIZE)

		drawtext(CHART_X0,AXIS_X_LEGEND_Y0+PADDING,xlegend,AXIS_LEGEND_FONTSIZE)

		val text:Text = new Text(ylegend);
		val text_x0=PADDING+AXIS_LEGEND_FONTSIZE
		text.setX(text_x0);
		text.setY(CHART_Y1);
		text.setFont(new Font(AXIS_LEGEND_FONTSIZE));

 		text.getTransforms().add(new Rotate(270, text_x0, CHART_Y1));

 		

 		remove_childs()

 		canvas_group.getChildren().add(text)

	}

	def clear()
	{
		gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT)	
		remove_childs()
	}

	
}