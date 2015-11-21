import javafx.application._

import gui.PlayersClass

object Gui
{
	def main(args: Array[String])
	{
		Application.launch(classOf[PlayersClass], args: _*)
	}
}