package Server;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.channels.FileLock;
import java.nio.file.Files;

import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class for a multithreaded .
 * configuration file : this file contain servers (server name , server ip , server port).
 * This file is reade into a list as soon as the server starts working.
 * user-server choose a server by his name.
 * ServerSocket is open with the ip and port that choose by user-server.
 *
 * HashMap FileToLock: created here and will be pass to others class.
 * this FileToLock will use for the files that lock(KEY:name file ,VALUE:ip of the server that locked this file )
 *
 * user-server can choose :  stop listening by entering 'stop' , resume listening by entering 'resume' and quit by entering 'quit'
 */


public class ServerMain {


    public static ServerSocket servsock = null;
    private static String FileDir="";
    private static PrintWriter printWriter;
     private static String folderTemp = "/src/Server/tempFolder";
   // private static String folderTemp = "/tempFolder";

    //private static String configuration = "/configuration.txt";
    private static String _logPath ="/logFile.txt";
    private static File logFile;
    public static int port;
    public static ArrayList<Socket> sockets = new ArrayList<Socket>();
    public static ArrayList<String> ipAndPort = new ArrayList<>();
    public static ArrayList<String> ipConnectedNow = new ArrayList<>();
    public static String nameServer;
    public static File checkFolder;
    public static File tempFolder;
    public static InetAddress address;
    public static ConcurrentHashMap<String,String> FileToLock = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,Integer>Version= new ConcurrentHashMap<>();
   public static ConcurrentHashMap<String,String> Version_1 = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,Integer> tempVersion = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
//        ConcurrentHashMap<String,String> FileToLock = new ConcurrentHashMap<>();
//        ConcurrentHashMap<String,Integer>Version= new ConcurrentHashMap<>();
//        ConcurrentHashMap<String,String> Version_1 = new ConcurrentHashMap<>();
//        ConcurrentHashMap<String,Integer> tempVersion = new ConcurrentHashMap<>();
        String basePath = new File("").getAbsolutePath();
        String[] temp;

        //create log file.
        logFile = new File((basePath+_logPath));
        if (logFile.exists()) {

            logFile.delete();
        }
        Files.createFile(logFile.toPath());



        //read configuration file (with list of servers)
        File ipPort = new File("configuration.txt");
        if (ipPort.exists())
        {
            List<String> readFile = Files.readAllLines(Paths.get(ipPort.getAbsolutePath()));
            for (String potIP : readFile)
            {
                ipAndPort.add(potIP.split(":")[0]+" "+potIP.split(":")[1]+" "+potIP.split(":")[2]);
            }
            WriteTOLog("read from configuration file with the list of servers." +" "+Paths.get(ipPort.getAbsolutePath())+".\n");

        }
        else
        {
            WriteTOLog("ERROR THE FILE NOT EXIST.\n");
        }




        //read from configuration file (path to root dir and the port of server)
        File configFile = new File(basePath+"/config.txt");//conig file
        File folderServer = null;
        if (configFile.exists())//config
        {
            //read from configuration file(root dir + port server)
            List<String> readFile = Files.readAllLines(Paths.get(configFile.getAbsolutePath()));
            for (String line : readFile)
            {
                temp = line.split(":");
                folderServer = new File(temp[0]);//read root path
                port = Integer.parseInt(temp[1]);//read ports server
            }
            WriteTOLog("read from configuration file whit root path and port." +" "+Paths.get(configFile.getAbsolutePath())+".\n");

        } else
        {
            WriteTOLog("ERROR THE FILE NOT EXIST.\n");
        }


        //get ips
//        InetAddress address = selectIPAddress();

        address = selectIPAddress();


        for(String line : ipAndPort)
        {
            if (!address.toString().equals("/"+line.split(" ")[1]))
            {
                ipConnectedNow.add(line.split(" ")[0]+" "+line.split(" ")[1]+" "+line.split(" ")[2]);
            }
            else
            {
                nameServer = line.split(" ")[0];//THE NAME OF FIRST SERVER
            }
        }

        //create folder server
        checkFolder = new File(basePath + folderServer);

        if (!checkFolder.exists())
        {
            Files.createDirectory(checkFolder.toPath());

        }

         tempFolder = new File(basePath+folderTemp);
        if(!tempFolder.exists())
            Files.createDirectory(tempFolder.toPath());

