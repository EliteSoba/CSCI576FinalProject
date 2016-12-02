import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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

	/**
	 * Serial ID to make Eclipse happy
	 */
	private static final long serialVersionUID = -8426080774234368297L;
	
	//Width and Height of frame
	public static final int WIDTH = 480;
	public static final int HEIGHT = 270;
	
	//Timer to allow video to update
	Timer timer;

	//AWT and Swing objects to help display video
	BufferedImage img;
	InputStream videoStream;
	JLabel frame;

	//Swing components to control video
	JButton play, pause, stop;

	//Audio info for generating audio playback
	File audio;
	AudioInputStream audioStream;
	AudioFormat format;
	DataLine.Info info;
	Clip clip;
	
	//Filenames of video and audio
	String videopath;
	String audiopath;

	//Current video frame
	int curFrame;

	//Framerate of video
	public static final int FRAMERATE = 30;

	public VideoPlayer(String videoPath, String audioPath) {
		//Get period from framerate to nearest int. 30 fps ~= 33 ms per frame
		int period = 1000 / FRAMERATE;

		//Remember filenames
		videopath = videoPath;
		audiopath = audioPath;
		
		//Timer updates every half frame so video will never be desynced by more than a half frame
		//Excluding off by one issues on my part that may or may not be present
		timer = new Timer(period/2, this);
		
		//Set up components to read video
		img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		try {
			videoStream = new FileInputStream(videopath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		//Swing Components to display video and controls
		frame = new JLabel(new ImageIcon(img));
		play = new JButton("Play");
		pause = new JButton("Pause");
		stop = new JButton("Stop");
		
		//Layout stuff to make it look pretty
		GridBagConstraints layout = new GridBagConstraints();
		this.setLayout(new GridBagLayout());
		
		layout.gridx = 0; layout.gridy = 0;
		layout.gridwidth = 3;
		this.add(frame, layout);
		
		layout.fill = GridBagConstraints.HORIZONTAL;
		layout.gridx = 0;
		layout.gridy = 1;
		layout.gridwidth = 1;
		layout.weightx = 1;
		this.add(play, layout);

		layout.gridx = 1;
		this.add(pause, layout);
		
		layout.gridx = 2;
		this.add(stop, layout);

		//ActionListeners to make buttons do things
		play.addActionListener(this);
		pause.addActionListener(this);
		stop.addActionListener(this);

		//Set up components to play audio
		audio = new File(audiopath);
		try {
			audioStream = AudioSystem.getAudioInputStream(audio);
			format = audioStream.getFormat();
			info = new DataLine.Info(Clip.class, format);
			clip = (Clip)AudioSystem.getLine(info);
			clip.open(audioStream);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

		//Start timer
		timer.start();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		//If pause button is pressed. Pause if playing.
		if (arg0.getSource() == pause) {
			//stream.skip((long)(10 * format.getFrameSize() * format.getFrameRate()));
			//clip.setFramePosition(clip.getFramePosition() + (int)(10 * format.getFrameRate()));
			if (clip.isActive()) {
				clip.stop();
				System.out.println("pause");
			}
		}
		//If play button is pressed. Play if paused/stopped
		else if (arg0.getSource() == play) {
			if (!clip.isActive()) {
				clip.start();
				System.out.println("play");
			}
		}
		//If stop button is pressed
		else if (arg0.getSource() == stop) {
			//Reset audio
			clip.stop();
			clip.close();
			
			try {
				audioStream = AudioSystem.getAudioInputStream(audio);
				format = audioStream.getFormat();
				info = new DataLine.Info(Clip.class, format);
				clip = (Clip)AudioSystem.getLine(info);
				clip.open(audioStream);
				
				//Reset video
				curFrame = 0;
				videoStream.close();
				videoStream = new FileInputStream(videopath);
				
				//Black out screen
				for(int y = 0; y < HEIGHT; y++){
					for(int x = 0; x < WIDTH; x++){
						img.setRGB(x,y,0);
					}
				}
				repaint();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//If this is a result of the timer firing
		else {
			//Don't bother doing anything if audio is stopped
			if (!clip.isActive()) {
				return;
			}
			try {
				//Generate byte array to hold frame
				byte[] bytes = new byte[WIDTH*HEIGHT*3];

				//Gets the video frame the audio is on
				int audioFrame = (int)(clip.getFramePosition() / format.getFrameRate() * FRAMERATE);

				//if audio is behind video, wait for audio to catch up
				if (audioFrame < curFrame) {
					return;
				}
				//If audio is ahead of video, keep going through frames until video catches up
				while (audioFrame > curFrame) {
					int offset = 0;
					int numRead = 0;
					while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
						offset += numRead;
					}

					//Update frame
					int ind = 0;
					for(int y = 0; y < HEIGHT; y++){
						for(int x = 0; x < WIDTH; x++){
							int r = bytes[ind];
							int g = bytes[ind+HEIGHT*WIDTH];
							int b = bytes[ind+HEIGHT*WIDTH*2]; 

							int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
							img.setRGB(x,y,pix);

							++ind;
						}
					}
					++curFrame;
				}
				//Actually paint the frame
				repaint();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

}
