import javax.swing.JFrame;


public class Player extends JFrame {

	public Player() {
		super("Player");
		this.setSize(480, 270);
		
		VideoPlayer p = new VideoPlayer();
		this.add(p);
		this.pack();
		
		//AudioPlayer a = new AudioPlayer();
		//this.add(a);
		
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static void main(String[] args) {
		Player player = new Player();
	}

}
