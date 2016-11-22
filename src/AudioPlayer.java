import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;
import javax.swing.JPanel;
import javax.swing.Timer;


public class AudioPlayer extends JPanel implements ActionListener {

	File audio;
	InputStream audioStream;
	AudioInputStream audioInputStream;
	SourceDataLine dataLine;
	
	Timer timer;
	int pos;
	
	public AudioPlayer() {
		audio = new File("dataset/Videos/data_test1.wav");
		pos = 0;
		
		try {
			audioStream = new FileInputStream(audio);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		audioInputStream = null;
		try {
		    //audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
			
			//add buffer for mark/reset support, modified by Jian
			InputStream bufferedIn = new BufferedInputStream(audioStream);
		    audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
			
		} catch (UnsupportedAudioFileException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Obtain the information about the AudioInputStream
		AudioFormat audioFormat = audioInputStream.getFormat();
		Info info = new Info(SourceDataLine.class, audioFormat);

		// opens the audio channel
		try {
		    dataLine = (SourceDataLine) AudioSystem.getLine(info);
		    dataLine.open(audioFormat, 524288);
		} catch (LineUnavailableException e1) {
			e1.printStackTrace();
		}
		
		dataLine.start();
		
		timer = new Timer(33, this);
		timer.start();
	}
	
	public void actionPerformed(ActionEvent e) {
		int readBytes = 0;
		byte[] audioBuffer = new byte[25600];

		try {
			readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
			if (readBytes >= 0){
				pos += readBytes;
			    dataLine.write(audioBuffer, 0, readBytes);
			}
		} catch (IOException e1) {
		    e1.printStackTrace();
		} finally {
		    // plays what's left and and closes the audioChannel
		    dataLine.drain();
		}
	}

}
