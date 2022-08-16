package Client;

import Server.ServerMain;

import javax.swing.*;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Scanner;

public class ClientProcess extends Thread{

    private static String FileDir="";
    private static String FileName="";
    public static int index;
    public static Socket socket = null;
    public static ArrayList<String> ipAndPort = new ArrayList<>();
    public static BufferedReader bufferedReader;
    public static PrintWriter printWriter= null;
    public static String folderClient = "/src/Client/FolderDown";//folder for client files
    //public static String folderClient = "/ClientFolderDown";//folder for client files

    public static String ip;
    public static int port;
    public static ArrayList<Socket> sockets = new ArrayList<>();
    public static String command;
    public static String path;
    public static boolean a = false;

    //FIRST CONSTRUCTOR
    public ClientProcess(ArrayList<String> ipPort,int index){

        super("ClientProcess-");
        this.ipAndPort = ipPort;
        this.index = index;


    }
    ////NOW: connected to the first server that online
    public ClientProcess(String command,ArrayList<String> ipPort,String path)
    {
        this.ipAndPort = ipPort;
        this.command = command;
        this.path = path;

        JOptionPane.showMessageDialog(null,"Trying to make a connection");
        for (String s : ipAndPort) {
            try {

                sockets.add(new Socket(s.split(":")[1], Integer.parseInt(s.split(":")[2])));
                a = true;
                JOptionPane.showMessageDialog(null, "Connected to :  " + s.split(":")[1]);
            } catch (IOException e) {
                a = false;

            }
            //Connected to the first server that online.(not try connected to all servers)
            if (a) {
                break;
            }

        }
    }

