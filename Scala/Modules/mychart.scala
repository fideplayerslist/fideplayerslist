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
import utils.Log._

case class Limit(val FIELD_NAME:String="",val LIMIT:Double=0.0){}

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
	val COLOR:Color=null
)
{
	val RANGE=new Range
	var Alpha=0.0
	var Beta=0.0
	var TRUE_MIN_V=0.0
	var TRUE_MAX_V=0.0
}

object MyChart
{
	val GRAY=Color.rgb(220,220,220)
	val PINK=Color.rgb(255,192,192)
	val TITLE=Color.rgb(200,255,200)
	val YELLOW=Color.rgb(255,255,192)
	val GRID=Color.rgb(192,192,64)
}

class MyChart(
	var DATA_SOURCE_TO_PATH_FUNC:(String)=>String = (x => x),
	val TITLE_FONT_SIZE:Int=20,
	val TITLE_HEIGHT:Int=50,
	val TITLE_BACKGROUND:Color=MyChart.TITLE,
	val AXIS_LEGEND_FONT_SIZE:Int=18,
	val AXIS_Y_LEGEND_WIDTH:Int=50,
	val AXIS_Y_LEGEND_BACKGROUND:Color=MyChart.GRAY,
	val AXIS_Y_SCALE_WIDTH:Int=100,
	val AXIS_Y_SCALE_BACKGROUND:Color=MyChart.PINK,
	val CHART_WIDTH:Int=750,
	val CHART_HEIGHT:Int=350,
	val CHART_BACKGROUND:Color=MyChart.YELLOW,
	val AXIS_X_SCALE_HEIGHT:Int=80,
	val AXIS_X_SCALE_BACKGROUND:Color=MyChart.PINK,
	val AXIS_X_LEGEND_HEIGHT:Int=50,
	val AXIS_X_LEGEND_BACKGROUND:Color=MyChart.GRAY,
	val LEGEND_WIDTH:Int=400,
	val LEGEND_BACKGROUND:Color=MyChart.GRAY,
	val LEGEND_FONT_SIZE:Int=18,
	val LEGEND_STEP:Int=40,
	val PADDING:Int=10,
	val BOX_WIDTH:Int=10,
	val GRID_COLOR:Color=MyChart.GRID,
	val SCALE_FONT_SIZE:Int=14,
	val FONT_FAMILY:String="Courier New",
	val KEY_TRANSLATIONS:Map[String,String]=Map[String,String]()
)
{

	do_log=false

	var title=""
	var xlegend=""
	var ylegend=""
	var data_source=""
	var data_source_path=""
	var x_series:Series=null
	var y_series:List[Series]=null
	var do_trend:Boolean=true
	var limit:Limit=new Limit
	var XYSS:scala.collection.mutable.ArrayBuffer[Map[Double,Double]]=null
	var MIN_X=0.0
	var MAX_X=0.0
	var MIN_Y=0.0
	var MAX_Y=0.0
	var FACTOR_X=0.0
	var FACTOR_Y=0.0
	var STEP_X=0.0
	var STEP_Y=0.0

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
	val AXIS_Y_SCALE_X1=AXIS_Y_SCALE_X0+AXIS_Y_SCALE_WIDTH
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

	def drawline(x0: Double, y0: Double, x1: Double, y1: Double, c: Color)
	{
		gc.setLineWidth(3)
		gc.setStroke(c)
		gc.strokeLine(x0,y0,x1,y1)
	}

	def drawtext(x: Double, y:Double, what: String, size: Double = 12)
	{
		gc.setFont(new Font(FONT_FAMILY,size))
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

		val text_x0=calc_middle(AXIS_Y_LEGEND_X0,AXIS_Y_LEGEND_WIDTH,AXIS_LEGEND_FONT_SIZE)+AXIS_LEGEND_FONT_SIZE-5
		val text_y0=CHART_Y1-2*PADDING

		text.setX(text_x0);
		text.setY(text_y0);

		text.setFont(new Font(FONT_FAMILY,AXIS_LEGEND_FONT_SIZE));

 		text.getTransforms().add(new Rotate(270, text_x0, text_y0));

 		remove_childs()

 		canvas_group.getChildren().add(text)
	}

	def drawxscale()
	{

		var x=MIN_X+STEP_X

		var i=0

		while(x < MAX_X)
		{

			val xc=cx(x)-SCALE_FONT_SIZE/2

			drawtext(xc,AXIS_X_SCALE_Y0+PADDING+i*SCALE_FONT_SIZE*2,""+x.toInt,SCALE_FONT_SIZE)

			i+=1

			if(i>1)
			{
				i=0
			}

			x+=STEP_X

		}

	}

	def drawyscale()
	{

		var y=MIN_Y+STEP_Y

		while(y < MAX_Y)
		{

			val yc=cy(y)-SCALE_FONT_SIZE/2

			val what=""+y

			val whatnice=what.replaceAll("\\.0$","")

			drawtext(AXIS_Y_SCALE_X1-PADDING-whatnice.length*SCALE_FONT_SIZE,yc,whatnice,SCALE_FONT_SIZE)

			y+=STEP_Y

		}

	}

	def drawscales()
	{

		drawyscale()

		drawxscale()

	}

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

		gc.setFill(TITLE_BACKGROUND)
		gc.fillRect(0,TITLE_Y0,CANVAS_WIDTH,TITLE_HEIGHT)
		gc.fillRect(0,CHART_Y1,CHART_X0,AXIS_X_SCALE_HEIGHT+AXIS_X_LEGEND_HEIGHT)
		gc.fillRect(CHART_X1,CHART_Y1,LEGEND_WIDTH,AXIS_X_SCALE_HEIGHT+AXIS_X_LEGEND_HEIGHT)

		gc.setFill(LEGEND_BACKGROUND)
		gc.fillRect(LEGEND_X0,LEGEND_Y0,LEGEND_WIDTH,LEGEND_HEIGHT)

		drawtext(2*PADDING,calc_middle(TITLE_Y0,TITLE_HEIGHT,TITLE_FONT_SIZE)-3,title,TITLE_FONT_SIZE)

		drawtext(CHART_X0+2*PADDING,calc_middle(AXIS_X_LEGEND_Y0,AXIS_X_LEGEND_HEIGHT,AXIS_LEGEND_FONT_SIZE)-3,xlegend,AXIS_LEGEND_FONT_SIZE)

		drawylegendtext(ylegend)

		drawscales()

	}

	val GRID_STEPS=List(5,10,20,25,75)

	def calc_step(range: Double):Double =
	{

		var grid_step_i=0

		var mult:Double=1.0

		var done=false

		var current_step:Double=1.0

		do
		{
			done=true
			current_step=GRID_STEPS(grid_step_i)*mult
			val current_no=range/current_step
			if(current_no>15)
			{
				if(grid_step_i<(GRID_STEPS.length-1))
				{
					grid_step_i=grid_step_i+1
				}
				else
				{
					grid_step_i=0
					mult=mult*10
				}
				done=false
			}
			if(current_no<5)
			{
				if(grid_step_i>0)
				{
					grid_step_i=grid_step_i-1
				}
				else
				{
					grid_step_i=GRID_STEPS.length-1
					mult=mult/10
				}
				done=false
			}
		}while(!done)
		
		current_step

	}

	def step_correct_down(what: Double,step: Double):Double = ((what/step).floor-1)*step
	def step_correct_up(what: Double,step: Double):Double = ((what/step).floor+2)*step

	def calc_linear(XYS: Map[Double,Double]): List[Double] =
	{
		var Sx:Double=0.0
		var Sy:Double=0.0
		var Sxx:Double=0.0
		var Sxy:Double=0.0
		var Syy:Double=0.0
		var n=0
		for((k,v)<-XYS)
		{
			val x=k
			val y=v
			Sx=Sx+x
			Sy=Sy+y
			Sxx=Sxx+x*x
			Sxy=Sxy+x*y
			Syy=Syy+y*y
			n=n+1
		}

		val denomBeta=(n*Sxx)-(Sx*Sx)

		if((denomBeta==0)||(n==0))
		{
			log("warning: linear trend could not be calculated")
			return(List(0,0))
		}
		
		val Beta=(n*Sxy-Sx*Sy)/(denomBeta)
		val Alpha=(Sy/n)-(Beta*Sx/n)
	
		List(Alpha,Beta)
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

										var limit_ok=true
										if(limit.FIELD_NAME!="")
										{
											if(record.contains(limit.FIELD_NAME))
											{
												val limit_str=record(limit.FIELD_NAME)
												if(isValue(limit_str))
												{
													val limit_value=myToDouble(limit_str)

													if(limit_value < limit.LIMIT)
													{
														limit_ok=false
													}
												}
												else
												{
													limit_ok=false
												}
											}
											else
											{
												limit_ok=false
											}
										}

										log("limit ok "+limit_ok)
										if(limit_ok)
										{
											XYSS(i)+=(x->y)

											log("series "+i+" added x "+x+" y "+y)

											x_series.RANGE.add(x)
											y_series(i).RANGE.add(y)
										}

									}

								}
							}

							i+=1
						}

					}
				}
			}
		}

		log("building series done")

		x_series.RANGE.force_min(x_series.FORCE_MIN)
		x_series.RANGE.force_max(x_series.FORCE_MAX)

		log("force min max done")

		val X_RANGE=x_series.RANGE.RANGE

		if((X_RANGE.abs < 1.0e-6) || (X_RANGE.abs> 1.0e6 ))
		{
			log("error: x range extreme "+X_RANGE)
			return false
		}

		log("calculating step for range "+X_RANGE)

		STEP_X=calc_step(X_RANGE)

		x_series.TRUE_MIN_V=x_series.RANGE.MINV
		x_series.TRUE_MAX_V=x_series.RANGE.MAXV

		log("step correction")

		MIN_X=step_correct_down(x_series.TRUE_MIN_V,STEP_X)
		MAX_X=step_correct_up(x_series.TRUE_MAX_V,STEP_X)

		val X_RANGE_CORRECTED=MAX_X-MIN_X

		FACTOR_X=CHART_WIDTH/X_RANGE_CORRECTED

		log("x range : "+MIN_X+" - "+MAX_X+" , factor "+FACTOR_X+" , step "+STEP_X)

		val Y_RANGE=new Range

		var i=0		
		for(series<-y_series)
		{

			log("adjusting series "+i)

			y_series(i).TRUE_MIN_V=y_series(i).RANGE.MINV
			y_series(i).TRUE_MAX_V=y_series(i).RANGE.MAXV

			y_series(i).RANGE.force_min(series.FORCE_MIN)
			y_series(i).RANGE.force_max(series.FORCE_MAX)

			log("y range "+i+" : "+y_series(i).TRUE_MIN_V+" - "+y_series(i).TRUE_MAX_V)

			Y_RANGE.add(y_series(i).TRUE_MIN_V)
			Y_RANGE.add(y_series(i).TRUE_MAX_V)

			val trend=calc_linear(XYSS(i))

			y_series(i).Alpha=trend(0)
			y_series(i).Beta=trend(1)

			log("Alpha "+y_series(i).Alpha+" Beta "+y_series(i).Beta)

			i+=1
		}

		if(Y_RANGE.RANGE==0)
		{
			log("error: y range zero")
			return false
		}

		STEP_Y=calc_step(Y_RANGE.RANGE)

		MIN_Y=step_correct_down(Y_RANGE.MINV,STEP_Y)
		MAX_Y=step_correct_up(Y_RANGE.MAXV,STEP_Y)

		val Y_RANGE_CORRECTED=MAX_Y-MIN_Y

		FACTOR_Y=CHART_HEIGHT/Y_RANGE_CORRECTED

		log("overall y range: "+MIN_Y+" - "+MAX_Y+" , factor "+FACTOR_Y+" , step "+STEP_Y)

		return true
	}

	def drawbox(x: Double, y: Double, c: Color)
	{
		gc.setFill(c)
		gc.fillRect(x-BOX_WIDTH/2,y-BOX_WIDTH/2,BOX_WIDTH,BOX_WIDTH)
	}

	def cx(x: Double):Double = CHART_X0+(x-MIN_X)*FACTOR_X
	def cy(y: Double):Double = CHART_Y0+CHART_HEIGHT-(y-MIN_Y)*FACTOR_Y

	def drawgrid()
	{

		gc.setFill(CHART_BACKGROUND)
		gc.fillRect(CHART_X0,CHART_Y0,CHART_WIDTH,CHART_HEIGHT)

		var x=MIN_X

		while(x<=MAX_X)
		{

			val xc=cx(x)

			drawline(xc,CHART_Y0,xc,CHART_Y1,GRID_COLOR)

			x+=STEP_X
		}

		var y=MIN_Y

		while(y<=MAX_Y)
		{

			val yc=cy(y)

			drawline(CHART_X0,yc,CHART_X1,yc,GRID_COLOR)

			y+=STEP_Y
		}
	}

	val DEFAULT_COLORS=List(
		Color.rgb(255,0,0),
		Color.rgb(0,0,255),
		Color.rgb(0,255,0)
		)

	def get_color_i(i:Int):Color =
	{
		val COLOR=y_series(i).COLOR

		if(COLOR==null)
		{
			if(i < DEFAULT_COLORS.length)
			{
				return DEFAULT_COLORS(i)
			}
			else
			{
				return Color.rgb(0,0,0)
			}
		}
		else
		{
			COLOR
		}
	}

	def drawseries()
	{

		var i=0
		for(series<-XYSS)
		{
			for((x,y)<-series)
			{
				drawbox(cx(x),cy(y),get_color_i(i))
			}
			i+=1
		}
	}

	def drawtrends()
	{
		var i=0
		for(series<-y_series)
		{
			val trend_x0=cx(x_series.TRUE_MIN_V)
			val trend_y0=cy(series.Alpha+x_series.TRUE_MIN_V*series.Beta)
			val trend_x1=cx(x_series.TRUE_MAX_V)
			val trend_y1=cy(series.Alpha+(x_series.TRUE_MAX_V)*series.Beta)

			drawline(trend_x0,trend_y0,trend_x1,trend_y1,get_color_i(i))

			i+=1
		}
	}

	def drawlegend()
	{
		for(i <- 0 to XYSS.length-1)
		{

			val legend_step_factor=if(do_trend) 2 else 1

			var cy=LEGEND_Y0+legend_step_factor*LEGEND_STEP*i+2*PADDING
			drawbox(LEGEND_X0+2*PADDING,cy,get_color_i(i))
			drawtext(LEGEND_X0+2*PADDING+2*BOX_WIDTH,cy-BOX_WIDTH-2,KEY_TRANSLATIONS(y_series(i).FIELD),LEGEND_FONT_SIZE)
			if(do_trend)
			{
				cy=LEGEND_Y0+2*LEGEND_STEP*i+LEGEND_STEP+2*PADDING
				drawline(LEGEND_X0+2*PADDING,cy,LEGEND_X0+2*PADDING+2*BOX_WIDTH,cy,get_color_i(i))
				drawtext(LEGEND_X0+2*PADDING+3*BOX_WIDTH,cy-BOX_WIDTH-2,"linear, beta "+"%.5f".format(y_series(i).Beta),LEGEND_FONT_SIZE)
			}
		}
	}

	def draw(
		set_title:String="",
		set_xlegend:String="",
		set_ylegend:String="",
		set_data_source:String=null,
		set_data_source_path:String=null,
		set_x_series:Series=null,
		set_y_series:List[Series]=null,
		set_do_trend:Boolean=true,
		set_limit:Limit=new Limit()
	)
	{

		title=set_title
		xlegend=set_xlegend
		ylegend=set_ylegend
		data_source=set_data_source
		data_source_path=set_data_source_path
		x_series=set_x_series
		y_series=set_y_series
		do_trend=set_do_trend
		limit=set_limit

		if(data_source_path==null)
		{
			if(data_source==null)
			{
				log("error: no data source")
				return
			}
			else
			{
				data_source_path=DATA_SOURCE_TO_PATH_FUNC(data_source)
			}
		}

		log(s"data source path: $data_source_path")

		if(x_series==null)
		{
			log("error: missing x series")
			return
		}

		log("x series "+x_series.FIELD)

		if(y_series==null)
		{
			log("error: missing y series")
			return
		}

		var i=0
		for(s<-y_series)
		{
			i+=1
			log("y series "+i+" "+s.FIELD)
		}

		log("parsing data")

		if(!parse_XYSS())
		{
			log("parsing data failed, chart drawing aborted")
			return
		}

		log("drawing grid")

		drawgrid()

		log("drawing series")

		drawseries()

		if(do_trend)
		{
			log("drawing trends")

			drawtrends()
		}

		log("drawing surround")

		drawsurround()

		log("drawing legend")

		drawlegend()

		log("chart drawn ok")

	}

	def clear()
	{
		gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT)
		remove_childs()
	}

	
}