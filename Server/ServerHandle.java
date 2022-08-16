package Server;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ServerHandle extends Thread
{

    PrintWriter printWriter = null;
    Socket socket;
    private File mainFolder;
    private File tempFolder;
    private ConcurrentHashMap<String, String> FileToLock;
    private ConcurrentHashMap<String, Integer> Version;
    private ConcurrentHashMap<String,String> Version_1;
    private ConcurrentHashMap<String,Integer> tempVersion;

    public ArrayList<String> serversIPandPort = new ArrayList<>();
    public String clientIP = null;
    public  String lockBy;
    public boolean inOffer = false;

    //CONSTRUCTOR
    public ServerHandle(Socket socket, File mainFolder, ConcurrentHashMap<String, String> FileToLock, ConcurrentHashMap<String, Integer> Version,File tempFolder,ConcurrentHashMap<String,String> Version_1,ConcurrentHashMap<String,Integer> tempVersion)
    {
        this.socket = socket;
        this.mainFolder = mainFolder;
        this.FileToLock = FileToLock;
        this.Version = Version;
        this.tempFolder = tempFolder;
        this.Version_1 = Version_1;
        this.tempVersion = tempVersion;
    }

    @Override
    public void run()
    {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            printWriter = new PrintWriter(socket.getOutputStream(), true);
            String clientCommand;
            try {
                 clientCommand = in.readLine();

            }catch (NullPointerException e)
            {
                System.out.println("the command is null");
                return;
            }


            String[] _serverRead = clientCommand.split(" ");

            ServerMain.WriteTOLog("Received from ip : " + socket.getInetAddress().toString() + " port: " + socket.getLocalPort() + ".\n" + "Client command: " + _serverRead[0]);

            for (int j = 0; j < ServerMain.ipAndPort.size(); j++) {
                String IpConnected = ServerMain.servsock.getInetAddress().toString();


                if (IpConnected.equals("/" + ServerMain.ipAndPort.get(j).split(" ")[1]) || socket.getInetAddress().toString().equals("/" + ServerMain.ipAndPort.get(j).split(" ")[1])) {
                    String tmp = ServerMain.ipAndPort.get(j).split(" ")[1];

                } else {
                    serversIPandPort.add(ServerMain.ipAndPort.get(j));

                }
            }


            switch (_serverRead[0]) {
                case "UPLOAD":

                    getFile(_serverRead[1], _serverRead[2]);
                    break;
                case "GETLIST":

                    getList();
                    break;

                case "LOCK":
                    Lock(_serverRead[1]);

                    break;
                case "LOCK_1":
                    LOCK_1(_serverRead[1],_serverRead[2]) ;
                    break;
                case "UNLOCK_1":
                    UNLOCK_1(_serverRead[1],_serverRead[2]);
                    break;
                case "UNLOCK":
                    unLock(_serverRead[1]);
                    break;
                case "GETVERSION":
                    UpVersion(_serverRead[1]);
                    break;

                case "DOWNLOAD":
                    sendFile(_serverRead[1]);
                    break;
                case "OFFER":
                    inOffer = true;
                    /*if 'lockBy' is null -> insert: " "; if lockBy not null -> insert the contact from server */
                    if ((lockBy = in.readLine()) == null)
                    {
                        lockBy = " ";
                    }
                     checkOffer(_serverRead[1],_serverRead[2],_serverRead[3]);
                    break;

                case "STOP":
                    //client want to stop
                    // something is wrong - just quit
                    stopCon();
                    break;
                default:
                    ServerMain.WriteTOLog("Incorrect command received.");
                    break;
            }


        } catch (IOException e) {
            ServerMain.WriteTOLog("Error in communication.  Closing.");

            //e.printStackTrace();
        }


    }//END run

    public void getFile(String fileName, String file)
    {
        ArrayList<String> ans = new ArrayList<>();
        ArrayList<String> addressesIP = new ArrayList<>();
        String successAns;
        String name = fileName;
        String digestANDTime = null;

        File f = new File(mainFolder.getPath() + "\\" + name);
       // File tempFile = new File(tempFolder.getPath() + "\\" + name);


          //----- file does not exists yet at server root folder OR the file exists and is currently locked bt the requesting client --> ----//
          //------- --> Server will store the file in temporary folder --> send OFFER(with : digest , timeStamp , file name) to all other servers
        if ((FileToLock.containsKey(name) && socket.getInetAddress().toString().equals(FileToLock.get(name)) && f.exists()) || !f.exists())//lock and exist
        {
           //----------upload file to the temp folder in first server----------//
            //the decode file to byte array.
            byte[] bytes = Base64.getDecoder().decode(file);
            /*store in temp folder until the first server got 'OK' from all servers*/
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(tempFolder + "/" + name);
            } catch (FileNotFoundException e) {
                System.out.println("wrong output stream");
            }
            try {
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                System.out.println();
            }
            ServerMain.WriteTOLog("OK : file:" + name + " upload from client: "+socket.getInetAddress().toString()+ ". to server temp folder( " + tempFolder.getAbsolutePath() + ")" );
            //----set version for this file----//
            setTempVersion(name);
            digestANDTime = Version_1.get(name);
            //digestANDTime = digestANDTime.split(" ")[1];//time
            ans.add("OK");//upload to the temp folder success
            System.out.println("temp version : ");
            System.out.println(Version_1);
            System.out.println(tempVersion);
//-------------------------------first server try connected to others servers and try to upload the file to them-------------------------------------------------------------------------//
            for (String s : serversIPandPort)
            {
                OutputStream writerStream1;
                JOptionPane.showMessageDialog(null, "The Server try make connection to : " + s.split(" ")[1]);
                ServerMain.WriteTOLog("The server: "+ServerMain.servsock.getInetAddress().toString()+" try make connection to: "+s.split(" ")[1]);

                Socket socket = null;
                try {
                    socket = new Socket(s.split(" ")[1], Integer.parseInt(s.split(" ")[2]));
                    ServerMain.WriteTOLog("The Server: " + ServerMain.servsock.getInetAddress().toString() + " Connected to server : " + socket.getInetAddress().toString());
                    writerStream1 = socket.getOutputStream();
                    PrintWriter printWriter1 = new PrintWriter(writerStream1, true);
                    digestANDTime = Version_1.get(name);
                    printWriter1.println("OFFER " + Version_1.get(name) + " " + name);//write to others server "OFFER" + digest + timestamp+nameofFile +
                    if (FileToLock.containsKey(name))
                    {
                        printWriter1.println(FileToLock.get(name));//write to others servers who lock the file
                    } else {
                        printWriter1.println(" ");
                    }
                }catch (IOException e) {
                    ServerMain.WriteTOLog("The server: "+ServerMain.nameServer+ " on: "+ServerMain.servsock.getInetAddress().toString()+" not Connected to : "+s);
                    continue;
                }
                    //get the ans from others servers after sending a 'OFFER'
                    String downAns = ansFromServer(socket);
                    String[] _command = downAns.split(" ");

                    //-----------if the ans from the servers is 'DOWNLOAD' then server_1 send this file to them-----------//
                    if (_command[0].equals("DOWNLOAD"))
                    {
                        File temp = new File(tempFolder.getPath() + "\\" + _command[1]);

                        /**
                         * NEED TO ADD MSG "OK" TO THE SERVER AND THEN SEND THE FILE
                         */
                        byte[] mybytearray = new byte[(int) temp.length()];
                        try {
                            FileInputStream fis = new FileInputStream(tempFolder + "/" + name);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            DataInputStream dis = new DataInputStream(bis);
                            dis.readFully(mybytearray, 0, mybytearray.length);
                            writerStream1 = socket.getOutputStream();  //handle file send over socket
                            DataOutputStream dos = new DataOutputStream(writerStream1); //Sending file name and file size to the server
                            dos.writeLong(mybytearray.length);
                            dos.write(mybytearray, 0, mybytearray.length);
                            dos.flush();
                            ServerMain.WriteTOLog("OK: " + fileName + " downloaded to server: "+s);
                            successAns = ansFromServer(socket);
                            ans.add(successAns);
                            addressesIP.add(socket.getInetAddress().toString());
                        } catch (IOException e) {
                            System.out.println("Something wrong!");

                        }

                    }//END if(server want to download the file)

                    else//ans from server = ERROR
                    {
                       ans.add(downAns);
                      // addressesIP.add(socket.getInetAddress().toString());
                    }//END else


            }//END for(the first server try connected to others servers)

            //-----------------------Checking all answers from servers-----------------------//
            if (ans.contains("ERROR"))
            {
              System.out.println("upload failed.on server: "+ServerMain.servsock.getInetAddress().toString());
              printWriter.println("ERROR");
              Version_1.remove(name);
              Version.remove(name);
              tempVersion.remove(name);
            }
            //-------------------if more the one server connected : download success --> upload the file to the root folder-------------------//
            if(!ans.contains("ERROR"))
            {
                boolean successUpload = false;
                try {
                    byte[] bytes_1 = Base64.getDecoder().decode(file);
                    FileOutputStream fileOutputStream_1 = null;
                    fileOutputStream_1 = new FileOutputStream(mainFolder + "/" + name);
                    fileOutputStream_1.write(bytes_1);

                    setVersion(name);
                    if(Version_1.containsKey(name) && !digestANDTime.equals(null))
                    {
                        Version_1.replace(name,digestANDTime.split(" ")[0]+" "+digestANDTime.split(" ")[1]);
                    }
                    System.out.println("version in root :");
                    System.out.println(Version_1);
                    System.out.println(Version);
                    //tempVersion.remove(name);
                    successUpload = true;//upload to the root folder success
                    addressesIP.add(ServerMain.servsock.getInetAddress().toString());
                } catch (IOException e) {
                    System.out.println("something wrong");
                }
                if (successUpload)
                {
                    ServerMain.WriteTOLog("OK : file:" + name + " upload  from client to the root folder( "+mainFolder.getAbsolutePath()+" )"+" AND to all servers that online: ");
                    ServerMain.WriteTOLog("online servers: "+addressesIP);

                    ServerMain.WriteTOLog("The version of the file : "+name+" "+Version_1.get(name));
                    printWriter.println("OK");
                    printWriter.close();
                }
                System.out.println("upload successful");

            }//END if (all servers return OK)


        }//END(if file locked and the lock was made by the same IP trying to upload this file) OR (if file does not exist the in root folder)


        //-------------file exists  and the client does not currently hold a lock on the file --> server immediately responds with ERROR!-------------//
        else
        {
            ServerMain.WriteTOLog("ERROR: The file : " + fileName + " NOT LOCKED and EXISTS! upload stops. From server: " + ServerMain.servsock.getInetAddress().toString());
            printWriter.println("ERROR");
            printWriter.close();

        }//END else(if the file exist in root folder but not lock(if it is locked then not by the same IP that trying to upload this file))

    }//END getFile()

    public void checkOffer(String digest,String time , String FileName)
    {
        String name = FileName;
        int bytesRead;
        String ansFromServer;
        File filePath = new File(mainFolder.getPath() + "\\" + name);

        if (FileToLock.containsKey(FileName) && FileToLock.get(FileName).equals(lockBy) && filePath.exists()|| !filePath.exists())//if the file is exists and lock---> you can mack uploud !
        {

            printWriter.println("DOWNLOAD "+name);
            String outputPath = mainFolder.getPath()+ "\\" + name;
            try {

                InputStream in = socket.getInputStream();

                DataInputStream clientData = new DataInputStream(in);

                OutputStream output = new FileOutputStream(outputPath);
                long size = clientData.readLong();
                byte[] buffer = new byte[1024];
                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }
                printWriter.println("OK");
                in.close();
                clientData.close();
                output.close();
                System.out.println("OK: The file: "+name+" received successfully from server: "+socket.getInetAddress().toString());
                ServerMain.WriteTOLog("OK: The file: "+name+" received successfully from server: "+socket.getInetAddress().toString());
                //JOptionPane.showMessageDialog(null,"OK: The file: "+name+" received successfully from server: "+socket.getInetAddress().toString());
                setVersion(name);
                if (Version_1.containsKey(name))
                {
                    Version_1.replace(name,digest+" "+time);
                }
                ServerMain.WriteTOLog("The version of the file : "+name+" "+Version_1.get(name));
            } catch (IOException ex) {
                System.out.println("Exception: "+ex);
            }
        }
        else
        {
            printWriter.println("ERROR");
        }
    }

    public void getList() {
        String Files = "FILELIST ";
        File folder = new File(mainFolder.getPath());
        File[] listOfFiles = folder.listFiles();
//        Set<String> files = Version.keySet();
        for (File listOfFile : listOfFiles) {

            if (listOfFile.isFile()) {
                Files += listOfFile.getName() + " ";
                ServerMain.WriteTOLog("GetList : list of files : " + listOfFile.getName() + "\n");
            }
        }

//        for (String file : files)
//        {
//            F += file.substring(listOfFiles.length);
//        }
        printWriter.println(Files);
        printWriter.close();
    }//End getList


    public void sendFile(String path) {

        String fileName = path;

        String FileLocation = mainFolder.getPath() + "\\" + path;
        File myFile = new File(FileLocation);  //handle file reading
        String ex = existFile(fileName);
        if (ex.equals("OK")) {
            printWriter.println("OK");
            try {
                byte[] mybytearray = new byte[(int) myFile.length()];

                FileInputStream fis = new FileInputStream(FileLocation);
                BufferedInputStream bis = new BufferedInputStream(fis);

                DataInputStream dis = new DataInputStream(bis);
                dis.readFully(mybytearray, 0, mybytearray.length);


                OutputStream os = socket.getOutputStream();  //handle file send over socket

                DataOutputStream dos = new DataOutputStream(os); //Sending file name and file size to the server
                //dos.writeUTF(myFile.getName());
                dos.writeLong(mybytearray.length);
                dos.write(mybytearray, 0, mybytearray.length);
                dos.flush();


                ServerMain.WriteTOLog("OK: " + fileName + " downloaded to client/toServer on: "+socket.getInetAddress().toString()+" "+socket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            ServerMain.WriteTOLog("ERROR: The file: " + fileName + " not exist in server folder.\ndownload failed.");
            printWriter.println("ERROR");
            printWriter.close();
        }


    }//END sendFile


    //check if the file exist in the server folded .
    //before sending a file to client : need to check that file exists
    //after geting file from client : need to check that file exists
    public String existFile(String fileName) {
        String exist;
        File myFile = new File(mainFolder.getPath() + "\\" + fileName);
        if (myFile.exists()) {
            exist = "OK";

        } else {
            exist = "ERROR";
        }
        //PrintStream writer = null;
        //   try {
        // writer = new PrintStream(socket.getOutputStream());
        // } catch (IOException e) {
        //   e.printStackTrace();
        //}

        //writer.println(exist);
        return exist;

    }//End existFile


    public void Lock(String nameOfFile) {

        String _nameLock = nameOfFile;
         clientIP = null;//hold the ip of the client that make a LOCK
        //String Files="OK ";
        ArrayList<String> ans = new ArrayList<>();
        ArrayList<String> addresses = new ArrayList<>();
        File folder = new File(mainFolder.getPath());
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                if (listOfFiles[i].getName().equals(_nameLock))//if this file exists in the folder server
                {
                    if (!FileToLock.containsKey(_nameLock))//if this name file not exist in FileLock
                    {
                        FileToLock.put(_nameLock, socket.getInetAddress().toString());
                        ServerMain.WriteTOLog("The file: "+_nameLock+" locke.by: "+socket.getInetAddress().toString()+" on the server : "+ServerMain.servsock.getInetAddress().toString());

                        //if (serversIPandPort.size() > 1)// if this is the first server
                      //  {
                            clientIP = socket.getInetAddress().toString();
                            ans.add("OK");//ans from SERVER 1
                            addresses.add("OK:" + ServerMain.servsock.getInetAddress().toString());
                            for (String s : serversIPandPort)
                            {
                                try {

                                    JOptionPane.showMessageDialog(null, "The Server try make connection to : " + s.split(" ")[1]);
                                    //SERVER 1 : CONNECTED  to others servers :
                                    Socket socket = new Socket(s.split(" ")[1], Integer.parseInt(s.split(" ")[2]));
                                    ServerMain.WriteTOLog("The Server: "+ServerMain.servsock.getInetAddress().toString() +" connected to the server on ip: "+socket.getInetAddress().toString());
                                    OutputStream writerStream1 = socket.getOutputStream();
                                    PrintWriter printWriter1 = new PrintWriter(writerStream1, true);
                                    printWriter1.println("LOCK_1 " + _nameLock +" "+clientIP);//write others servers
                                    String a = ansFromServer(socket);//get the ans of others server(ERROR/OK)
                                    ans.add(a);//SERVER 1 : save the ans from others serves
                                    addresses.add(a + ":" + socket.getInetAddress().toString());//SERVER 1 : save the ans from others serves and there IP

                                } catch (IOException e) {
                                    // connected.add(false);
                                    ServerMain.WriteTOLog("The server on: "+ServerMain.servsock.getInetAddress().toString()+"not connected to the server : " +s);
                                    System.out.println("The server: "+s+" not connected.");
                                }
                            }//END FOR(finish for try connected and lock the file in others servers )

                    }//END if(file not exists in FileLock --> not lock)

                    else//when this name file already exist in FileLock
                    {

                        printWriter.println("ERROR");
                        ServerMain.WriteTOLog("ERROR: The file " + _nameLock + " is currently locked. on: " + ServerMain.servsock.getInetAddress().toString());
                        break;

                    }//END else(file exists in FileLock --> Lock)


                    //--------at least one return 'ERROR'(lock failed) -> unlock the file where servers return 'OK'(lock successful)-------//
                    if (ans.contains("ERROR"))
                    {
                        //one of the server return : ERROR =>
                        //=>now we need to make unlock for all servers.
                        for (String s : addresses)
                        {
                            if (s.split(":")[0].equals("OK"))
                            {
                                if (ServerMain.servsock.getInetAddress().toString().equals(s.split(":")[1]))
                                {
                                    FileToLock.remove(_nameLock);//unlock the file in first server
                                }//END if(unlock in SERVER_1)
                                else {//unlock the file in all servers
                                    try {
                                        Socket socket = new Socket(s.split(":")[1], ServerMain.port);
                                        OutputStream writerStream1 = socket.getOutputStream();
                                        PrintWriter printWriter1 = new PrintWriter(writerStream1, true);
                                        printWriter1.println("UNLOCK_1 " + _nameLock+" "+clientIP);//write to others servers
                                        String ansForUnlock = ansFromServer(socket);
                                        if (ansForUnlock.equals("OK")) {
                                            System.out.println("The unlock success in server : "+socket.getInetAddress().toString());
                                        } else {
                                            System.out.println("The unlock not success in server : "+socket.getInetAddress().toString());
                                        }
                                    } catch (IOException e) {
                                        System.out.println("server not connected: "+ s.split(":")[1]);
                                        continue;
                                    }
                                }//END else (unlock in all servers that return ok)
                            }//END if
                        }//END for
                        printWriter.println("ERROR");
                        ServerMain.WriteTOLog("At least one server returned an error-> lock failed in all servers -> The client will receive an error message");

                    }//END if(at least one ERROR)

                    //-----------All servers return 'OK'-------------//
                    if (!ans.contains("ERROR"))
                    {
                        printWriter.println("OK");
                        ServerMain.WriteTOLog("The file : "+ _nameLock + " lock in all servers that online : ");
                        for (String adr : addresses)
                            ServerMain.WriteTOLog("The online servers :"+adr.split(":")[1]);
                    }//END if(all 'OK')
                    break;
                }//END IF(file exists in the server folder)
                else
                {
                    if (i == listOfFiles.length - 1)
                    {
                        //when this name file dont exist in server folder
                        printWriter.println("ERROR");
                        ServerMain.WriteTOLog("ERROR: The file " + _nameLock + " does not exist in server folder. on : "+ ServerMain.servsock.getInetAddress().toString());
                        ServerMain.WriteTOLog("The file does not exist on the first server that connected-> The client receives an error message");
                        break;
                    }
                }//END ELSE(file not exist is server folder)

            }//END if

        }//END FOR

        if(listOfFiles.length == 0)
        {
            printWriter.println("ERROR");
            ServerMain.WriteTOLog("The folder is empty");
        }

    }//END LOCK

    public void LOCK_1(String _nameLock , String ipClient)
    {
        File folder = new File(mainFolder.getPath());
        File[] listOfFiles = folder.listFiles();
        for (int i = 0 ; i< listOfFiles.length ; i++)
        {
            if (listOfFiles[i].isFile())
            {
                if (listOfFiles[i].getName().equals(_nameLock))//if this file exists in the folder server
                {
                    if (!FileToLock.containsKey(_nameLock))//if this name file not exist in FileLock
                    {
                        FileToLock.put(_nameLock,ipClient);//lock the file from server in this server by the client
                        //ServerMain.WriteTOLog("The file: "+_nameLock+" is lock. by client : "+ipClient+".\non server : "+ServerMain.servsock.getInetAddress().toString());
                        OutputStream writerStream2 = null;
                        try {
                            writerStream2 = socket.getOutputStream();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //addresses.add(socket.getInetAddress().toString());
                        PrintWriter printWriter2 = new PrintWriter(writerStream2, true);
                        printWriter2.println("OK");//SERVER 2 send : OK
                        printWriter2.close();
                        ServerMain.WriteTOLog("The file : "+_nameLock+" locke.by client: "+ipClient+". on the server : "+ServerMain.servsock.getInetAddress().toString());
                    }//END if(file doesnt exists in FileLock --> file not locked)
                    else//name file exist in FileLock
                    {
                        printWriter.println("ERROR");
                        printWriter.close();
                        ServerMain.WriteTOLog("ERROR: The file "+_nameLock+"already Locked! on server :" +ServerMain.servsock.getInetAddress().toString());
                    }//END else (file exists in FileLock --> file locked)

                    break;
                }//END if(file exists in server folder)
                else//when file not exists in server folder
                {
                    if (i == listOfFiles.length - 1) {
                        //when this name file dont exist in server folder
                        printWriter.println("ERROR");
                        ServerMain.WriteTOLog("ERROR: The file " + _nameLock + " doesnt exist in server folder.(on the server : "+ServerMain.servsock.getInetAddress().toString()+")");
                    }
                }//END else (file not exists in server folder)
            }//end if (file not a file)
        }//END for
        if (listOfFiles.length == 0)
        {
            printWriter.println("ERROR");
            ServerMain.WriteTOLog("The folder is empty. on the server : "+ServerMain.servsock.getInetAddress().toString());
        }
    }

    /**
     * read the ans from server (by socket)
     * @param socket
     * @return string that contain the ans from server
     */
    public static String ansFromServer(Socket socket)
    {
        BufferedReader reader = null;


        String ansFromServer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ansFromServer = reader.readLine();
            //reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ansFromServer;
    }//END ansFromServer


    public void unLock(String fileName)
    {

        String _FileToUnlock = fileName;
        String ipClient = null;
        ArrayList<String> ans = new ArrayList<>();
        boolean b = false;

        if (FileToLock.containsKey(_FileToUnlock))//when this name file exist in FileLock
        {
            if (socket.getInetAddress().toString().equals(FileToLock.get(_FileToUnlock)))//when the same client that lock this file try to unlock this file
            {
                    FileToLock.remove(_FileToUnlock);
                    ipClient = socket.getInetAddress().toString();
                    ans.add("OK");
                    for (String s : serversIPandPort)
                    {
                         try
                         {
                         JOptionPane.showMessageDialog(null, "The Server try make connection to : " + s.split(" ")[1]);
                         //server try CONNECTED to others server:
                         Socket socket = new Socket(s.split(" ")[1], Integer.parseInt(s.split(" ")[2]));
                         ServerMain.WriteTOLog("The Server: "+ServerMain.servsock.getInetAddress().toString() +" connected to the server on ip: "+socket.getInetAddress().toString());


                         OutputStream writerStream1 = socket.getOutputStream();
                         PrintWriter printWriter1 = new PrintWriter(writerStream1, true);
                         printWriter1.println("UNLOCK_1 " + _FileToUnlock+" "+ipClient);//write to server
                         String a = ansFromServer(socket);
                         ans.add(a);

                        }catch (IOException e) {
                             ServerMain.WriteTOLog("The server on: "+ServerMain.servsock.getInetAddress().toString()+"not connected to the server : " +s);
                             System.out.println("The server: "+s+" not connected.");
                        }

                    }//END for : SERVER one finish try connected and unlock file in others servers



            }//END if(same client that locke the file try to make unlock on this file)

            else//not that same client try to unlock
            {
                //printWriter.println("ERROR"+" "+_FileToUnlock +" "+"is not currently locked by this client:"+socket.getInetAddress().toString());
                ans.add("ERROR");
                printWriter.println("ERROR");
                ServerMain.WriteTOLog("ERROR: The file : " + _FileToUnlock + " do not currently locked by this client. on the first server that online : "+ServerMain.servsock.getInetAddress().toString());
                ServerMain.WriteTOLog("The first server that was connected could not unlock the file -> Not transferred to the other servers-> A client will receive an error");

            }//END else(the file exists in FileLock BUT do not currently locke by this client)

        }//END if(file exists in FileLock --> file  already locked)
        else
        {
            //printWriter.println("ERROR"+_FileToUnlock+" "+"dosen`t exist in FileLock");
            ans.add("ERROR");
            printWriter.println("ERROR");
            ServerMain.WriteTOLog("ERROR : The file: " + _FileToUnlock + " doesnt exist in FileLock \n");
        }//END else (file not exist in FileLock --> file not locked)

        /**
         * if the first server return OK(unlock) -->the client get OK
         * if the first server return ERROR(no unlock) --> the client get ERROR
         */

           if(ans.get(0).equals("OK"))
           {
               printWriter.println("OK");
               ServerMain.WriteTOLog("The first server : "+ServerMain.servsock.getInetAddress().toString()+" unlocke the file.");
           }
            else//ans.get(0).equals("ERROR)
            {
                printWriter.println("ERROR");
            }



    }//End unlock for first SERVER

    public void UNLOCK_1(String _fileToUnlock , String ipClient)
    {
        if (FileToLock.containsKey(_fileToUnlock))//when this name file exist in FileLock
        {
            if (ipClient.equals(FileToLock.get(_fileToUnlock)))//when the same client that lock this file try to unlock this file
            {
                FileToLock.remove(_fileToUnlock);
                printWriter.println("OK");
                printWriter.close();
                ServerMain.WriteTOLog("OK: The file " + _fileToUnlock + " Unlocked by the same client that locked this file. on server : " +ServerMain.servsock.getInetAddress().toString()+".by the client : "  +ipClient+ "\n");
            }//END if(the same client that lock the file try make unlock on this file)
            else
            {
                printWriter.println("ERROR");
                ServerMain.WriteTOLog("ERROR : The file: " + _fileToUnlock + " do not currently locked by this client(+"+ipClient+")"+"\n");

                printWriter.close();
            }//END else(not the same client that lock this file try make unlock)

        }//END if (file exists in fileLock --> file locke)
        else
        {
            printWriter.println("ERROR");
            ServerMain.WriteTOLog("ERROR : The file: " + _fileToUnlock + " doesnt exist in FileLock on the server :"+ServerMain.servsock.getInetAddress().toString()+"\n");
        }//END else(file not exist in FileLock --> file not lock)

    }//END unlock for others servers

    //-------------set version to the file that uploaded to temp folder------------//
    public void setTempVersion(String FileName)
    {
        int _version = 0;
        String digest=null;
        String time=null;
        File tempPathFile = new File(tempFolder.getPath()+"\\"+FileName);//path to the temp folder with ha name of the file

        if (tempPathFile.exists())
        {
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            if (!tempVersion.containsKey(FileName) && !Version_1.containsKey(FileName))//when the name file not exist in Version
            {
                MessageDigest msgD;
                try
                {
                    msgD = MessageDigest.getInstance("SHA-256");
                    digest = checkSum(msgD,tempPathFile );//digest
                    time = dt.format(tempPathFile.lastModified());//time
                } catch (NoSuchAlgorithmException | IOException e) {
                    ServerMain.WriteTOLog("update of version(digest && timeStamp) failed");
                }

                Version_1.put(FileName,digest+" "+time);
                tempVersion.put(FileName, _version + 1);

            }//END if(file not exists in Version&&Version_1)
            else//exist in Version --> update the version(number,digest,timestamp)
            {
                MessageDigest msgD;
                try
                {
                    msgD = MessageDigest.getInstance("SHA-256");
                    digest = checkSum(msgD, tempPathFile);//digest
                    time = dt.format(tempPathFile.lastModified());//time
                } catch (NoSuchAlgorithmException | IOException e) {
                    ServerMain.WriteTOLog("update of version(digest && timeStamp) failed");
                }

                Version_1.replace(FileName,digest+" "+time);
                //update version number
                int temp = tempVersion.get(FileName);
                tempVersion.replace(FileName, temp + 1);
            }//END else(file exists in Version && version_1)

        }



    }



    public void setVersion(String FileName) {

        int _version = 0;
        String _name = FileName;
        String digest = null;
        String timeStamp= null;
        String LockBy;
        File folder = new File(mainFolder.getPath());
        File pathFile = new File(folder + "\\" + FileName);//path to the root folder with a name of the file
        File tempPathFile = new File(tempFolder.getPath()+"\\"+_name);//path to the temp folder with ha name of the file

        if (pathFile.exists())//exist in server root folder
        {

            //the file already in Version(name, number of version) and in Version_1(name,digest+timestamp)
            SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            if (!Version_1.containsKey(_name))//when the name file not exist in Version
            {
                MessageDigest msgD;
                try
                {
                    msgD = MessageDigest.getInstance("SHA-256");
                    digest = checkSum(msgD, pathFile);//digest
                    timeStamp = dt.format(pathFile.lastModified());//time
                } catch (NoSuchAlgorithmException | IOException e) {
                    ServerMain.WriteTOLog("update of version(digest && timeStamp) failed");
                }

                Version_1.put(_name,digest+" "+timeStamp);
                Version.put(_name, _version + 1);


            }//END if(file not exists in Version&&Version_1)

            else//exist in Version_1 --> update the version(number,digest,timestamp)
            {
                MessageDigest msgD;
                try
                {
                    msgD = MessageDigest.getInstance("SHA-256");
                    digest = checkSum(msgD, pathFile);//digest
                    timeStamp = dt.format(pathFile.lastModified());//time
                } catch (NoSuchAlgorithmException | IOException e) {
                    ServerMain.WriteTOLog("update of version(digest && timeStamp) failed");
                }

                Version_1.replace(_name,digest+" "+timeStamp);
///////////////////////////////////
                if (inOffer)
                {
                    int temp= Version.get(_name);
                    Version.replace(_name,temp+1);
                }
/////////////////////////////////////
                else
                {
                    //update version number
                    int temp = tempVersion.get(_name);

                    Version.put(_name, temp);
                    Version.replace(_name,temp);

                    //who lock the file.
                    LockBy = FileToLock.get(_name);
                }


            }//END else(file exists in Version && version_1)


        }//END if(file exists in root folder)

        else//when this file name not already exist in the folder.
        {
            //File exists in temp folder.
            if (tempPathFile.exists())
            {
                MessageDigest msgD;
                SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                try {
                    msgD = MessageDigest.getInstance("SHA-256");
                    digest = checkSum(msgD, tempPathFile);//digest
                    timeStamp = dt.format(tempPathFile.lastModified());//time
                    Version_1.put(_name,digest+" "+timeStamp);
                    tempVersion.put(_name,1);
                    //ServerMain.WriteTOLog("full version of file after upload : "+ Version_1);
                } catch (NoSuchAlgorithmException | IOException e) {
                    e.printStackTrace();
                }

            }//END if(file exists in temp folder)


            _version = 1;
            Version.put(_name, _version);

        }//END else()


    }//End SetVersion(file not exists in root folder)


    public String checkSum(MessageDigest msgD, File file) throws IOException {
        File folder = new File(mainFolder.getPath());

        FileInputStream fileInputStream = new FileInputStream(file);

        byte[] bytes = new byte[1024];
        int bCount = 0;

        while ((bCount = fileInputStream.read(bytes)) != -1) {
            msgD.update(bytes, 0, bCount);

        }

        fileInputStream.close();
        byte[] bytes1 = msgD.digest();

        StringBuilder _strbuil = new StringBuilder();
        for (int i = 0; i < bytes1.length; i++) {
            _strbuil.append(Integer.toString((bytes1[i] & 0xff) + 0x100, 16).substring(1));
        }
        return _strbuil.toString();
    }//END checkSum

//-------------------GET-VERSION------------------//
    public void UpVersion(String name) {
        File folder = new File(mainFolder.getPath());
        int _version = 0;
        String res = "";
        File pathFile = new File(folder + "\\" + name);

        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        MessageDigest msgD;
        String versionDigest;
        String timeStamp;

        try {
            //when file name exist in a folder server
            if (pathFile.exists())
            {
                _version =1;
                msgD = MessageDigest.getInstance("SHA-256");
                timeStamp = dt.format(pathFile.lastModified());
                versionDigest = checkSum(msgD, pathFile);
                if (Version.containsKey(name) && Version_1.containsKey(name))
                {
                    Version.get(name);
                    Version_1.get(name);
                }
                else
                {
                    Version.put(name, _version);
                    Version_1.put(name,versionDigest+" "+timeStamp);
                }
                res+=" " + Version.get(name);       //Version number digest time
                res += " " + Version_1.get(name);   //digest+time
                //res += " " + dt.format(pathFile.lastModified());
                if(FileToLock.containsKey(name))
                {
                    res+= " LockedBy:"+FileToLock.get(name);
                }

                printWriter.println("VERSION" + res.toUpperCase(Locale.ROOT) + " FROM:" + ServerMain.servsock.getInetAddress().toString());
                ServerMain.WriteTOLog("OK : The version of the file : " + name + " is: " + res + "\n");
            } else {
                printWriter.println("ERROR");
                ServerMain.WriteTOLog("ERROR: The file : " + name + " doesnt exist in the server folder.\n");
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            ServerMain.WriteTOLog("version doesnt available");
            e.printStackTrace();
        }


    }//END UpVersion


    public void stopCon() {
        ServerMain.WriteTOLog("Finished and closed the connection with: " + socket.getRemoteSocketAddress().toString());
        try {
            socket.close();
        } catch (Exception ex) {
            ServerMain.WriteTOLog("cant close the connection with: "+socket.getRemoteSocketAddress().toString());
        }
        return;
    }//END stopCon


}//END SERVERHANDLE
