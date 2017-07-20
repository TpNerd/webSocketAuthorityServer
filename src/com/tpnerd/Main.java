package com.tpnerd;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;


    public class Main extends WebSocketServer {

        /** The web socket port number */
        private static String databaseAddress="jdbc:hsqldb:file:nerd.db";
        private static int PORT = 8887;
        public static final String USER_TABLE="users";
        public static final String COLUMN_ID="id";
        public static final String COLUMN_EMAIL="email";
        public static final String COLUMN_PASSWORD="password";
        public static final String COLUMN_USERNAME ="username";
        public static final String CREATE_TABLE_USERS = "CREATE TABLE "+ USER_TABLE + "("
                + COLUMN_ID+" INTEGER NOT NULL,"+
                COLUMN_EMAIL + " VARCHAR(30) NOT NULL,"+
                COLUMN_USERNAME + " VARCHAR(30) NOT NULL,"+
                COLUMN_PASSWORD+" VARCHAR(30) NOT NULL, PRIMARY KEY ("+COLUMN_ID+"));";

        private Set<WebSocket> conns;
        private Map<WebSocket, String> nickNames;
        private static Connection authconnection;
        private static int userIndex=0;
        /**
         * Creates a new WebSocketServer with the wildcard IP accepting all connections.
         */
        public Main() throws SQLException {
            super(new InetSocketAddress(PORT));
            conns = new HashSet<>();
            nickNames = new HashMap<>();
            System.out.println("Server started at address ");
            try {
                Class.forName("org.hsqldb.jdbc.JDBCDriver" );
            } catch (Exception e) {
                System.out.println("ERROR: failed to load HSQLDB JDBC driver.");
                e.printStackTrace();
                return;
            }

            authconnection = getConnection();
            System.out.println(databaseAddress+" available");



            userIndex=getTotalRecords(authconnection);

            if (userIndex==0) {
                System.out.println("Users is empty");

                authconnection.createStatement().executeUpdate(
//                        "IF NOT EXISTS " + USER_TABLE + " " +
                                CREATE_TABLE_USERS);
                insertUser("eugenio@amato.it","Toshi","monero11",authconnection);
                userIndex++;
            }

            System.out.println("Actually "+userIndex+" is the user index");
            System.out.println("Actually "+getTotalRecords(authconnection)+" users registered");
            if (isTaken("Toshi",authconnection))
                System.out.println("name Toshi is taken!");
            int level2=accessLevel("Tosh","monero11",authconnection);
            System.out.println("Tosh has level "+level2);
            int level=accessLevel("Toshi","monero11",authconnection);
            System.out.println("Toshi has level "+level);
            int level3=accessLevel("eugenio@amato.it","monero11",authconnection);
            System.out.println("eu has level "+level3);


            authconnection.close();




        }

        private int getTotalRecords(Connection auc) {

            try ( Statement statement = auc.createStatement();) {

                ResultSet result = statement.executeQuery("SELECT count(*) as total FROM "+USER_TABLE);

                if (result.next()) {

                    return result.getInt("total");

                }

            } catch (SQLException e) {

                e.printStackTrace();

            }

            return 0;

        }

        private static Connection getConnection() throws SQLException {
            return DriverManager.getConnection(databaseAddress, "SA", "");
        }

        public boolean isTaken(String nam, Connection auc) throws SQLException {
        String selectQuery1="select * from "+USER_TABLE+ " where "+COLUMN_USERNAME+" = '"+nam+"';";
        ResultSet rs=
        auc.createStatement().executeQuery(selectQuery1);

        if (rs.next())
        {
            return true;
        }
            return false;
        }

        public static int accessLevel(String name, String password, Connection con) throws SQLException {

            String qq = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USERNAME + "='" + name + "' and " + COLUMN_PASSWORD
                    + "='" + password + "'";
            ResultSet rs =
                    con.createStatement().executeQuery(qq);
            if (rs.next())
                return 1;
            else {

                String qq2 = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_EMAIL + "='" + name + "' and " + COLUMN_PASSWORD
                        + "='" + password + "'";
                ResultSet rs2 =
                        con.createStatement().executeQuery(qq2);

                if (rs2.next())
                    return 1;
                else


                return 0;
            }
        }


        private static int getFirstFreeId(Connection auc)
        {
            try {
                ResultSet r=auc.createStatement().executeQuery(
                        "SELECT MAX (ID) FROM USERS"
                );
                if (r.next())
                return r.getInt(1)+1;
            } catch (SQLException e) {
                e.printStackTrace();
            }


            return -1;
        }

        private static boolean insertUser(String email, String username, String password, Connection auc)
        throws SQLException {


            userIndex=getFirstFreeId(auc);
            System.out.println("first free id is "+userIndex);

            String inserter="INSERT INTO "+USER_TABLE+
                    " ("+
                    COLUMN_ID+","+COLUMN_EMAIL+","+COLUMN_USERNAME+","+COLUMN_PASSWORD
                    +") VALUES ("+userIndex+",'"+
                    email+"','"+username+"','"+password
                    +"')"
                    ;
            try {
                auc.createStatement().executeUpdate(inserter);
            }
            catch (SQLException e){
                e.printStackTrace();
                return false;
            }

            return true;
        }







        /**
         * Method handler when a new connection has been opened.
         */
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            conns.add(conn);
            System.out.println("New connection from " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        }

        /**
         * Method handler when a connection has been closed.
         */
        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            String nick = nickNames.get(conn);
            conns.remove(conn);
            nickNames.remove(conn);
            if (nick!= null) {
                removeUser(nick);
            }
            System.out.println("Closed connection to " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        }

        /**
         * Method handler when a message has been received from the client.
         */
        @Override
        public void onMessage(WebSocket conn, String message) {
            System.out.println("Received: " + message);
            if (message.substring(0,3).equals(">>>"))
            {
                String command=message.substring(3);
                System.err.println("received Command : "+command);
                String[] part=command.split(" ");

                /*
                possibili comandi
                verifica nome disponibile
                checkavailable NOME
                resul vero-falso


                verifica userlevel
                userlevel nome password
                risposta integer

                registra nuovo utente
                register email nome password
                risposta ok-fallito
                 */
                switch (part[0].charAt(0)) {
                    case 'c':
                    {
                        boolean trovato = false;
                        try {
                            trovato = isTaken(part[1], getConnection());
                        } catch (SQLException e) {
                            e.printStackTrace();
                            conn.send("<<<err");
                            return;
                        }
                        conn.send(trovato ? "<<<y" : "<<<n");
                        System.out.println(trovato?"y sent to auth question":"" +
                                "n sent to auth question");
                        break;
                    }
                    case 'u':
                    {
                        int level=0;
                        if (part.length<2) return;

                        try {
                            level=accessLevel(part[1],part[2],getConnection());
                        }
                        catch ( SQLException qe){
                            qe.printStackTrace();
                            conn.send("<<<err");
                            return;
                        }

                        conn.send("<<<"+level);
                        System.out.println("query answered "+level);

                        break;
                    }
                    case 'r':
                        System.out.println("checking username availability");
                        boolean trovato = false;
                        try {
                            trovato = isTaken(part[2], getConnection());
                        } catch (SQLException e) {
                            e.printStackTrace();
                            conn.send("<<<err");
                            return;
                        }

                        System.out.println("checking username availability 2");

                        if (trovato)
                        {
                            conn.send("<<<taken");
                            System.out.println("sent taken");
                        }
                        else {
                            boolean success = false;


                            try {
                                success = insertUser(part[1], part[2], part[3], getConnection());
                            } catch (SQLException e) {
                                e.printStackTrace();
                                conn.send("<<<err");
                                return;
                            }

                            conn.send("<<<" + success);
                            System.out.println("sent " + success);
                        }
                        break;
                }



            }
            else
            if (!nickNames.containsKey(conn)) {
                //No nickname has been assigned by now
                //the first message is the nickname
                //escape the " character first
                message = message.replace("\"", "\\\"");

                //broadcast all the nicknames to him
                for (String nick : nickNames.values()) {
                    conn.send("{\"addUser\":\"" + nick + "\"}");
                }

                //Register the nickname with the
                nickNames.put(conn, message);

                //broadcast him to everyone now
                String messageToSend = "{\"addUser\":\"" + message + "\"}";
                for (WebSocket sock : conns) {
                    sock.send(messageToSend);
                }
            } else {
                //Broadcast the message
                String messageToSend = "{\"nickname\":\"" + nickNames.get(conn)
                        + "\", \"message\":\"" + message.replace("\"", "\\\"") +"\"}";
                for (WebSocket sock : conns) {
                    sock.send(messageToSend);
                }
            }
        }


        /**
         * Method handler when an error has occured.
         */
        @Override
        public void onError(WebSocket conn, Exception ex) {

            String nick = nickNames.get(conn);
            conns.remove(conn);
            nickNames.remove(conn);
            if (nick!= null) {
                removeUser(nick);
            }
           if (conn==null)
           System.out.println("ERROR from null websocket connection :"+ex.toString());
               else
            System.out.println("ERROR from " + conn.getRemoteSocketAddress().getAddress().getHostAddress()+" "
            );
            ex.printStackTrace();
        }

        private void removeUser(String username) {

            String messageToSend = "{\"removeUser\":\"" + username + "\"}";
            for (WebSocket sock : conns) {
                sock.send(messageToSend);
            }


        }


        /**
         * Main method.
         */
        public static void main(String[] args) throws SQLException {
            Main server = new Main();
            server.start();
            System.out.println("Server listening  to port "+PORT);

            Scanner in=new Scanner(System.in);
            while(true) {
                String a = in.nextLine();
                if (a .toLowerCase().equals("exit"))
                {
                    System.out.println("exiting server");
                    break;
                }
                else
                    System.out.println("command unknown "+a);


            }


        }

    }