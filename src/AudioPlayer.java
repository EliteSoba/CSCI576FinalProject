import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
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
	
	public static void main(String args[]) throws Exception{
		AudioInputStream stream;
		AudioFormat format;
		File audio;
		audio = new File("dataset2/Videos/data_test2.wav");
		
		File out = new File("testout.wav");
		AudioInputStream cut;
		
		stream = AudioSystem.getAudioInputStream(audio);
		format = stream.getFormat();
		double framerate = format.getFrameRate();
		int framesize = format.getFrameSize();
		System.out.println("Framerate: " + framerate + ", Channels: " + format.getChannels() + ", Framesize: " + framesize + ", BigEndian?: " + format.isBigEndian());
		
		byte[] buffer = new byte[framesize];
		int f = 0;
		int x, y;
		double prev = 0;
		int p = 0;
		int[] keyframes = {1, 14, 70, 118, 295, 451, 1850, 2014, 3019, 4009,
				5549, 6001, 6057, 6083, 6119, 6198, 6233, 6388, 6412, 6451, 6589, 7212, 7548, 7550, 7940, 8419, 9001};
		double[] avgs = new double[keyframes.length];
		double values = 0;
		int sign = 1, swaps = 0;
		while (stream.read(buffer, 0, buffer.length) > 0) {
			x = (buffer[1]) << 8;
			y = (buffer[0]);
			double xy = (double)(x | y);
			
			
			//System.out.println(Arrays.toString(buffer));
			/*if (Math.abs(xy - prev) > Short.MAX_VALUE / 2) {
				System.out.println(f);
				System.out.println((xy - prev) / Short.MAX_VALUE);
			}*/
			int curframe = (int)(f * 30 / framerate)+1;
			if (curframe >= keyframes[p]) {
				if (p != 0)
				avgs[p] /= keyframes[p]-keyframes[p-1];
				++p;
			}
			
			values += Math.abs(xy);
			++f;
			if (f % (framerate) == 0) {
				System.out.println(values / (framerate) + " | " + (swaps));
				
				swaps = 0;
				values = 0;
			}
			
			if (sign*xy < 0) {
				sign *= -1;
				++swaps;
			}
			avgs[p] += Math.abs(xy/Short.MAX_VALUE);
			prev = xy;
			
		}
		stream.close();
		stream = AudioSystem.getAudioInputStream(audio);
		ArrayList<AudioInputStream> streams = new ArrayList<AudioInputStream>();
		//AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out);
		int pos = 0;
		boolean aaa = true;
		int length = 0;
		//buffer holds a frame of audio
		byte buff[] = new byte[(int)(framerate*framesize/30)];
		FileOutputStream fout = new FileOutputStream("audio.temp");
		

		int read = 0;
		int offset = 0;
		for (int curFrame = 1; (read = stream.read(buff)) > 0; ++curFrame) {
			//If we don't read a full frame for some reason. I hope this doesn't happen because it's hard to test
			//if my logic here is right
			if (read != framesize) {
				offset = read;
				while (offset < framesize && (read = stream.read(buff, offset, framesize - offset)) >= 0) {
					offset += read;
				}
			}
			
			if (curFrame <= 450) {
				fout.write(buff);
				length += (int)(framerate/30);
			}
			else if (curFrame >= 6001 && curFrame <= 6450) {
				fout.write(buff);
				length += (int)(framerate/30);
			}
		}
		fout.close();
		FileInputStream fin = new FileInputStream("audio.temp");
		AudioInputStream as = new AudioInputStream(fin, format, length);
		/*for (int i = 0; i < keyframes.length-1; i++) {
			if(i <= 5 || i >= 11 && i <= 18) {
				stream.skip((int)(framerate / 30 * keyframes[i]) - pos);
				pos = (int)(framerate / 30 * keyframes[i]);
				streams.add(new AudioInputStream(stream, format, (int)((keyframes[i+1] - keyframes[i])*framerate/30)));
				length += (int)((keyframes[i+1] - keyframes[i])*framerate/30);
				if (aaa) {
				//AudioSystem.write(cut, AudioFileFormat.Type.WAVE, o);
				aaa = false;
				}
				//return;
			}
		}*/
		
		
		//cut = new AudioInputStream(s, format, length);
		AudioSystem.write(as, AudioFileFormat.Type.WAVE, out);
		
		System.out.println(f);
		System.out.println(Arrays.toString(avgs));
		for (int i = 1; i < avgs.length; i++) {
			if (Math.abs(avgs[i] - avgs[i-1]) > 2000) {
				System.out.println(i);
			}
		}
	}

}
