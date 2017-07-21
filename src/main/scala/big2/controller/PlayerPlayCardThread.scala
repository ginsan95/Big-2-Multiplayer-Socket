package big2.controller

import big2.repository.Big2Repository;
import big2.model.{Card,Player};
import big2.util.SocketUtil;
import java.net.{ServerSocket,Socket,InetAddress,InetSocketAddress};
import java.io.IOException;
import scala.collection.mutable.{Buffer,ArrayBuffer,Map,HashMap};

/**
 * @author AveryChoke
 */
class PlayerPlayCardThread(private val port:Int, private val hostAddress:String, 
    private val timeout:Int, private val cards:ArrayBuffer[Card]) extends Runnable {
  
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
      
      //send the request code
      SocketUtil.sendInt(socket, HostRequest.PLAY_CARD.id);
      //send the table cards
      SocketUtil.sendObject[ArrayBuffer[Card]](socket, cards);
      
      //receive acknowledgement from host
      SocketUtil.readInt(socket);
    } catch {
      case e:IOException => e.printStackTrace();
    } finally {
      if(socket != null && !socket.isClosed())
      {
        socket.close();
      }
    }
  }
  
}