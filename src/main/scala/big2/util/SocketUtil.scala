package big2.util

import java.net.{ServerSocket,Socket};
import java.io.IOException;
import java.util.concurrent.{Executors,ExecutorService,TimeUnit};
import java.io.{ObjectInputStream,ObjectOutputStream,DataInputStream,DataOutputStream};

/**
 * @author AveryChoke
 */
object SocketUtil {
  
  //method to allow sending of any object that extends Serializable over a socket
  def sendObject[T<:Serializable](socket:Socket, myObject:T)
  {
    val oos:ObjectOutputStream = new ObjectOutputStream(socket.getOutputStream);
    oos.writeObject(myObject);
    oos.flush();
  }
  
  //method to allow reading of any object that extends Serializable from a socket
  def readObject[T<:Serializable](socket:Socket):T =
  {
    val ois:ObjectInputStream = new ObjectInputStream(socket.getInputStream);
    val myObject:T = ois.readObject() match {
      case o:T => o;
    };
    return myObject;
  }
  
  def sendString(socket:Socket, myString:String)
  {
    val dos = new DataOutputStream(socket.getOutputStream());
    dos.writeUTF(myString);
    dos.flush();
  }
  
  def readString(socket:Socket):String =
  {
    val dis = new DataInputStream(socket.getInputStream());
    val myString = new String(dis.readUTF());
    return myString;
  }
  
  def sendInt(socket:Socket, num:Int)
  {
    val dos = new DataOutputStream(socket.getOutputStream());
    dos.writeInt(num);
    dos.flush();
  }
  
  def readInt(socket:Socket):Int =
  {
    val dis = new DataInputStream(socket.getInputStream());
    val num = dis.readInt();
    return num;
  }
  
  
  
  def serverSocket(socketNum:Int, clientCount:Int, foo:(Socket)=>Unit)
  {
    var serverSocket:ServerSocket = null;
    var clientSocket: Socket = null;
    var exec:ExecutorService = null;
    
    try
    {
      serverSocket = new ServerSocket(socketNum);
      exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()+1);
      for(i<-1 to clientCount)
      {
        try
        {
          clientSocket = serverSocket.accept();
          exec.execute(new Runnable{
            def run() {foo(clientSocket)}
          });
        } catch {
          case e:IOException => e.printStackTrace();
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
      if(serverSocket != null && !serverSocket.isClosed())
      {
        serverSocket.close();
      }
    }
  }
  
}