import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

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
	public int tt = 0;
	public double prevFrameEntropy = 0, prevR = 0, prevG = 0, prevB = 0;
	public double prevDifY = 0, prevDifR, prevDifG, prevDifB;

	JButton button;

	AudioInputStream stream;
	AudioFormat format;
	DataLine.Info info;
	Clip clip;
	PrintWriter out;

	public static final int FRAMERATE = 30;

	public VideoPlayer() {
		int period = 1000 / FRAMERATE; //To nearest int. 30 fps ~= 33 ms per frame = 29.97 fps

		
		try {
			out = new PrintWriter("output.txt", "UTF-8");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		timer = new Timer(3, this);
		img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		video = new File("dataset2/Videos/data_test2.rgb");
		audio = new File("dataset/Videos/data_test1.wav");

		try {
			videoStream = new FileInputStream(video);
			audioStream = new FileInputStream(audio);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		frame = new JLabel(new ImageIcon(img));

		final PlaySound p = new PlaySound(audioStream);
		button = new JButton("Stop");

		Thread t = new Thread(){public void run() {
			try {
				p.play();
			} catch (PlayWaveException e) {
				e.printStackTrace();
			}
		}};


		this.add(frame);
		this.add(button);
		button.addActionListener(this);
		//t.start();
		try {
			stream = AudioSystem.getAudioInputStream(audio);
			format = stream.getFormat();
			info = new DataLine.Info(Clip.class, format);
			clip = (Clip)AudioSystem.getLine(info);
			//stream.skip((long)(70 * format.getFrameSize() * format.getFrameRate()));
			clip.open(stream);
			//clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		timer.start();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == button) {
			try {
				//stream.skip((long)(10 * format.getFrameSize() * format.getFrameRate()));
				clip.setFramePosition(clip.getFramePosition() + (int)(10 * format.getFrameRate()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				byte[] bytes = new byte[WIDTH*HEIGHT*3];
		
				int offset = 0;
				int numRead = 0;
				while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
					offset += numRead;
				}
		        
		        double h=0;
		        double sum=0;
		        double sumr=0;
		        double sumg=0;
		        double sumb=0;
				int ind = 0;
				int[] YSpace = new int[256];
				int[] RSpace = new int[256];
				int[] GSpace = new int[256];
				int[] BSpace = new int[256];
				
				for(int y = 0; y < HEIGHT; y++){
		
					for(int x = 0; x < WIDTH; x++){
						int r = bytes[ind];
						int g = bytes[ind+HEIGHT*WIDTH];
						int b = bytes[ind+HEIGHT*WIDTH*2]; 
						

						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						img.setRGB(x,y,pix);
						
						int Y = (int)(0.299*r+0.587*g+0.114*b);
		                //calculate the entropy for the frame
				        //System.out.print("Y value: "+Y+"    ");
				        /*if(Y<=0)
				        {
				        	h=0;
				        }
				        else{
				        	 h=(double)(Y*Math.log10(Y));
				        }*/
				        if (Y <= 0) {
				        	Y = 0;
				        }
				        else if (Y >= 255){
				        	Y = 255;
				        }
				        if (r <= 0) {
				        	r = 0;
				        }
				        else if (r >= 255){
				        	r = 255;
				        }
				        if (g <= 0) {
				        	g = 0;
				        }
				        else if (g >= 255){
				        	g = 255;
				        }
				        if (b <= 0) {
				        	b = 0;
				        }
				        else if (b >= 255){
				        	b = 255;
				        }
				        
				        ++YSpace[Y];
				        ++RSpace[r];
				        ++GSpace[g];
				        ++BSpace[b];
				       
				        
				        //System.out.println(+h);
				        //sum=sum+h;
						ind++;
						
					}
				}
				
				for (int i : YSpace) {
					if (i != 0) {
						double prob = i*1.0 / (WIDTH*HEIGHT);
						sum += prob * Math.log(i) / Math.log(2);
					}
				}
				for (int i : RSpace) {
					if (i != 0) {
						double prob = i*1.0 / (WIDTH*HEIGHT);
						sumr += prob * Math.log(i) / Math.log(2);
					}
				}
				for (int i : GSpace) {
					if (i != 0) {
						double prob = i*1.0 / (WIDTH*HEIGHT);
						sumg += prob * Math.log(i) / Math.log(2);
					}
				}
				for (int i : BSpace) {
					if (i != 0) {
						double prob = i*1.0 / (WIDTH*HEIGHT);
						sumb += prob * Math.log(i) / Math.log(2);
					}
				}
				
				
				//out.println("Frame : "+ (++tt) + " "+sum);
				out.println(++tt + "," + sum + " | " + sumr + " | " + sumg + " | " + sumb);
				//tt++;
				// Print Entropy Difference
				/*if (Math.abs(prevFrameEntropy - sum) > .1) {
					System.out.println(tt);
					repaint();
				}*/
				
				double difY = Math.abs(prevFrameEntropy - sum);
				double difR = Math.abs(prevR - sumr);
				double difG = Math.abs(prevG - sumg);
				double difB = Math.abs(prevB - sumb);
				
				/*if (difY > 0.7) {
					if (prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 5) {
						System.out.println(tt);
						repaint();
					}
				}*/
				
				if (difY > 0.4 || (difR > 0.35 || difG > 0.35 || difB > 0.35)) { 
					if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 100) || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 100) || 
							(prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 100) || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 100)) {
						System.out.println(tt);
						repaint();
					}
				}if (difY > 0.5 || (difR > 0.5 || difG > 0.5 || difB > 0.5)) { 
					if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 50) || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 50) || 
							(prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 50) || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 50)) {
						System.out.println(tt);
						repaint();
					}
				}
				if (difY > 0.7 || (difR > 0.7 || difG > 0.7 || difB > 0.7)) {
					if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 10) || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 10) || 
							(prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 10) || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 10)) {
						System.out.println(tt);
						repaint();
					}
				}
				//out.println("Difference in entropy from previous frame : " + Math.abs(prevFrameEntropy-sum));
				prevFrameEntropy=sum;
				prevDifY = difY;
				prevDifR = difR;
				prevDifG = difG;
				prevDifB = difB;
				prevR = sumr;
				prevG = sumg;
				prevB = sumb;

				h=0;
				sum=0;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
