package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.Player;
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafxml.core.macros.sfxml
import big2.repository.Big2Repository;
import scalafx.application.JFXApp
import scalafx.Includes._
import scalafxml.core.{ NoDependencyResolver, FXMLView, FXMLLoader }
import scalafx.scene.control._
import scalafx.scene.control.Alert.AlertType
import scala.collection.mutable.{Buffer,ArrayBuffer}

@sfxml
class MainMenuController (private val playerText: TextField, private val hostText: TextField
    , private val hostIPText: TextField) extends JFXApp {
  
  def hostButtonClick(event:ActionEvent):Unit={
    //initialize the repository
    if(hostText.getText=="")
    {
      new Alert(AlertType.Warning) {
        initOwner(stage)
        title = "Warning Dialog"
        headerText = "Text field is empty!"
        contentText = "Please make sure the text field is not empty"
      }.showAndWait()
    }
    else
    {
      val buf = new ArrayBuffer[Player]();
      buf += new Player(hostText.getText, Big2Repository.STARTING_SCORE);
      Big2Repository.initialize(buf, 0, "localhost");
      //start the server thread
      Application.startHostThread();
      
      //change to game room scene
      Application.changeHostRoomScene();
    }
  }
  
  def playerButtonClick(event:ActionEvent):Unit={
    if( (playerText.getText=="")||(hostIPText.getText==""))
    {
      new Alert(AlertType.Warning) {
        initOwner(stage)
        title = "Warning Dialog"
        headerText = "Text field is empty!"
        contentText = "Please make sure both text fields are not empty"
      }.showAndWait()
    }
    else
    {
      //start the client thread
      Application.startPlayerThread(hostIPText.getText, playerText.getText);
    }
  }
  
  def ruleButton(event:ActionEvent):Unit=
  {
    
  }
    
}

