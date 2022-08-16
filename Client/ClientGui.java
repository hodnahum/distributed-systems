package Client;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientGui extends JFrame {

    private JPanel mainPanel1;
    private JComboBox ServerList;
    private JLabel serverLable;
    private JButton LockButton;
    private JButton UnlockButton;
    private JButton VersionButton;
    private JButton UploadButton;
    private JButton DownloadButton;
    private JLabel ActionLabele;
    private JLabel FileLable;
    private  JTextField FilePath;
    private JButton LIST;
    private JButton stopConection;
    private JButton UploadAllServers;
    private JButton LockForAllBottom;
    private JButton UnlockForAllButton;
    private JButton GetVertionButtonToAll;
    public static ArrayList<String> ipAndPort = new ArrayList<>();
    public int index;





    public ClientGui(String title)
    {
        super(title);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel1);
        this.pack();
        //read configuration file
        File ipPort = new File("configuration.txt");
        try {
            Scanner sc = new Scanner(ipPort);
            while (sc.hasNext())
            {
                ipAndPort.add(sc.nextLine());//array list with all Servers information
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }



        ServerList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ServerName = ServerList.getSelectedItem().toString();
                index = ServerList.getSelectedIndex();//the index of server that client chose.
                ClientProcess process = new ClientProcess(ipAndPort,index);


            }
        });






        DownloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String commandName = DownloadButton.getText().toString();

                //if client forget to insert file name.
                if(FilePath.getText().toString().equals(""))
                {
                    JOptionPane.showMessageDialog(null,"you must insert the name of file !");
                    return;
                }
                ClientProcess.SendCommand(commandName,FilePath.getText().toString(),index);



            }
        });
        LIST.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String commandName = LIST.getText().toString();
                ClientProcess.SendCommand(commandName,FilePath.getText(),index);

            }
        });


        stopConection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String commandName = stopConection.getText().toString();
                ClientProcess.SendCommand(commandName,FilePath.getText().toString(),index);

                System.out.println("Closed connection.");
            }
        });
        VersionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String commandName = VersionButton.getText().toString();
                if(FilePath.getText().toString().equals(""))
                {
                    JOptionPane.showMessageDialog(null,"you must insert the name of file !");
                    return;
                }
                else
                {
                    ClientProcess.SendCommand(commandName,FilePath.getText().toString(),index);
                }


            }
        });
        UploadAllServers.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String commandName = UploadAllServers.getText().toString();
                ClientProcess clientProcess = new ClientProcess(commandName,ipAndPort,FilePath.getText().toString());
                if(ClientProcess.a)
                {
                    clientProcess.run();
                }
                else
                {
                    JOptionPane.showMessageDialog(null,"No server is connected");
                }


            }
        });

        LockForAllBottom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(FilePath.getText().toString().equals(""))
                {
                    JOptionPane.showMessageDialog(null,"you must insert the name of file !");
                    return;
                }
                String CommandName = LockForAllBottom.getText().toString();
                //ClientProcess[] clients = new ClientProcess[len];
                ClientProcess clientProcess = new ClientProcess(CommandName,ipAndPort,FilePath.getText().toString());

                /*When the client was able to connect to the first server */
                if(ClientProcess.a)
                {
                    clientProcess.run();
                }
                else
                {
                    JOptionPane.showMessageDialog(null,"No server is connected");
                }



            }
        });
        UnlockForAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(FilePath.getText().toString().equals(""))
                {
                    JOptionPane.showMessageDialog(null,"you must insert the name of file !");
                    return;
                }
                String CommandName = UnlockForAllButton.getText().toString();
                int len = ServerList.getItemCount();

                ClientProcess clientProcess = new ClientProcess(CommandName,ipAndPort,FilePath.getText().toString());
                if(ClientProcess.a)
                {
                    clientProcess.run();
                }
                else
                {
                    JOptionPane.showMessageDialog(null,"No server is connected");
                }

//
            }
        });

        GetVertionButtonToAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(FilePath.getText().toString().equals(""))
                {
                    JOptionPane.showMessageDialog(null,"you must insert the name of file !");
                    return;
                }
                String CommandName = GetVertionButtonToAll.getText().toString();
                //int len = ServerList.getItemCount();
                int len = ipAndPort.size();
                ClientProcess[] clients = new ClientProcess[len];

                for (int i = 0 ; i<len ; i++)
                {
                    clients[i] = new ClientProcess(i,CommandName,ipAndPort,FilePath.getText().toString());
                }
                clients[0].run();
            }
        });
    }



}
