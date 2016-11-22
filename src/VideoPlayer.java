import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import sun.audio.*;

public class VideoPlayer extends JPanel implements ActionListener {

	Timer timer;
	public static final int WIDTH = 480;
	public static final int HEIGHT = 270;
	
	BufferedImage img;
	File video;
	InputStream videoStream;
	JLabel frame;
	
	File audio;
	InputStream audioStream;
	
	public static final int FRAMERATE = 30;
	
	public VideoPlayer() {
		int period = 1000 / FRAMERATE; //To nearest int. 30 fps ~= 33 ms per frame = 29.97 fps
		
		timer = new Timer(33, this);
		img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		video = new File("dataset/Videos/data_test1.rgb");
		audio = new File("dataset/Videos/data_test1.wav");
		
		try {
			videoStream = new FileInputStream(video);
			audioStream = new FileInputStream(audio);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		frame = new JLabel(new ImageIcon(img));

		final PlaySound p = new PlaySound(audioStream);
		
		Thread t = new Thread(){public void run() {
			try {
				p.play();
			} catch (PlayWaveException e) {
				e.printStackTrace();
			}
		}};
		
		
		this.add(frame);
		t.start();
		timer.start();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			byte[] bytes = new byte[WIDTH*HEIGHT*3];
	
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}
	
	
			int ind = 0;
			for(int y = 0; y < HEIGHT; y++){
	
				for(int x = 0; x < WIDTH; x++){
					byte r = bytes[ind];
					byte g = bytes[ind+HEIGHT*WIDTH];
					byte b = bytes[ind+HEIGHT*WIDTH*2]; 
	
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x,y,pix);
					ind++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		repaint();
	}
	
}
