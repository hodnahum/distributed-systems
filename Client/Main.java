package Client;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {
        JFrame frame =  new ClientGui("Client interface");
        frame.setVisible(true);


    }
}
