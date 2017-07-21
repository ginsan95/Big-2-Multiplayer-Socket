package big2.controller

import big2.repository.Big2Repository;
import big2.model.{Card,Player};
import big2.util.SocketUtil;
import java.net.{ServerSocket,Socket,InetAddress,InetSocketAddress};
import java.io.IOException;
import HostRequest._

/**
 * @author AveryChoke
 */
class PlayerSendIndexThread(private val port:Int, private val hostAddress:String, 
    private val timeout:Int, private val playerIndex:Int, private val request:HostRequest) extends Runnable {
  
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
      SocketUtil.sendInt(socket, request.id);
      //send player index
      SocketUtil.sendInt(socket, playerIndex);
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