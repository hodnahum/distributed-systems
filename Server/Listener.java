package Server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.concurrent.ConcurrentHashMap;

public class Listener extends Thread{

    private ServerSocket serverSocket;
    private File mainFolder;
    private File tempFolder;
    private File logFile;
    BufferedWriter bufferedWriter= null;
    Calendar calendar;
    private ConcurrentHashMap <String,String> FileToLock;
    private ConcurrentHashMap<String,Integer>Version;
    private ConcurrentHashMap<String,String> Version_1;
    public ConcurrentHashMap<String,Integer> tempVersion = new ConcurrentHashMap<>();


    public ArrayList<String> ipConnected ;
    public Socket socket;


    //constructor : ServerSocket
    public Listener (ServerSocket socket, File mainFolder, File logFile, ConcurrentHashMap<String,String> FileToLock, ConcurrentHashMap<String,Integer>Version, ArrayList<String> ConnectedServer,File tempFolder,ConcurrentHashMap<String,String> Version_1,ConcurrentHashMap<String,Integer> tempVersion){

        try {
            this.Version=Version;
            this.FileToLock=FileToLock;
            this.mainFolder=mainFolder;
            this.logFile=logFile;
            serverSocket = socket;
            bufferedWriter=new BufferedWriter(new FileWriter(logFile.toString(),true));
            calendar= Calendar.getInstance();
            this.ipConnected = ConnectedServer;
            this.tempFolder = tempFolder;
            this.Version_1 = Version_1;
            this.tempVersion = tempVersion;
            bufferedWriter.append(calendar.getTime()+"the msg... ");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        //syncNow();

        // start to listen on the server socket
        System.out.println("Listening on " + serverSocket.getInetAddress().toString() + ":" + serverSocket.getLocalPort());
        ServerMain.WriteTOLog("Start Listening on ip : "+serverSocket.getInetAddress().toString()+" and port : "+serverSocket.getLocalPort());
        while (!interrupted()) {
            try {

                // get a new connection
                Socket clientSocket = serverSocket.accept();
                // start a worker
                ServerHandle clientThread = new ServerHandle(clientSocket,mainFolder, FileToLock,Version,tempFolder,Version_1,tempVersion);
                clientThread.start();

            } catch (Exception ex)
            {
                // something is wrong, let's quit
            }
        }

        // we're done!
        System.out.println("Stopped listening.");
        try {
            serverSocket.close();
        } catch (Exception ex)
        {
            //noting to do
        }
    }//END run()

//    public void syncNow()
//    {
//        System.out.println(ServerMain.nameServer+" sync files");
//        ServerMain.WriteTOLog("Start sync with online servers");
//        for (String s : ipConnected)
//        {
//            try {
//                socket = new Socket(s.split(" ")[1], Integer.parseInt(s.split(" ")[2]));
//
//
//
//            }catch (IOException e) {
//                System.out.println("The server not connected: " + s);
//                //ServerMain.WriteTOLog("This server not connected -> "+s+" Ignore.");
//                if(!(ipConnected.indexOf(s) == ipConnected.size()-1))
//                {
//                    continue;
//                }
//
//                break;
//            }
//            OutputStream writerStream = null;
//            try {
//                writerStream = socket.getOutputStream();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            PrintWriter pr = new PrintWriter(writerStream,true);
//            pr.println("GETLIST");//ask for get list of files from the server that connected now
//            String[] fileList = ServerHandle.ansFromServer(socket).split(" ");
//            for (String fileName : fileList)
//            {
//                if(fileName.equals("OK"))
//                {/*get the files without the world 'OK'*/
//                    continue;
//                }
//                System.out.println(fileName);
//            }
//        }//END for(try connected to server 's')
//        System.out.println("sync finish");
//        //ServerMain.WriteTOLog("sync finish");
//    }//END syncNow()

}//END class Listener
