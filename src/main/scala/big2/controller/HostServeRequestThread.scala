package big2.controller

import big2.Application;
import big2.repository.Big2Repository;
import big2.model.{Card,Player};
import big2.util.SocketUtil;
import java.net.{ServerSocket,Socket};
import java.io.IOException;
import scala.util.control.Breaks;
import java.util.concurrent.{Executors,ExecutorService,TimeUnit,CountDownLatch};
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};
import java.io.{DataInputStream,PrintWriter,ObjectInputStream};

/**
 * @author AveryChoke
 */

object HostRequest extends Enumeration
{
    type HostRequest = Value;
    val JOIN_ROOM, READY, LEAVE_ROOM, PLAY_CARD, PASS, WIN_GAME = Value
}

import HostRequest._;

class HostServeRequestThread(private val port:Int) extends Runnable {
  
  private var serverSocket:ServerSocket = null;
  private var clientSocket: Socket = null;
  private var exec:ExecutorService = null;
  
  override def run()
  {
    try
    {
      serverSocket = new ServerSocket(port);
      //dynamic thread pools based on computer cpu
      exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()+1);
      
      val loop = new Breaks;
      loop.breakable{
        while(true)
        {
          try
          {
            //obtain client socket
            clientSocket = serverSocket.accept();
            
            //get the request code
            val requestCode:HostRequest = HostRequest(SocketUtil.readInt(clientSocket));
            
            requestCode match {
              case HostRequest.JOIN_ROOM =>
                joinRoom(clientSocket,exec);
              case HostRequest.READY =>
                ready(clientSocket,exec);
              case HostRequest.LEAVE_ROOM =>
                leaveRoom(clientSocket,exec);
              case HostRequest.PLAY_CARD => 
                playCard(clientSocket,exec);
              case HostRequest.PASS =>
                pass(clientSocket,exec);
              case HostRequest.WIN_GAME =>
                winGame(clientSocket,exec);
            }
            
          } catch {
            case e:IOException => e.printStackTrace();
            println("thread die")
            loop.break;
          }
        }
      }
    } catch {
      case e:IOException => e.printStackTrace();
    } finally {
      if(exec != null && !exec.isShutdown())
      {
         exec.shutdown();
         exec.awaitTermination(Long.MaxValue, TimeUnit.SECONDS);
      }
      Big2Repository.clientSockets.foreach(keyVal => 
        if(keyVal._2 != null && keyVal._2.isClosed())
        {
          keyVal._2.close();
        }
      )
      if(serverSocket != null && !serverSocket.isClosed())
      {
        serverSocket.close();
      }
    }
  }
  
  //join room
  private def joinRoom(clientSocket:Socket, exec:ExecutorService)
  {
    val playerIndex:Int = Big2Repository.players.length; //the player index
    println(playerIndex)
    //check if the host already maxed out with players
    if(playerIndex < 4)
    {
      //read data
      var playerName:String = SocketUtil.readString(clientSocket); //get the name
      //change the name if exist
      playerName = changeName(playerName);
      //add the player
      Big2Repository.addPlayer(playerIndex, new Player(playerName, Big2Repository.STARTING_SCORE));
      
      //send data to the player back
      //send the receive code
      SocketUtil.sendInt(clientSocket, PlayerReceive.JOIN_ROOM.id);
      //send the player index
      SocketUtil.sendInt(clientSocket, playerIndex);
      //send the player list
      SocketUtil.sendObject[ArrayBuffer[Player]](
                  clientSocket,Big2Repository.players.asInstanceOf[ArrayBuffer[Player]]);
      
      val latch:CountDownLatch = new CountDownLatch(Big2Repository.clientSockets.size);
      
      //tell other players his existence
      Big2Repository.clientSockets.keys.foreach
      {
        name =>
          exec.execute(new Runnable{
            override def run() {
              val socket:Socket = Big2Repository.clientSockets(name);
              //send the receiveCode
              SocketUtil.sendInt(socket, PlayerReceive.UPDATE_ROOM.id);
              //send the players list to the other players
              SocketUtil.sendObject[ArrayBuffer[Player]](
                  socket,Big2Repository.players.asInstanceOf[ArrayBuffer[Player]]);
              //reduce latch
              latch.countDown();
            }
          });
      }
      
      //save to the map
      Big2Repository.clientSockets += playerName -> clientSocket;
      
      //update game room view
      Application.updateHostRoomView();
      
      //wait for all players to successfully the players list
      //important to prevent read-write modify race condition - old players list overwrite new 1
      latch.await();
    }
    else
    {
      SocketUtil.sendInt(clientSocket, PlayerReceive.ROOM_FULL.id);
    }
  }
  
  //ready
  private def ready(clientSocket:Socket, exec:ExecutorService)
  {
    //get player index
    val playerIndex:Int = SocketUtil.readInt(clientSocket);
    
    //send the data to other players
    Big2Repository.clientSockets.keys.foreach
    {
      name =>
        //do not send back to the same player
        if(!name.equals(Big2Repository.players(playerIndex).name))
        {
          exec.execute(new Runnable{
            override def run() {
              val socket:Socket = Big2Repository.clientSockets(name);
              //send the receiveCode
              SocketUtil.sendInt(socket, PlayerReceive.READY.id);
              //send the player index to other players
              SocketUtil.sendInt(socket, playerIndex);
            }
          });
        }
    }
    
    //update the player ready
    val player = Big2Repository.players(playerIndex);
    player.isReady = !player.isReady;
    
    //update the view
    Application.updateHostRoomReadyView(playerIndex);
  }
  
  //leave room
  private def leaveRoom(clientSocket:Socket, exec:ExecutorService)
  {
    //get player index
    val playerIndex:Int = SocketUtil.readInt(clientSocket);
    
    //remove player socket
    Big2Repository.clientSockets -= Big2Repository.players(playerIndex).name;
    //remove player from repository
    Big2Repository.players.remove(playerIndex);
    
    val latch:CountDownLatch = new CountDownLatch(Big2Repository.clientSockets.size);
    //inform all the other players
    Big2Repository.clientSockets.keys.foreach
    {
      name =>
        exec.execute(new Runnable{
          override def run() {
            val socket:Socket = Big2Repository.clientSockets(name);
            //send the receiveCode
            SocketUtil.sendInt(socket, PlayerReceive.LEAVE_ROOM.id);
            //send the player index to all the other players
            SocketUtil.sendInt(socket, playerIndex);
            //reduce latch
            latch.countDown();
          }
        });
    }
    
    //update game room view
    Application.updateHostRoomView();
    
    //wait for all players to successfully the players list
    //important to prevent check then act race condition - remove wrong player
    latch.await();
  }
  
  //play card
  private def playCard(clientSocket:Socket, exec:ExecutorService)
  {
    //get updated table cards
    val tableCards:ArrayBuffer[Card] = SocketUtil.readObject[ArrayBuffer[Card]](clientSocket);
    
    val latch:CountDownLatch = new CountDownLatch(Big2Repository.clientSockets.size);
    
    //send the data to other players
    Big2Repository.clientSockets.keys.foreach
    {
      name =>
        //do not send back to the same player
        if(!name.equals(Big2Repository.players(Big2Repository.currentTurnIndex).name))
        {
          exec.execute(new Runnable{
            override def run() {
              val socket:Socket = Big2Repository.clientSockets(name);
              try
              {
                //send the receiveCode
                SocketUtil.sendInt(socket, PlayerReceive.PLAY_CARD.id);
                //send the table cards to other players
                SocketUtil.sendObject[ArrayBuffer[Card]](socket,tableCards);
              } catch {
                case e:IOException => e.printStackTrace();
                  //playerDisconnected(index);
              } finally {
                //reduce latch
                latch.countDown();
              }
            }
          });
        }
        else
        {
          latch.countDown(); //reduce also
        }
    }
    
    //update the repository if haven
    if(!Big2Repository.isMyTurn)
    {
      //update table
      Big2Repository.tableCards = tableCards;
      //reduce the current player hand
      Big2Repository.currentPlayer.cardsAmount -= Big2Repository.tableCards.length; 
      //go to next player
      Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
      //update gameplay view
      Application.updateGameplay();
    }
    
    //send acknowledgement to player
    SocketUtil.sendInt(clientSocket, -1);
    
    //wait for all players to successfully receive the table
    //important to prevent read-write modify race condition - old table cards overwrite new 1
    latch.await();
  }
  
  //pass
  private def pass(clientSocket:Socket, exec:ExecutorService)
  { 
    //get pass count
    val passCount:Int = SocketUtil.readInt(clientSocket);
    
    val latch:CountDownLatch = new CountDownLatch(Big2Repository.clientSockets.size);
    
    //send the data to other players
    Big2Repository.clientSockets.keys.foreach
    {
      name =>
        //do not send back to the same player
        if(!name.equals(Big2Repository.players(Big2Repository.currentTurnIndex).name))
        {
          exec.execute(new Runnable{
            override def run() {
              val socket:Socket = Big2Repository.clientSockets(name);
              try
              {
                //send the receiveCode
                SocketUtil.sendInt(socket, PlayerReceive.PASS.id);
                //send the pass count to other players
                SocketUtil.sendInt(socket, passCount);
              } catch {
                case e:IOException => e.printStackTrace();
                  //playerDisconnected(index);
              } finally {
                //reduce latch
                latch.countDown();
              }
            }
          });
        }
        else
        {
          latch.countDown(); //reduce also
        }
    }
    
    //update the repository if haven
    if(!Big2Repository.isMyTurn)
    {
      //update pass count
      Big2Repository.passCount = passCount;
      //go to next player
      Big2Repository.currentTurnIndex = Big2Repository.nextPlayerIndex(Big2Repository.currentTurnIndex,1);
      //update gameplay view
      Application.updateGameplay();
    }
    
    //send acknowledgement to player
    SocketUtil.sendInt(clientSocket, -1);
    
    //wait for all players to successfully receive the pass count
    //important to prevent read-write modify race condition - old pass count overwrite new 1
    latch.await();
  }
  
  //win game
  private def winGame(clientSocket:Socket, exec:ExecutorService)
  { 
    //update winnerIndex
    Big2Repository.winnerIndex = SocketUtil.readInt(clientSocket);
    
    //check penalty from previous player
    val preIndex = Big2Repository.prePlayerIndex(Big2Repository.winnerIndex, 1);
    //if host is not the previous player
    if(preIndex != Big2Repository.myIndex)
    {
      val preSocket = Big2Repository.clientSockets(Big2Repository.players(preIndex).name);
      //send the receiveCode
      SocketUtil.sendInt(preSocket, PlayerReceive.PENALTY.id);
      //update the repository
      Big2Repository.penaltyIndex = SocketUtil.readInt(preSocket);
    }
   
    //send the data to other players
    Big2Repository.clientSockets.keys.foreach
    {
      index =>
        //do not send back to the same player
        //if(index != playerIndex)
        {
          exec.execute(new Runnable{
            override def run() {
              val socket:Socket = Big2Repository.clientSockets(index);
              //send the receiveCode
              SocketUtil.sendInt(socket, PlayerReceive.WIN_GAME.id);
              //send the winnerIndex to other players
              SocketUtil.sendInt(socket, Big2Repository.winnerIndex);
              //send the penaltyIndex to other players
              SocketUtil.sendInt(socket, Big2Repository.penaltyIndex);
            }
          });
        }
    }
    
    //change to score board scene
    Application.changeScoreBoardScene();
  }
  
  private def playerDisconnected(index:Int)
  {
    //Big2Repository.clientSockets -=index;
    //Big2Repository.disconnectedPlayers += index;
  }
  
  private def changeName(name:String):String =
  {
    var count = 1;
    var newName = name;
    while(Big2Repository.clientSockets.contains(newName))
    {
      newName = s"$name-$count";
      count += 1;
    }
    return newName;
  }
  
  def stop()
  {
    if(serverSocket != null && !serverSocket.isClosed())
    {
      serverSocket.close();
    }
  }
}