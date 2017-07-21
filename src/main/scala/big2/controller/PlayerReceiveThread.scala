package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.{Card,Player,Deck};
import big2.util.SocketUtil;
import java.net.{ServerSocket,Socket,InetAddress,InetSocketAddress};
import java.io.IOException;
import scala.util.control.Breaks;
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};

object PlayerReceive extends Enumeration
{
    type PlayerReceive = Value;
    val JOIN_ROOM, UPDATE_ROOM, READY, LEAVE_ROOM, ROOM_FULL, NEW_ROUND, 
    PLAY_CARD, PASS, WIN_GAME, PENALTY, PING = Value
}

import PlayerReceive._;

/**
 * @author AveryChoke
 */
class PlayerReceiveThread(private val port:Int, hostAddress:String, 
    private val timeout:Int, private val playerName:String) extends Runnable{
  
  var socket:Socket = null;
  val address:InetSocketAddress = new InetSocketAddress(InetAddress.getByName(hostAddress),port);
  
  override def run()
  {
    try
    {
      //create socket
      socket = new Socket();
      //connect socket with timeout
      socket.connect(address, timeout);
      
      //send the data for join room and establish connection
      //send the request code
      SocketUtil.sendInt(socket, HostRequest.JOIN_ROOM.id);
      //send the player name to the host
      SocketUtil.sendString(socket, playerName);
      
      //receive different data from the host
      val loop = new Breaks;
      loop.breakable{
        while(true)
        {
          try
          {
            //get the receive code
            val receiveCode:PlayerReceive = PlayerReceive(SocketUtil.readInt(socket));
            
            receiveCode match {
              case PlayerReceive.JOIN_ROOM => joinRoom(socket);
              case PlayerReceive.UPDATE_ROOM => updateRoom(socket);
              case PlayerReceive.READY => ready(socket);
              case PlayerReceive.LEAVE_ROOM => leaveRoom(socket);
              case PlayerReceive.ROOM_FULL => roomFull(socket);
              case PlayerReceive.NEW_ROUND => newRound(socket);
              case PlayerReceive.PLAY_CARD => playCard(socket);
              case PlayerReceive.PASS => pass(socket);
              case PlayerReceive.WIN_GAME => winGame(socket);
              case PlayerReceive.PENALTY => penalty(socket);
              case PlayerReceive.PING => ping(socket);
            }
            
          } catch {
            case e:IOException => e.printStackTrace();
            println("thread die")
            //Application.restartPlayer();
            loop.break;
          }
        }
      }
      
    } catch {
      case e:IOException => e.printStackTrace();
    } finally {
      if(socket != null && !socket.isClosed())
      {
        socket.close();
      }
    }
  }
  
  //join room
  private def joinRoom(socket:Socket)
  {
    //obtain the player index
    val playerIndex:Int = SocketUtil.readInt(socket);
    println(playerIndex)
    //obtain the player list from host
    val players:Buffer[Player] = SocketUtil.readObject[ArrayBuffer[Player]](socket);
    
    //intialize
    Big2Repository.initialize(players, playerIndex, hostAddress);
    
    //change to game room scene
    Application.changeClientRoomScene();
  }
  
  //update room
  private def updateRoom(socket:Socket)
  {
    //obtain the new player list from host & update repository
    Big2Repository.players = SocketUtil.readObject[ArrayBuffer[Player]](socket);
    
    //update game room view
    Application.updateClientRoomView();
  }
  
  //ready
  private def ready(socket:Socket)
  {
    //get player index
    val playerIndex:Int = SocketUtil.readInt(socket);
    
    //update the player ready
    val player = Big2Repository.players(playerIndex);
    player.isReady = !player.isReady;
    
    //update the view
    Application.updateClientRoomReadyView(playerIndex);
  }
  
  //leave room
  private def leaveRoom(socket:Socket)
  {
    //get player index
    val playerIndex:Int = SocketUtil.readInt(socket);
    
    //remove the player from the repository
    Big2Repository.players.remove(playerIndex);
    //update my index
    if(playerIndex < Big2Repository.myIndex)
    {
      Big2Repository.myIndex -= 1;
    }
    
    //update game room view
    Application.updateClientRoomView();
  }
  
  //update room
  private def roomFull(socket:Socket)
  {
    //terminate the connection
    if(socket != null && !socket.isClosed())
    {
      socket.close();
    }
    
    //perform room full logic
    println("sorry the host game room is full")
  }
  
  //new round
  private def newRound(socket:Socket)
  {
    //obtain the cards distributed to my player
    Big2Repository.myPlayer.cards = SocketUtil.readObject[ArrayBuffer[Card]](socket);
    //get to know who start first
    Big2Repository.currentTurnIndex = SocketUtil.readInt(socket);
    
    //start gameplay
    Application.playerNewRound();
  }
  
  //play card
  private def playCard(socket:Socket)
  {
    //obtain the new table cards
    Big2Repository.tableCards = SocketUtil.readObject[ArrayBuffer[Card]](socket);
    
    //reduce the current player hand
    Big2Repository.currentPlayer.cardsAmount -= Big2Repository.tableCards.length; 
    
    //go to next player
    Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
    
    //update gameplay view
    Application.updateGameplay();
  }
  
  //pass
  private def pass(socket:Socket)
  {
    //obtain the new pass count
    Big2Repository.passCount = SocketUtil.readInt(socket);
    
    //go to next player
    Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
    
    //update gameplay view
    Application.updateGameplay();
  }
  
  //win game
  private def winGame(socket:Socket)
  {
    //get the player who won
    Big2Repository.winnerIndex = SocketUtil.readInt(socket);
    //get the penalty
    Big2Repository.penaltyIndex = SocketUtil.readInt(socket);
    
    //change to score board scene
    Application.changeScoreBoardScene();
  }
  
  //send penalty
  private def penalty(socket:Socket)
  {
    //send back to server his penalty index
    SocketUtil.sendInt(socket, Big2Repository.penaltyIndex);
  }
  
  private def ping(socket:Socket)
  {
    //reply back to host to inform everything is ok
    SocketUtil.sendInt(socket, 0);
  }
  
  def stop()
  {
    if(socket != null && !socket.isClosed())
    {
      socket.close();
    }
  }
}