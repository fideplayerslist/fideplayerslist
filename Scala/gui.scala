import javafx.application._

//import gui.PlayersClass

import drawchart.DrawChartClass

object Gui
{
	def main(args: Array[String])
	{
		Application.launch(classOf[DrawChartClass], args: _*)
	}
}