    public ClientProcess(int index ,String command,ArrayList<String> ipPort,String path )
    {
        this.index = index;
        this.ipAndPort = ipPort;
        this.command = command;
        this.path = path;

        try {
            JOptionPane.showMessageDialog(null,"Try make a connection to : " + ipAndPort.get(this.index).split(":")[1]);
            sockets.add(new Socket(ipAndPort.get(this.index).split(":")[1],Integer.valueOf(ipAndPort.get(this.index).split(":")[2])));
            JOptionPane.showMessageDialog(null,"Connected. "+ipAndPort.get(this.index).split(":")[1]);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,"not connected" + ipAndPort.get(this.index).split(":")[1]);
        }
    }
    //when client upload file to all server
    public static void sendToAllServer(String path)
    {
        String ans;
        OutputStream writerStream;
        String fileName = Path.of(path).getFileName().toString();
        try {

            for(int i = 0 ; i<sockets.size() ; i++)
            {
                writerStream = sockets.get(i).getOutputStream();
                printWriter=new PrintWriter(writerStream,true);
                SendFile(path,fileName);
                ans = ansFromServer(sockets.get(i));
                if(ans.equals("OK"))
                {
                    JOptionPane.showMessageDialog(null,"OK: file sent to the server: "+sockets.get(i).getInetAddress().toString() +"and to all servers that online. upload Succeeded. ");
                }
                if(ans.equals("ERROR"))
                {
                    JOptionPane.showMessageDialog(null,"ERROR: The file not lock and exsit in server folder\n upload failed. "+sockets.get(i).getInetAddress());
                }

            }
            sockets.removeAll(sockets);//after  uploading remove all sockets from list .

        } catch (IOException e) {
            e.printStackTrace();
        }



    }
    //**********for : client to multi server********//
    @Override
    public void run()
    {
        OutputStream writerStream;
        switch (command)
        {
            case "UPLOAD_TOALL":
                ChooseForAll();//open dailog for chooce file to upload
                if (FileDir.equals(""))//client dont choose any file .
                {
                    sockets.removeAll(sockets);
                    break;
                }
                sendToAllServer(FileDir);
                break;
            case "LOCK_TOALL":

                try {
                    for (int i = 0; i<sockets.size(); i++)
                    {
                        writerStream = sockets.get(i).getOutputStream();
                        printWriter=new PrintWriter(writerStream,true);
                        printWriter.println("LOCK "+ path);
                        String ans = ansFromServer(sockets.get(i));
                        if (ans.equals("OK"))
                        {
                            JOptionPane.showMessageDialog(null,"OK:The file:  "+path+" now is locke on:\n"+sockets.get(i).getInetAddress()+"AND in all servers that online.");

                        }
                        if (ans.equals("ERROR"))
                        {
                            JOptionPane.showMessageDialog(null,"ERROR:"+path+" is currently locked OR doesn`t exist(At least on one of the online servers)\nfrom: "+sockets.get(i).getInetAddress());
                        }

                    }
                    sockets.removeAll(sockets);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "UNLOCK_TOALL":

                try {
                    for (int i = 0; i<sockets.size(); i++)
                    {
                        writerStream = sockets.get(i).getOutputStream();
                        printWriter=new PrintWriter(writerStream,true);
                        printWriter.println("UNLOCK "+ path);
                        String ans = ansFromServer(sockets.get(i));
                        if (ans.equals("OK"))
                        {
                            JOptionPane.showMessageDialog(null,"OK:The file:  "+path+" is unlock on: "+sockets.get(i).getInetAddress()+"AND in all servers that online.");

                        }
                        if (ans.equals("ERROR"))
                        {
                            JOptionPane.showMessageDialog(null,"ERROR:"+path+" is currently locked OR doesn`t exist. on: "+sockets.get(i).getInetAddress());
                        }

                    }
                    sockets.removeAll(sockets);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "GETVERSION_TOALL":
                try {
                    for (int i = 0 ; i< sockets.size(); i++)
                    {
                        writerStream = sockets.get(i).getOutputStream();
                        printWriter=new PrintWriter(writerStream,true);
                        printWriter.println("GETVERSION " +path);
                        String[] getVertion = ansFromServer(sockets.get(i)).split(" ");
                        JOptionPane.showMessageDialog(null,getVertion,"GetVersion",JOptionPane.INFORMATION_MESSAGE);
                    }
                    sockets.removeAll(sockets);

                }catch (IOException e){
                    e.printStackTrace();
                }
                break;
        }

    }//END RUN
//**************************************************************************//

    public static void SendCommand(String CommandName,String path,int index)
    {
        OutputStream writeStream;
        String ans = null;
        String ansFromServer = null;
        String fileName = Path.of(path).getFileName().toString();

        try {
            if (ipAndPort.size() == 0)
            {
                JOptionPane.showMessageDialog(null,"You must choose a Server First !");

                return;
            }
            socket = new Socket(ipAndPort.get(index).split(":")[1],Integer.valueOf(ipAndPort.get(index).split(":")[2]));
            writeStream = socket.getOutputStream();
            printWriter=new PrintWriter(writeStream,true);
        } catch (SocketException ex)
        {
            JOptionPane.showMessageDialog(null,"this server not connected. "+ipAndPort.get(index).split(":")[0]);
            return;

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,"this server not connected! "+ipAndPort.get(index).split(":")[0]);
            return;
        }

        switch (CommandName)
        {


            case "DOWNLOAD" :
                getFile(fileName);

                break;

            case "List" :
                printWriter.println("GETLIST");
                String [] filelist = ansFromServer(socket).split(" ");
                JOptionPane.showMessageDialog(null,filelist,"List",JOptionPane.INFORMATION_MESSAGE);
                break;





            case "GETVERSION":
                printWriter.println("GETVERSION " +path);
                String[] getVersion = ansFromServer(socket).split(" ");
                JOptionPane.showMessageDialog(null,getVersion,"GetVersion",JOptionPane.INFORMATION_MESSAGE);
                break;

            case "STOP":
                printWriter.println("STOP");
                stopClientCon();
                break;
        }


    }//END sendCommand


    //send file to the server by use socket .
    public static void SendFile(String path , String name) throws IOException {

        String uploadFile;
        File kovetz =new File(path);
        try {

            uploadFile="UPLOAD "+name +" ";
            byte[] bytes= Files.readAllBytes(Paths.get(kovetz.toString()));
            uploadFile+=Base64.getEncoder().encodeToString(bytes);
            printWriter.println(uploadFile);
            String fileName = path;
            File myFile = new File(fileName);
            byte[] mybytearray = new byte[(int) myFile.length()];
            if(!myFile.exists()) {
                JOptionPane.showMessageDialog(null,"File does not exist..");
            }


        } catch (Exception e) {
            System.err.println("Exceptionnnn: "+e);
            JOptionPane.showMessageDialog(null,"Exceptionnnn: "+e);

        }


    }//END sendFile


    public static void getFile(String name)
    {   File fileToSave=null;
        String fileName;
        fileName = name;
        String downFile;
        int bytesRead;
        String ans;
        //String fileName;
        downFile="DOWNLOAD "+fileName +" ";

        printWriter.println(downFile);
        JFrame parentFrame = new JFrame();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");
        fileChooser.setSelectedFile(new File(fileName));
        int userSelection = fileChooser.showSaveDialog(parentFrame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
             fileToSave = fileChooser.getSelectedFile();

            fileToSave= new File(fileToSave.getAbsolutePath());
            System.out.println("Save as file: " + fileToSave.getAbsolutePath());
        }



        //get the relative path of this location
     //   String basePath = new File("").getAbsolutePath();

        //create folder to client`s files
//        File checkFolder = new File(basePath + folderClient);
//        if (!checkFolder.exists())
//        {
//
//            try {
//                Files.createDirectory(checkFolder.toPath());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }
        ans = ansFromServerDown();

        if(ans.equals("OK"))
        {
          // String outputPath = checkFolder.getAbsolutePath() + "\\" + name;

            try {

                InputStream in = socket.getInputStream();

                DataInputStream clientData = new DataInputStream(in);

              OutputStream output = new FileOutputStream(fileToSave.toString());

                long size = clientData.readLong();
                byte[] buffer = new byte[1024];

                while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                    output.write(buffer, 0, bytesRead);
                    size -= bytesRead;
                }

                in.close();
                clientData.close();
                output.close();
               JOptionPane.showMessageDialog(null,"OK: The file: "+name+" received successfully from server.");
            } catch (IOException ex) {
                System.out.println("Exception: "+ex);
            }

        }//IF OK
        if(ans.equals("ERROR"))
        {
            JOptionPane.showMessageDialog(null,"ERROR: Download failed ");
        }//IF ERROR


    }//END getFile



    //read from input stream .
    public static String ansFromServer(Socket socket)
    {
        BufferedReader reader = null;


        String ansFromServer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ansFromServer = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ansFromServer;
    }//END ansFromServer

    public static String ansFromServerDown()
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

    //check if the file excite in client folder
    public static String exicteFile(String fileName,File folder)
    {
        String exicte = null;
        File myFile = new File(folder.getPath()+"\\"+fileName);
        if(myFile.exists())
        {
            exicte = "yes";

        }
        else
        {
            exicte = "no";
        }


        return exicte;
    }//END exicte


    ////open  dialog for choose file to upload-->WHEN CLIENT SEND TO ALL SERVER
    public static void ChooseForAll()
    {
        FileDir="";
        JFileChooser chooser = new JFileChooser();
        Scanner in = null;
        if (chooser.showOpenDialog(null)== JFileChooser.APPROVE_OPTION)
        {

            File selectedFile =chooser.getSelectedFile();
            try {
                in = new Scanner(selectedFile);
            }
            catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            FileDir = selectedFile.getAbsolutePath();//get the path

        }

    }//END ChooseForAll

    //open dialog--->WHEN CLIENT SEND TO ONE SERVER
    public static void Choose(String btr)
    {
        JFileChooser chooser = new JFileChooser();
        Scanner in = null;
        String anss="";
        if (chooser.showOpenDialog(null)== JFileChooser.APPROVE_OPTION)
        {
            String commandName = btr.toString();
            File selectedFile =chooser.getSelectedFile();
            try {
                in = new Scanner(selectedFile);
            }
            catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
            FileDir= selectedFile.getAbsolutePath();//get the path
            try {

                SendCommand(commandName,FileDir.toString(),index);
                anss= ansFromServer(socket);

                if(anss.equals("OK"))
                {
                    JOptionPane.showMessageDialog(null,"OK: file sent to the server\nupload Succeeded.");
                }
                if(anss.equals("ERROR"))
                {
                    JOptionPane.showMessageDialog(null,"ERROR: The file doesnt lock\n upload failed.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }//END Choose


    //StopConnection.
    public static void stopClientCon(){
        try {
            socket.close();
            JOptionPane.showMessageDialog(null,"Closed connection.");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }//END stopClientCon



}//END CLASS : CLientProcess
