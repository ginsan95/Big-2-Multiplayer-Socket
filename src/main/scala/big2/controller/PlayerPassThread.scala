package big2.controller

import big2.repository.Big2Repository;
import big2.model.{Card,Player};
import big2.util.SocketUtil;
import java.net.{ServerSocket,Socket,InetAddress,InetSocketAddress};
import java.io.IOException;

/**
 * @author AveryChoke
 */
class PlayerPassThread(private val port:Int, private val hostAddress:String, 
    private val timeout:Int, private val passCount:Int) extends Runnable {
  
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
      SocketUtil.sendInt(socket, HostRequest.PASS.id);
      //send the pass count
      SocketUtil.sendInt(socket, passCount);
      
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