import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class Game {
    public boolean lost=false;
    public  boolean ogameover=false;
    public String oscore="0";
    public String line="4";
    public   BufferedReader in=null;
    public  PrintWriter out=null;
    public Socket tcpsocket = null;


    public static final int PIPE_DELAY = 100;
    private int pipeDelay;

    private Bird bird;
    private ArrayList<Pipe> pipes;
    private Keyboard keyboard;

    public int score=0;
    public Boolean gameover;
    public Boolean started;

    public Game() {
        keyboard = Keyboard.getInstance();

        restart();

    }

    public void restart() {
        started = false;
        gameover = false;

        score = 0;
        pipeDelay = 0;

        bird = new Bird();
        pipes = new ArrayList<Pipe>();
    }

    public void update() throws IOException, InterruptedException {

        watchForStart();

        if (!started)
            return;

        bird.update();

        if (gameover) {                             // if i died
          /*  while (!line.equalsIgnoreCase("gameover")){   // while the opponent didn't send that he died keep reading
                line=in.readLine();
            }*/
            return;
        }

        movePipes();


        checkForCollisions();

        out.println(score); // keep sending my score to the opponent
        out.flush();
        if(!ogameover) {               // ogameover = opponent died   so while he is alive
            line = in.readLine();  // read his score
            if (!line.equalsIgnoreCase("gameover")) {       // if he didn't send that he died

                oscore = line;   // NOTE : line while have 2 possibilties in this code 1- OPPONENT SCORE ( oscore) or 2- gameover
                // when its gameover we know that he died else will be his score so we will display it on the screen by saving it in oscore

            }
            else {
                ogameover = true; // if he send gameover means that he died
            }
        } else
            ogameover = true ;

    }

    public ArrayList<Render> getRenders() {
        ArrayList<Render> renders = new ArrayList<Render>();
        renders.add(new Render(0, 0, "lib/background.png"));
        for (Pipe pipe : pipes)
            renders.add(pipe.getRender());
        renders.add(new Render(0, 0, "lib/foreground.png"));
        renders.add(bird.getRender());
        return renders;
    }

    private void watchForStart() throws IOException {   // udp broadcasting and tcp algorithm

        if (!started && keyboard.isDown(KeyEvent.VK_SPACE)) {  // if i hit space means that iam ready to play

            started = true;
            boolean client = false;
            int port = 0;
            while (port < 1024 || port > 65536) { //our random generates from 0 to 99999 so we need to make sure its in range of  1024 and 65536
                Random rand = new Random(System.currentTimeMillis());  // copied from stackoverflow
                String id = String.format("%04d", rand.nextInt(100000));  // generates a random number based on timestamp
                port = Integer.parseInt(id);
            }  // port now has our random port number that will be our tcp port
            System.out.println("My TCP RANDOM PORT IS " + port);
            DatagramSocket socket = new DatagramSocket();       // new datagram socket for UDP broadcasting
            socket.setBroadcast(true); // enabling broadcast
            String message = "HI I AM DEVICE 1 MY PORT IS : " + port;  // preparing the message
            byte[] buffer = message.getBytes(); // putting the message in a byte array
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("192.168.43.255"), 2222); // putting the packet in packet
            socket.send(packet); // sending the packet
            DatagramSocket s = new DatagramSocket(2222); // Socket that i will receive on it the broadcast
            DatagramPacket n = new DatagramPacket(new byte[5000], 5000);   // packet that i will receive the message ata

            // first device to enter the game will be the client who connects to the second device ( the server)
            ServerSocket tcpsocket = new ServerSocket(port);
            Socket connect = null;
            tcpsocket.setSoTimeout(1000);  // THE SOCKET WILL TIMEOUT AFTER 5 Seconds if it doesn't receive the tcp connection I.E This device is a server
/* Device sends the message then assume its a server and try to accept a connection for 5 seconds if it doesn't find someone who connects to it then
  it is a client */

            try {
                connect = tcpsocket.accept(); // will accept any connection for 5s second
                client = false;  // if you received the tcp connection  then i am a srver and the opponent connected to me already
            } catch (SocketTimeoutException e) { // if the 1 second passed and i no one connected to me then iam a client
                client = true;  // i am client because no one was there before me to connect at me when i waited 5 seconds
            }

            if (client) {  // if iam a client ( the first player )

                System.out.println("Iam client");
                s.receive(n); // will block till someone enters and send a broadcast message
                System.out.println(new String(n.getData(), StandardCharsets.UTF_8)); // print the received message
                String op = new String(n.getData(), 0, n.getLength());  // preparing to get the port number of the opponent from the message
                StringTokenizer parse = new StringTokenizer(op);
                for (int i = 0; i < 10; i++) {
                    op = parse.nextToken();   // to get the last token which is the port number
                }
                int iport = Integer.parseInt(op); // iport is the tcp port of the opponent
                System.out.println("Opponent tcp port is " + iport);
                /* got the port number */
                String IP = n.getAddress().toString(); // getting the ip address of the opponent
                IP = IP.substring(1);
                System.out.println("OPPONENT IP IS : " + IP);
                Socket tcpsocketClient = new Socket(IP, iport); // starting a tcp connection to the opponent
                if (tcpsocketClient.isConnected())
                    System.out.println("Connected");
                in = new BufferedReader(new InputStreamReader(tcpsocketClient.getInputStream()));
                out = new PrintWriter(tcpsocketClient.getOutputStream());



            } else {
                System.out.println("iam a server");
                System.out.println("Connected to TCP");

                in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
                out = new PrintWriter(connect.getOutputStream());

            }
        }
    }

    private void movePipes() {
        pipeDelay--;

        if (pipeDelay < 0) {
            pipeDelay = PIPE_DELAY;
            Pipe northPipe = null;
            Pipe southPipe = null;

            // Look for pipes off the screen
            for (Pipe pipe : pipes) {
                if (pipe.x - pipe.width < 0) {
                    if (northPipe == null) {
                        northPipe = pipe;
                    } else if (southPipe == null) {
                        southPipe = pipe;
                        break;
                    }
                }
            }

            if (northPipe == null) {
                Pipe pipe = new Pipe("north");
                pipes.add(pipe);
                northPipe = pipe;
            } else {
                northPipe.reset();
            }

            if (southPipe == null) {
                Pipe pipe = new Pipe("south");
                pipes.add(pipe);
                southPipe = pipe;
            } else {
                southPipe.reset();
            }

            northPipe.y = southPipe.y + southPipe.height + 175;
        }

        for (Pipe pipe : pipes) {
            pipe.update();
        }
    }

    private void checkForCollisions() throws IOException, InterruptedException {


        for (Pipe pipe : pipes) {

            if (pipe.collides(bird.x, bird.y, bird.width, bird.height)) {  // if i died
                gameover = true;
                bird.dead = true;
                out.println("gameover");    // sent the opponent that i died
                out.flush();
                line=in.readLine();
                while (!line.equalsIgnoreCase("gameover")){   // while the opponent didn't send that he died keep reading
                    line=in.readLine();
                    if (!line.equalsIgnoreCase("gameover")) {
                        oscore = line;
                        out.println("gameover");    // sent the opponent that i died
                        out.flush();
                    }
                }
                ogameover=true;

               /* if(!line.equalsIgnoreCase("gameover")){      // this to display the lost algorithm
                    // if i died then i will check if the opponent is alive or dead if he is alive then for sure i lost
                    ogameover=true; // to make the if condition in gamepanel true  // not a good practice
                    lost=true;  // that i lost to display lost on the screen
                }*/

            } else if (pipe.x == bird.x && pipe.orientation.equalsIgnoreCase("south")) { // i am alive and i passed a pipe
                score++; // increment my score


            }
        }

        // Ground + Bird collision
        if (bird.y + bird.height > App.HEIGHT - 80) {   // same algorithm of the first if // that i died
            gameover = true;
            bird.y = App.HEIGHT - 80 - bird.height;

            out.println("gameover");
            out.flush();
            line=in.readLine();
            while (!line.equalsIgnoreCase("gameover")){   // while the opponent didn't send that he died keep reading
                line=in.readLine();
                if (!line.equalsIgnoreCase("gameover")) {
                    oscore = line;
                    out.println("gameover");    // sent the opponent that i died
                    out.flush();
                }
            }
            ogameover=true;
           /* if(!line.equalsIgnoreCase("gameover")){
                ogameover=true;
                lost=true;
            }*/



        }
    }
}
