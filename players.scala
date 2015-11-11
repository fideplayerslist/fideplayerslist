import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.scene.control.Label

class PlayersClass extends Application {

  override def start(primaryStage: Stage) {
    primaryStage.setTitle("FIDE Players")

    val root = new StackPane
    root.getChildren.add(new Label("FIDE players"))

    primaryStage.setScene(new Scene(root, 300, 300))
    primaryStage.show()
  }
}

object Players {
  def main(args: Array[String]) {
    Application.launch(classOf[PlayersClass], args: _*)
  }
}