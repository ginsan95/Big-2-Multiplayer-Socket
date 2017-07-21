package big2

import big2.model._
import big2.util._
import big2.controller._
import big2.repository.Big2Repository;
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.Includes._
import scalafxml.core.{ NoDependencyResolver, FXMLView, FXMLLoader }
import javafx.scene.layout.GridPane;
import scalafx.scene.control._
import scala.collection.mutable.{Buffer,ArrayBuffer};
import scala.util.control.Breaks;
import scalafx.application.Platform;

import java.net.Socket;
import java.io.{DataOutputStream,PrintWriter};
import scala.io.StdIn.{readLine,readInt};

/**
 * @author AveryChoke
 */
object Application extends JFXApp {
  
  //the threads
  var hostThread:HostServeRequestThread = null;
  var playerThread:PlayerReceiveThread = null;
  
  def startHostThread()
  {
    if(hostThread == null)
    {
      hostThread = new HostServeRequestThread(Big2Repository.PORT_NUM);
    }
    new Thread(hostThread).start();
  }
  
  def endHostThread()
  {
    if(hostThread != null)
    {
      hostThread.stop();
      hostThread = null;
    }
  }
  
  def startPlayerThread(hostAddress:String, playerName:String)
  {
    if(playerThread == null)
    {
      playerThread = new PlayerReceiveThread(Big2Repository.PORT_NUM, hostAddress, 0, playerName);
    }
    new Thread(playerThread).start();
  }
  
  def endPlayerThread()
  {
    if(playerThread != null)
    {
      playerThread.stop();
      playerThread = null;
    }
  }
  
  //the controllers
  private var gameplayControl:Option[GameplayController#Controller] = Option(null);
  private var hostRoomControl:Option[HostRoomController#Controller] = Option(null);
  private var clientRoomControl:Option[ClientRoomController#Controller] = Option(null);
  
  //main menu scene
  lazy val mainMenuScene = new Scene
  {
    val resourceIS = getClass.getResourceAsStream("view/MainMenu.fxml");
    val loader = new FXMLLoader( null, NoDependencyResolver)
    loader.load(resourceIS);
    root = loader.getRoot[javafx.scene.layout.AnchorPane];
  }
  
  //change to host room scene  
  def changeHostRoomScene()
  {
    //create the scene
    val hostRoomScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/HostRoom.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
      hostRoomControl = Option(loader.getController[HostRoomController#Controller]);
    }
    Platform.runLater {
      stage.scene = hostRoomScene;
    }
  }
  
  //update host room view
  def updateHostRoomView()
  {
    if(hostRoomControl.isDefined)
    {
      Platform.runLater {
        hostRoomControl.get.updateRoom();
      }
    }
  }
  
  //update host room ready view
  def updateHostRoomReadyView(playerIndex:Int)
  {
    if(hostRoomControl.isDefined)
    {
      Platform.runLater {
        hostRoomControl.get.updateReadyView(playerIndex);
      }
    }
  }
  
  //change to client room scene  
  def changeClientRoomScene()
  {
    //create the scene
    val clientRoomScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/ClientRoom.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
      clientRoomControl = Option(loader.getController[ClientRoomController#Controller]);
    }
    Platform.runLater {
      stage.scene = clientRoomScene;
    }
  }

  //update client room view
  def updateClientRoomView()
  { 
    if(clientRoomControl.isDefined)
    {
      Platform.runLater {
        clientRoomControl.get.updateRoom();
      }
    }
  }
  
  //update host room ready view
  def updateClientRoomReadyView(playerIndex:Int)
  {
    if(clientRoomControl.isDefined)
    {
      Platform.runLater {
        clientRoomControl.get.updateReadyView(playerIndex);
      }
    }
  }
  
  //create gameplay scene
  private def createGameplayScene():Scene =
  {
    //create the scene
    val gameplayScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/Gameplay.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
      gameplayControl = Option(loader.getController[GameplayController#Controller]);
    }
    return gameplayScene;
  }
  
  //update the gameplay view
  def updateGameplay()
  {
    if(gameplayControl.isDefined)
    {
      Platform.runLater {
        gameplayControl.get.displayData();
      }
    }
  }
  
  //change to score board scene
  def changeScoreBoardScene()
  {
    //create the scene
    val scoreScene = new Scene
    {
      val resourceIS = getClass.getResourceAsStream("view/ScoreBoard.fxml");
      val loader = new FXMLLoader( null, NoDependencyResolver)
      loader.load(resourceIS);
      root = loader.getRoot[javafx.scene.layout.AnchorPane];
    }
    //change the scene
    Platform.runLater {
      stage.scene = scoreScene; //display
    }
  }
  
  //set the stage
  stage = new PrimaryStage
  {
    title = "P4 Big 2 Game"
    scene = mainMenuScene;
  }
  
  //Kill all the threads if the user suddenly close the window
  stage.onCloseRequest() = handle
  {
    Platform.exit();
    System.exit(0);
  }
  
  //server
  def hostNewRound()
  {
    //shuffle the cards
    Deck.shuffle();
    
    //distribute the cards
    val handCards:Array[Buffer[Card]] = new Array(Big2Repository.players.length);
    for(i<-0 until handCards.length)
    {
      handCards(i) = Deck.cards.slice(i*13,(i+1)*13).sorted.toBuffer;
    }
    
    //immediately scramble the cards for security
    Deck.shuffle();
    
    //find who contain diamond 3 and start first
    val loop = new Breaks;
    loop.breakable{
      for(i <- 0 until handCards.length)
      {
        if(handCards(i).contains(Deck.d3Card))
        {
          Big2Repository.currentTurnIndex = i;
          loop.break;
        }
      }
    }
    
    //set players cards
    val sendThreads:Buffer[Thread] = new ArrayBuffer[Thread]();
    for(i<-0 until handCards.length)
    { 
      if(Big2Repository.myIndex == i) //is my cards
      {
        Big2Repository.players(i).cards = handCards(i);
      }
      else //other players card
      {
        //send the cards to other players
        val thread = new Thread(new DistributeCardsThread(
            Big2Repository.clientSockets(Big2Repository.players(i).name),
            handCards(i).asInstanceOf[ArrayBuffer[Card]],
            Big2Repository.currentTurnIndex));
        thread.start();
        sendThreads += thread;
      }
    }
    
    //wait for all player to receive their cards
    for(thread <- sendThreads)
    {
      thread.join();
    }
    
    //start the scene
    Platform.runLater {
      stage.scene = createGameplayScene();
    }
  }
  
  //client
  def playerNewRound()
  {
    //start the scene
    Platform.runLater {
      stage.scene = createGameplayScene();
    }
  }
  
  //restart the client since host die
  def restartPlayer()
  {
    Platform.runLater {
      val oldStage = stage;
      //set the stage again
      stage = new PrimaryStage
      {
        title = "P4 Big 2 Game"
        scene = mainMenuScene;
      }
      oldStage.close();
    }
  }
}