        String line = " ";
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        while (true)
        {
            Listener listener = null;

            // open a socket
            servsock = new ServerSocket(port, 10, address);

            syncNow();
            listener= new Listener(servsock,checkFolder,logFile,FileToLock,Version,ipConnectedNow,tempFolder,Version_1, tempVersion);
            listener.start();

            System.out.println("Started to listen.  Enter \"stop\" to pause listening: ");
            WriteTOLog("\"Started to listen.  Enter \"stop\" to pause listening: ");
            do {
                line = bf.readLine();
            } while (!line.equalsIgnoreCase("stop"));
            WriteTOLog("user server asked to STOP");
            // server user asked to stop
            listener.interrupt();
            servsock.close();
            System.out.println("Stopped listening.  To quit, enter \"quit\".  To resume listening, enter \"resume\": ");
            WriteTOLog("Stopped listening.  To quit, enter \"quit\".  To resume listening, enter \"resume\": ");
            do {
                line = bf.readLine();
            } while (!line.equalsIgnoreCase("quit") && !line.equalsIgnoreCase("resume"));

            if (line.equals("resume")) {
                WriteTOLog("user server asked to resume listening.\n");
                continue;
            } else if (line.equals("quit")) {
                break;
            }
        }//end while(true)

        //server quit.
        WriteTOLog("user server asked to quit listening.\nGoodbye.");
        System.out.println("Goodbye.");
        return;
    }

    //this function write to log file
    public static void WriteTOLog(String info){
        try {

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date(System.currentTimeMillis());
            FileWriter writer = new FileWriter(logFile,true);
            writer.write(System.lineSeparator()+ LocalDateTime.now().toString()+" "+info);
            writer.close();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public static InetAddress selectIPAddress()
    {
        // get the local IPs
        Vector<InetAddress> addresses = getLocalIPs();
        // see how many they are

        System.out.println("Choose an IP address to listen on:");
        for (int i = 0; i < addresses.size(); i++)
        {
            // show it in the list
            System.out.println(i + ": " + addresses.elementAt(i).toString());
        }

        BufferedReader brIn = new BufferedReader(new InputStreamReader(System.in));
        int choice = -1;

        while ( choice < 0 || choice >= addresses.size())
        {
            System.out.print(": ");
            try {
                String line = brIn.readLine();
                choice = Integer.parseInt(line.trim());
            }
            catch (Exception ex) {
                System.out.print("Error parsing choice\n: ");
            }
        }

        return addresses.elementAt(choice);

    }

    public static Vector<InetAddress> getLocalIPs()
    {
        // make a list of addresses to choose from
        // add in the usual ones
        Vector<InetAddress> adds = new Vector<InetAddress>();
        try {
            adds.add(InetAddress.getByAddress(new byte[] {0, 0, 0, 0}));
            adds.add(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}));
        } catch (UnknownHostException ex) {
            // something is really weird - this should never fail
            System.out.println("Can't find IP address 0.0.0.0: " + ex.getMessage());
            ex.printStackTrace();
            return adds;
        }

        try {
            // get the local IP addresses from the network interface listing
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while ( interfaces.hasMoreElements() )
            {
                NetworkInterface ni = interfaces.nextElement();
                // see if it has an IPv4 address
                Enumeration<InetAddress> addresses =  ni.getInetAddresses();
                while ( addresses.hasMoreElements())
                {
                    // go over the addresses and add them
                    InetAddress add = addresses.nextElement();
                    // make sure it's an IPv4 address
                    if (!add.isLoopbackAddress() && add.getClass() == Inet4Address.class)
                    {
                        adds.addElement(add);
                    }
                }
            }
        }
        catch (SocketException ex)
        {
            // can't get local addresses, something's wrong
            System.out.println("Can't get network interface information: " + ex.getLocalizedMessage());
        }
        return adds;
    }

    public static boolean connectedNow(String ip , int port)
    {
        boolean con;
        try {
            Socket socket = new Socket(ip,port);
            sockets.add(socket);
            con = true;
        } catch (IOException e) {
            System.out.println("not connected");
            con = false;
        }

        return con;
    }

    private static void syncNow()
    {
        Socket socket;
        ConcurrentHashMap<String, ArrayList<String>> ipAndFiles = new ConcurrentHashMap<>();
        boolean empty = false;
        int numFile1 = 1;
        System.err.println(nameServer+" on: "+servsock.getInetAddress().toString()+" "+servsock.getLocalPort()+" start sync files.");
        WriteTOLog(nameServer+" on: "+servsock.getInetAddress().toString()+" "+servsock.getLocalPort()+" start sync files.");
        for (String s : ipConnectedNow)
        {
            try
            {
                //open connection
                socket = new Socket(s.split(" ")[1], Integer.parseInt(s.split(" ")[2]));
                try {
                    PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    while (!socket.isClosed())
                    {
                        System.err.println("Connect to the server: " + s);
                        WriteTOLog("Connect to the server: " + s);
                        System.err.println(nameServer+" sask for: 'GetList'");
                        WriteTOLog(nameServer+" sask for: 'GetList'");
                        printWriter.println("GETLIST ");
                        //---server that asked return file list---//
                        String ansFromServer = reader.readLine();
                        if (ansFromServer.equals("FILELIST "))
                        {
                            empty = true;
                            socket.close();
                            break;
                        }

                        ansFromServer = ansFromServer.replace("FILELIST ", "");//'FILELIST -> "" '
                        String[] serverFilesList = ansFromServer.split(" ");//serverFilesList[0] = file1 , serverFilesList[1] = file2 ....
                        ArrayList<String> fileList = new ArrayList<>();
                        for (String file:serverFilesList)
                        {
                            fileList.add(file);
                        }
                        ipAndFiles.put(s, fileList);//KEY = server info(nameServer,ip,port) VALUE = [file1,file2.....]
                        socket.close();
                    }//END WHILE(!socket.close)

                    //----The server folder (which received the GETLIST command) is empty----//
                    if (empty)
                    {
                        System.err.println("folder of server: "+s+" empty. finish sync with this server");
                        WriteTOLog("folder of server: "+s+" empty. finish sync with this server");
                        continue;
                    }//END IF(empty==tru)
                } catch (Exception e)
                {
                    System.err.println("EXCEPTION: server: "+s+" disconnected");
                    WriteTOLog("EXCEPTION: This server: " + s + " disconnected.");
                    continue;
                }
                ArrayList<String> valueFilesTemp;
                valueFilesTemp = ipAndFiles.get(s);//arrayList hold all files of server 's'
                for (int j = 0; j < ipAndFiles.get(s).size(); j++)
                {
                    //open connection
                    socket = new Socket(s.split(" ")[1], Integer.parseInt(s.split(" ")[2]));
                    try
                    {
                        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        while (!socket.isClosed())
                        {
                            //server send get version for file in index i (from arrayList)
                            printWriter.println("GETVERSION " + ipAndFiles.get(s).get(j));
                            String ansFromServer = reader.readLine();//VERSION numVersion digest time lock(if locked)
                            numFile1= Integer.parseInt(ansFromServer.split(" ")[1]);
                            String LockBy = ansFromServer.split(" ")[4];

                            if (!ansFromServer.equals("ERROR"))
                            {
                                String ansFromServer_1 ;
                                //----------save from getVersion only timeStamp(date+time),digest AND who lock a file(if the file locked)-----------//
                                if (LockBy.contains("LockedBy:"))
                                {
                                     ansFromServer_1 = ansFromServer.split(" ")[2]+" "+ansFromServer.split(" ")[3] + " " + LockBy;//time+who lock
                                }
                                else
                                {
                                    ansFromServer_1 = ansFromServer.split(" ")[2]+" "+ansFromServer.split(" ")[3];//time
                                }

                                String ValueFile = ipAndFiles.get(s).get(j);//A single file from all  files of server 's'
                                //upDate valueFilesTemp: for every file in valueFilesTemp add timeStamp and who lock
                                valueFilesTemp.set(j, ValueFile + " " + ansFromServer_1);
                            }//END if(!ERROR)
                             socket.close();
                        }//END WHILE(!socket.close)

                    } catch (Exception e)
                    {
                        System.out.println("GETVERSION failed.This server: "+s+" disconnected.");
                        WriteTOLog("GETVERSION failed.This server: "+s+" disconnected.");
                        break;
                    }
                }//END FOR(Go over the files of server 's')

                ipAndFiles.put(s, valueFilesTemp);//KEY = server VALUE = files(of server 's')
            } catch (IOException e) {
                System.err.println("This server: " + s + " not connected.");
                WriteTOLog("This server: " + s + " not connected.");
                continue;
            }
        }//END for(connected to others servers)

        //System.out.println("files with version: "+ipAndFiles);

        //get the most update files(by timeStamp) with her server
        ArrayList<String> filesToDownload = checkFileToDownload(ipAndFiles);

        for (String file_download:filesToDownload)
        {
            try
            {
                //open connection
                socket = new Socket(file_download.split(" ")[1], Integer.parseInt(file_download.split(" ")[2]));

                try
                {
                    OutputStream outputStream = socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(outputStream,true);
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    writer.println("DOWNLOAD "+file_download.split(" ")[3]);
                    String response = reader.readLine();
                    if (response.equals("OK"))
                    {

                        String fileName = file_download.split(" ")[3];
                        System.err.println("sync file: "+fileName+" with: "+file_download.split(" ")[0]+" "+file_download.split(" ")[1]+" "+ Integer.parseInt(file_download.split(" ")[2]));
                        int bytesRead;
                        String outputPath = checkFolder.getPath()+ "\\" + fileName;
                        try
                        {
                            InputStream in = socket.getInputStream();

                            DataInputStream clientData = new DataInputStream(in);

                            OutputStream output = new FileOutputStream(outputPath);
                            long size = clientData.readLong();
                            byte[] buffer = new byte[1024];
                            while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                                output.write(buffer, 0, bytesRead);
                                size -= bytesRead;
                            }
//                            in.close();
//                            clientData.close();
//                            output.close();
                            WriteTOLog("sync of: "+fileName+" succeeded");
                            String digest = file_download.split(" ")[4];
                            String timeStamp = file_download.split(" ")[5];
                            //set version to file and update file indexer
                            Version_1.put(fileName,digest+" "+timeStamp);
                            Version.put(fileName,numFile1);
                            if(file_download.length() == 7)
                            {
                                String LockBy = file_download.split(" ")[6];
                                FileToLock.put(fileName,LockBy);
                            }
                            System.err.println("sync of: "+fileName+" succeeded");
                        } catch (IOException ex) {
                            System.out.println("Exception: "+ex);
                        }
                    }
                }catch (IOException ex)
                {
                    System.err.println("EXCEPTION: server: "+file_download.split(" ")[0]+" "+file_download.split(" ")[1]+" "+file_download.split(" ")[2]+" disconnected");
                    WriteTOLog("This server disconnected: "+file_download.split(" ")[0]+" "+file_download.split(" ")[1]+" "+file_download.split(" ")[2]);
                }

            }catch (IOException e)
            {
                System.err.println("This server: " + file_download.split(" ")[0]+" "+file_download.split(" ")[1]+" "+file_download.split(" ")[2] + " not connected.");
                WriteTOLog("This server: " + file_download.split(" ")[0]+" "+file_download.split(" ")[1]+" "+file_download.split(" ")[2] + " not connected.");
                continue;
            }
        }

        System.err.println("sync finish.");

    }//END SYNC


    /**
     * This function compares the VALUES  (from 'ipsAndFiles') that each server has (VALUES = files).
     * The comparison is made according to the timeStamp (date + time) of each file.
     * When there are several servers with the same file -> will select the most recent file-<
     * -> the file and the information on it and the server will enter the ArrayList('filesToDownload')
     * @param ipsAndFiles -> ConcurrentHashMap that contain : KEY=server name IP Port , VALUE=file1,file2...(arraylist)
     * @return 'filesToDownload' -> array list that has the most update files with his server
     */
    public static ArrayList<String> checkFileToDownload(ConcurrentHashMap<String,ArrayList<String>>ipsAndFiles)
    {
       ArrayList<String> filesToDownload = new ArrayList<>();
       boolean flag = false;
        for (Map.Entry<String,ArrayList<String>> entry: ipsAndFiles.entrySet())
        {
            String key = entry.getKey();
            ArrayList<String> files = entry.getValue();//files of server key = { ...}

            for(int i = 0; i<files.size(); i++)
            {
                boolean exist = false;
                for(int j = 0; j<filesToDownload.size(); j++)
                {
                    //ip file[j]
                    if(filesToDownload.get(j).contains(files.get(i).split(" ")[0]))
                    {
                        exist = true;

                        //convert timestamp string to DateFormat
                        String timeStamp = filesToDownload.get(j).split(" ")[5];
                        String timeTemp_1 = files.get(i).split(" ")[2];
                        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

                        try {
                            if(dt.parse(timeStamp).before(dt.parse(timeTemp_1)))
                            {
                                filesToDownload.set(j, key + " " + files.get(i));
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        break;
                    }//END if(file already exits in filesToDownload)
                    else
                    {
                        exist = false;
                    }//END else(file not exists in fileToDownload)
                }
                //if file not exists in filesToDownload --> add server and his file
                if(!exist)
                {
                    filesToDownload.add(key + " " +files.get(i));//serverName ip port file
                }
            }//END for(go over files of server 'key')
        }//END FOR(entry)
        //System.out.println("files with servers to download : " + filesToDownload);
        return filesToDownload;
    }



}