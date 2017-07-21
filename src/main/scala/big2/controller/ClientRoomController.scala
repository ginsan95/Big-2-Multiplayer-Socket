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
import scalafx.scene.image.{Image,ImageView};
import scalafx.scene.text.{Font,FontWeight}

@sfxml 
class ClientRoomController(
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
   
   def updateRoom()
   {
      for(i <- 0 until playersLabel.length)
      {
        playersLabel(i).setFont(Font.font(null, FontWeight.NORMAL, 24));
        if(i<Big2Repository.players.length)
        {
          val name = Big2Repository.players(i).name;
          playersLabel(i).text = s"$name";
          if(i == Big2Repository.myIndex)
          {
            playersLabel(i).setFont(Font.font(null, FontWeight.BOLD, 24));
          }
        }
        else
        {
          playersLabel(i).text = "No Player";
        }
        updateReadyView(i);
      }
   }
   
   def clientReadyButton(event:ActionEvent):Unit=
   {
     //change my ready status
     Big2Repository.myPlayer.isReady = !Big2Repository.myPlayer.isReady
     
     //update the view
     updateReadyView(Big2Repository.myIndex);
     
     //create a thread to inform the host that he is ready
     new Thread(new PlayerSendIndexThread(Big2Repository.PORT_NUM, Big2Repository.hostAddress,
        0, Big2Repository.myIndex, HostRequest.READY)).start();
   }
   
   //change the view of the ready
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
     //create a thread to inform host to leave room
     new Thread(new PlayerSendIndexThread(Big2Repository.PORT_NUM, Big2Repository.hostAddress,
        0, Big2Repository.myIndex, HostRequest.LEAVE_ROOM)).start();
     
     //leave the room
     Application.stage.scene = Application.mainMenuScene
     
     //kill the thread
     Application.endPlayerThread();
   }
   
}