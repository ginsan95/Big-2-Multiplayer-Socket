package big2.controller

import big2.model.{Card,Deck};
import big2.util.SocketUtil;
import java.net.Socket;
import java.io.IOException;
import scala.collection.mutable.{Buffer,ArrayBuffer};

/**
 * @author AveryChoke
 */
class DistributeCardsThread(private val socket:Socket, private val cards:ArrayBuffer[Card], 
    private val startIndex:Int) extends Runnable {
  
  override def run()
  {
    //send receive code
    SocketUtil.sendInt(socket, PlayerReceive.NEW_ROUND.id);
    
    //send cards
    SocketUtil.sendObject[ArrayBuffer[Card]](socket, cards);
    
    //send start index
    SocketUtil.sendInt(socket, startIndex);
  }
  
}