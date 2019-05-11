import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;

public class GamePanel extends JPanel implements Runnable {

    private Game game;


    public GamePanel() {

        game = new Game();


        new Thread(this).start();
    }

    public void update() throws IOException, InterruptedException {

        game.update();
        repaint();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2D = (Graphics2D) g;
        for (Render r : game.getRenders())
            if (r.transform != null)
                g2D.drawImage(r.image, r.transform, null);
            else
                g.drawImage(r.image, r.x, r.y, null);


        g2D.setColor(Color.BLACK);

        if (!game.started) {
            g2D.setFont(new Font("TimesRoman", Font.PLAIN, 20));
            g2D.drawString("Press SPACE to start", 150, 240);
        } else {

            g2D.setFont(new Font("TimesRoman", Font.PLAIN, 24));
            g2D.drawString(Integer.toString(game.score), 10, 465);
            g2D.drawString(game.oscore, 10, 240);


        }




        if (game.gameover && game.ogameover) {  // if i died and the opponent died


            if(game.score > Integer.parseInt(game.oscore)) {  // if my score is bigger then i won

                g2D.setFont(new Font("TimesRoman", Font.PLAIN, 20));
                g2D.drawString("Winner ", 150, 240);

            }
            else if (game.score < Integer.parseInt(game.oscore)){    // if lost boolean is true then i lost  NOTE Lost is handled in CheckforCollison in game class

                g2D.setFont(new Font("TimesRoman", Font.PLAIN, 20));
                g2D.drawString("Loser", 150, 240);
            }
            else if (game.score == Integer.parseInt(game.oscore)){ // if equal score then tie

                g2D.setFont(new Font("TimesRoman", Font.PLAIN, 20));
                g2D.drawString("Tie", 150, 240);
            }

            /*try {
                game.tcpsocket.close();  // close tcp socket
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }
    }

    public void run() {
        try {

            while (true) {

                update();

                Thread.sleep(25);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
