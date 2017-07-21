package big2.repository

import big2.model._;

import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};
import java.net.{ServerSocket,Socket};

/**
 * @author AveryChoke
 */
object Big2Repository {
  
  val PORT_NUM:Int = 6767;
  val STARTING_SCORE = 500;
  private var _clientSockets:Map[String,Socket] = new HashMap[String,Socket]();
  private var _disconnectedPlayers:Buffer[Int] = new ArrayBuffer[Int]();
  
  //buffer of players
  private var _players:Buffer[Player] = new ArrayBuffer[Player](); 
  //the player index in the buffer
  private var _myIndex:Int = 0;
  //host address
  private var _hostAddress:String = "";
  //current turn player index
  private var _currentTurnIndex:Int = 0;
  //the cards on the game table
  private var _tableCards:Buffer[Card] = new ArrayBuffer[Card]();
  //amount of passes
  private var _passCount = Integer.MAX_VALUE;
  //player who get penalty
  private var _penaltyIndex:Int = -1;
  //winner player index
  private var _winnerIndex:Int = -1;
  
  def initialize(plays:Buffer[Player], index:Int, address:String)
  {
    players = plays;
    myIndex = index;
    hostAddress = address;
  }
  
  //reset all the round data of the repository
  def resetRound()
  {
    for(player <- players)
    {
      player.cardsAmount = 13;
    }
    tableCards.clear();
    passCount = Integer.MAX_VALUE;
    penaltyIndex = -1;
  }
  
  //for host to add player into the room
  def addPlayer(index:Int, player:Player)
  {
    players.insert(index,player);
  }
  
  //return the player himself
  def myPlayer:Player = players(myIndex);
  
  //return the current player
  def currentPlayer:Player = players(currentTurnIndex);
  
  //return the next player
  def nextPlayer:Player = players(nextPlayerIndex(currentTurnIndex,1));
  
  //return the previous player
  def prePlayer:Player = players(prePlayerIndex(currentTurnIndex,1));
  
  //get the next player index
  def nextPlayerIndex(index:Int, count:Int):Int =
  {
    var nextIndex = index+count;
    if(nextIndex>=players.length)
    {
      nextIndex -= (players.length);
    }
    return nextIndex;
  }
  
  //get the previous player index
  def prePlayerIndex(index:Int, count:Int):Int =
  {
    var preIndex = index-count;
    if(preIndex<0)
    {
      preIndex += players.length;
    }
    return preIndex;
  }
  
  //check if is my turn
  def isMyTurn:Boolean =
  {
    return myIndex==currentTurnIndex;
  }
  
  //check if all the other player passed
  def isAllPass:Boolean =
  {
    return passCount >= (players.length-1);
  }
  
  def isAllReady:Boolean =
  {
    for(i <- 0 until players.length)
    {
      if(i!=myIndex && !players(i).isReady)
      {
        return false;
      }
    }
    return true;
  }
  
  
  //get set
  def players:Buffer[Player] = _players;
  def players_= (value:Buffer[Player]){ _players=value }
  
  def myIndex:Int = _myIndex;
  def myIndex_= (value:Int){ _myIndex=value }
  
  def hostAddress:String = _hostAddress;
  def hostAddress_= (value:String){ _hostAddress=value }
  
  def currentTurnIndex:Int = _currentTurnIndex;
  def currentTurnIndex_= (value:Int){ _currentTurnIndex=value }
  
  def tableCards:Buffer[Card] = _tableCards;
  def tableCards_= (value:Buffer[Card]){ _tableCards=value }
  
  def passCount:Int = _passCount;
  def passCount_= (value:Int){ _passCount=value }
  
  def penaltyIndex:Int = _penaltyIndex;
  def penaltyIndex_= (value:Int){ _penaltyIndex=value }
  
  def winnerIndex:Int = _winnerIndex;
  def winnerIndex_= (value:Int){ _winnerIndex=value }
  
  def clientSockets:Map[String,Socket] = _clientSockets;
  def clientSockets_= (value:Map[String,Socket]){ _clientSockets=value }
  
  def disconnectedPlayers:Buffer[Int] = _disconnectedPlayers;
  def disconnectedPlayers_= (value:Buffer[Int]){ _disconnectedPlayers=value }
}