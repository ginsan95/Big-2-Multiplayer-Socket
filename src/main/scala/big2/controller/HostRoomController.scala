package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.Player;
import scalafx.event.ActionEvent
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafxml.core.macros.sfxml
import scala.collection.mutable.ArrayBuffer;
import big2.repository.Big2Repository;
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene._
import scalafx.Includes._
import scalafx.scene.control._
import scala.collection.mutable.{Buffer,ArrayBuffer};
import scalafx.application.Platform;
import scalafx.scene.text.Text
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.image.{Image,ImageView};

@sfxml 
class HostRoomController(
    private val hostLabel:Label,
    private val player1Label:Label,
    private val player2Label:Label,
    private val player3Label:Label,
    private val readyLabel1:Label,
    private val readyLabel2:Label,
    private val readyLabel3:Label,
    private val imageView1:ImageView,
    private val imageView2:ImageView)
{  
   //define image
   private val resource1 = getClass.getResourceAsStream("/big2/view/big2logo.png");
   private val resource2 = getClass.getResourceAsStream("/big2/view/daidee.png");
   private val big2Logo = new Image(resource1);
   private val bigDee = new Image(resource2);
   imageView1.image = big2Logo;
   imageView2.image = bigDee;
   
   //define labels
   private val playersLabel:Array[Label] = new Array[Label](4);
   playersLabel(0) = hostLabel;
   playersLabel(1) = player1Label;
   playersLabel(2) = player2Label;
   playersLabel(3) = player3Label;
   
   updateRoom();
   
   def startGameButton(event:ActionEvent):Unit=
   {
      if(Big2Repository.players.length >= 4 && Big2Repository.isAllReady)
      {
         Application.hostNewRound();
      }
      else
      {
         new Alert(AlertType.Warning) {
         initOwner(Application.stage)
         title = "Warning Dialog"
         headerText = "Unable to start game"
         contentText = "The room is not full or everyone is not ready"
       }.showAndWait()
      }
   }
   
   def updateRoom()
   {
      for(i <- 0 until playersLabel.length)
      {
        if(i<Big2Repository.players.length)
        {
          val name = Big2Repository.players(i).name;
          playersLabel(i).text = s"$name";
        }
        else
        {
          playersLabel(i).text = "No Player";
        }
        updateReadyView(i);
      }
   }
   
   def updateReadyView(playerIndex:Int)
   {
     val playerReadyLabel:Label = playerIndex match {
       case 1 => readyLabel1;
       case 2 => readyLabel2;
       case 3 => readyLabel3;
       case _ => return;
     }
     
     if(playerIndex<Big2Repository.players.length && Big2Repository.players(playerIndex).isReady)
     {
       playerReadyLabel.setStyle("-fx-border-color:black; -fx-background-color: green;");
     }
     else
     {
       playerReadyLabel.setStyle("-fx-border-color:black; -fx-background-color: grey;");
     }
   }
   
   def leaveRoomButton(event:ActionEvent):Unit=
   {
      Application.stage.scene = Application.mainMenuScene;
      
      //kill the thread
     Application.endHostThread();
   }
}