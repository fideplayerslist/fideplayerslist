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

import utils.Dir._
import utils.Parse._

class Range
{

	var MINV:Double=Series.INFINITE

	var MAXV:Double=(-Series.INFINITE)

	def force_min(what: Double)
	{
		if(what < Series.INFINITE)
		{
			MINV=what
		}
	}

	def force_max(what: Double)
	{
		if(what > (-Series.INFINITE))
		{
			MAXV=what
		}
	}

	def add(what: Double)
	{
		if(what > MAXV)
		{
			MAXV=what
		}

		if(what < MINV)
		{
			MINV=what
		}

	}

	def RANGE:Double=MAXV-MINV
}

object Series
{
	val INFINITE=1e20
}

case class Series(
	val FIELD:String="",
	val OK_FUNC:(Double)=>Boolean=(x => true),
	val APPLY_FUNC:(Double)=>Double=(x => x),
	val FORCE_MIN:Double=Series.INFINITE,
	val FORCE_MAX:Double=(-Series.INFINITE),
	val COLOR:Color=Color.rgb(255,0,0)
)
{
	val RANGE=new Range
}

class MyChart(
	var DATA_SOURCE_TO_PATH_FUNC:(String)=>String = (x => x),
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
	val PADDING:Int=15,
	val BOX_WIDTH:Int=10
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
	var data_source=""
	var data_source_path=""
	var x_series:Series=null
	var y_series:List[Series]=null
	var XYSS:scala.collection.mutable.ArrayBuffer[Map[Double,Double]]=null
	var MIN_X=0.0
	var MAX_X=0.0
	var MIN_Y=0.0
	var MAX_Y=0.0
	var FACTOR_X=0.0
	var FACTOR_Y=0.0

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

		/*gc.setFill(CHART_BACKGROUND)
		gc.fillRect(CHART_X0,CHART_Y0,CHART_WIDTH,CHART_HEIGHT)*/

		gc.setFill(TITLE_BACKGROUND)
		gc.fillRect(TITLE_X0,TITLE_Y0,TITLE_WIDTH,TITLE_HEIGHT)

		gc.setFill(LEGEND_BACKGROUND)
		gc.fillRect(LEGEND_X0,LEGEND_Y0,LEGEND_WIDTH,LEGEND_HEIGHT)

		drawtext(CHART_X0+PADDING,calc_middle(TITLE_Y0,TITLE_HEIGHT,TITLE_FONT_SIZE),title,TITLE_FONT_SIZE)

		drawtext(CHART_X0+PADDING,calc_middle(AXIS_X_LEGEND_Y0,AXIS_X_LEGEND_HEIGHT,AXIS_LEGEND_FONT_SIZE),xlegend,AXIS_LEGEND_FONT_SIZE)

		drawylegendtext(ylegend)
	}

	def parse_XYSS():Boolean =
	{

		XYSS=scala.collection.mutable.ArrayBuffer[Map[Double,Double]]()

		for(series<-y_series)
		{
			XYSS+=Map[Double,Double]()
		}

		val records=parseTxtSmart(data_source_path)

		val X_FIELD=x_series.FIELD

		for(record<-records)
		{
			if(record.contains(X_FIELD))
			{
				val x_str=record(X_FIELD)

				if(isValue(x_str))
				{
					val x_orig=myToDouble(x_str)

					val x=x_series.APPLY_FUNC(x_orig)

					val x_ok=x_series.OK_FUNC(x)

					if(x_ok)
					{

						var i=0

						for(series<-y_series)
						{

							val Y_FIELD=series.FIELD

							if(record.contains(Y_FIELD))
							{
								val y_str=record(Y_FIELD)

								if(isValue(y_str))
								{

									val y_orig=myToDouble(y_str)

									val y=series.APPLY_FUNC(y_orig)

									val y_ok=series.OK_FUNC(y)

									if(y_ok)
									{

										XYSS(i)+=(x->y)

										println("series "+i+" added x "+x+" y "+y)

										x_series.RANGE.add(x)
										y_series(i).RANGE.add(y)

									}

								}
							}

							i+=1
						}

					}
				}
			}
		}

		x_series.RANGE.force_min(x_series.FORCE_MIN)
		x_series.RANGE.force_max(x_series.FORCE_MAX)

		val X_RANGE=x_series.RANGE.RANGE

		if(X_RANGE==0)
		{
			println("error: x range zero")
			return false
		}

		FACTOR_X=CHART_WIDTH/X_RANGE

		MIN_X=x_series.RANGE.MINV
		MAX_X=x_series.RANGE.MAXV

		println("x range : "+MIN_X+" - "+MAX_X+" , factor "+FACTOR_X)

		val Y_RANGE=new Range

		var i=0		
		for(series<-y_series)
		{

			y_series(i).RANGE.force_min(series.FORCE_MIN)
			y_series(i).RANGE.force_max(series.FORCE_MAX)

			println("y range "+i+" : "+series.RANGE.MINV+" - "+series.RANGE.MAXV)

			Y_RANGE.add(y_series(i).RANGE.MINV)
			Y_RANGE.add(y_series(i).RANGE.MAXV)

			i+=1
		}

		if(Y_RANGE.RANGE==0)
		{
			println("error: y range zero")
			return false
		}

		MIN_Y=Y_RANGE.MINV
		MAX_Y=Y_RANGE.MAXV

		FACTOR_Y=CHART_HEIGHT/Y_RANGE.RANGE

		println("overall y range: "+MIN_Y+" - "+MAX_Y+" , factor "+FACTOR_Y)

		return true
	}

	def drawbox(x: Double, y: Double, c: Color)
	{
		gc.setFill(c)
		gc.fillRect(x-BOX_WIDTH/2,y-BOX_WIDTH/2,BOX_WIDTH,BOX_WIDTH)
	}

	def cx(x: Double):Double = CHART_X0+(x-MIN_X)*FACTOR_X
	def cy(y: Double):Double = CHART_Y0+CHART_HEIGHT-(y-MIN_Y)*FACTOR_Y

	def drawseries()
	{
		var i=0
		for(series<-XYSS)
		{
			for((x,y)<-series)
			{
				drawbox(cx(x),cy(y),y_series(i).COLOR)
			}
			i+=1
		}
	}

	def draw(
		set_title:String="",
		set_xlegend:String="",
		set_ylegend:String="",
		set_data_source:String=null,
		set_data_source_path:String=null,
		set_x_series:Series=null,
		set_y_series:List[Series]=null
	)
	{

		title=set_title
		xlegend=set_xlegend
		ylegend=set_ylegend
		data_source=set_data_source
		data_source_path=set_data_source_path
		x_series=set_x_series
		y_series=set_y_series

		if(data_source_path==null)
		{
			if(data_source==null)
			{
				println("error: no data source")
				return
			}
			else
			{
				data_source_path=DATA_SOURCE_TO_PATH_FUNC(data_source)
			}
		}

		println(s"data source path: $data_source_path")

		if(x_series==null)
		{
			println("error: missing x series")
			return
		}

		println("x series "+x_series.FIELD)

		if(y_series==null)
		{
			println("error: missing y series")
			return
		}

		var i=0
		for(s<-y_series)
		{
			i+=1
			println("y series "+i+" "+s.FIELD)
		}

		if(!parse_XYSS())
		{
			return
		}

		drawseries()

		drawsurround()

	}

	def clear()
	{
		gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT)	
		remove_childs()
	}

	
}