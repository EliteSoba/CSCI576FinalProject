import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class MyPart2 {
	
	/**
	 * Category of a shot. I'd use a bit array if that wasn't so silly
	 * @author Tobias
	 */
	public static enum Category {
		UNKNOWN,
		SPAM,
		HAM,
		EITHER;
	}
	
	/**
	 * A single shot. Contains timestamps, category, and other info
	 * @author Tobias
	 */
	public static class Shot {
		public int start, end;
		public Category cat;
		double avgAmp;
		//sampleCount is unnecessary because I can just use length()/30*audio framerate
		int sampleCount;
		//Really bootleg quick way to measure frequencies
		double bootleg;
		public Shot(int s, int e) {
			start = s;
			end = e;
			cat = Category.UNKNOWN;
			avgAmp = 0;
			sampleCount = 0;
		}
		
		public int length() {
			return end - start;
		}
		
		public void addSample(double s) {
			avgAmp += s;
			++sampleCount;
		}
		
		public void avgSample() {
			if (sampleCount != 0) {
				avgAmp /= sampleCount;
			}
		}
	}

	public static final int WIDTH = 480;
	public static final int HEIGHT = 270;
	
	/**
	 * Analyzes entropies in a video and divides it into shots
	 * It will not catch every shot in an ad and will sometimes
	 * give extra shots in non-ads but is generally fairly accurate
	 * @param videopath The video filename
	 * @return The list of shots
	 * @throws IOException If IO goes wrong
	 */
	public static Shot[] analyzeVideo(String videopath) throws IOException {
		//Analyze Video
		
		//Turn file into path
		File f = new File(videopath);
		InputStream videoStream = new FileInputStream(f);
		
		//Previous values for calculating differences
		double prevEntY = 0, prevEntR = 0, prevEntG = 0, prevEntB = 0;
		double prevDifY = 0, prevDifR = 0, prevDifG = 0, prevDifB = 0;
		
		//Get the shot transition frames; shots are between these frames
		ArrayList<Integer> borders = new ArrayList<Integer>();

		//Some initial data for the for loop
		int numRead = 0;
		int frame = 1;
		//I just don't want to allocate more space each time
		byte bytes[] = new byte[3*WIDTH*HEIGHT];
		
		//Count frames each time and go until end of file (numRead == -1)
		//Note: I start on frame 1, not frame 0
		for (frame = 1; numRead != -1; ++frame) {
			
			//tbh idk why it's done this way, but I guess it's to ensure we read to the full buffer
			int offset = 0;
			while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}
			
			//Entropies
	        double entY=0;
	        double entR=0;
	        double sumG=0;
	        double sumB=0;
	        //Space of each component to calculate frequencies
			int[] YSpace = new int[256];
			int[] RSpace = new int[256];
			int[] GSpace = new int[256];
			int[] BSpace = new int[256];

			//tbh I probably don't need ind and can probably use (WIDTH*y + x)
			int ind = 0;
			for(int y = 0; y < HEIGHT; y++){
				for(int x = 0; x < WIDTH; x++){
					//Read RGB from buffer for frame
					int r = bytes[ind];
					int g = bytes[ind+HEIGHT*WIDTH];
					int b = bytes[ind+HEIGHT*WIDTH*2]; 
					
					//Calculate Y from rgb
					int Y = (int)(0.299*r+0.587*g+0.114*b);

					//Normalize values
					Y = Math.max(0, Y);
					Y = Math.min(255, Y);
					r = Math.max(0, r);
					r = Math.min(255, r);
					g = Math.max(0, g);
					g = Math.min(255, g);
					b = Math.max(0, b);
					b = Math.min(255, b);
			        
			        //Increase frequency for those values in their respective spaces
			        ++YSpace[Y];
			        ++RSpace[r];
			        ++GSpace[g];
			        ++BSpace[b];
			       
					ind++;
				}
			}
			
			//Calculate entropies for each space
			for (int i : YSpace) {
				if (i != 0) {
					double prob = i*1.0 / (WIDTH*HEIGHT);
					entY += prob * Math.log(i) / Math.log(2);
				}
			}
			for (int i : RSpace) {
				if (i != 0) {
					double prob = i*1.0 / (WIDTH*HEIGHT);
					entR += prob * Math.log(i) / Math.log(2);
				}
			}
			for (int i : GSpace) {
				if (i != 0) {
					double prob = i*1.0 / (WIDTH*HEIGHT);
					sumG += prob * Math.log(i) / Math.log(2);
				}
			}
			for (int i : BSpace) {
				if (i != 0) {
					double prob = i*1.0 / (WIDTH*HEIGHT);
					sumB += prob * Math.log(i) / Math.log(2);
				}
			}
			
			//Calculate the change in entropies with the previous values
			double difY = Math.abs(prevEntY - entY);
			double difR = Math.abs(prevEntR - entR);
			double difG = Math.abs(prevEntG - sumG);
			double difB = Math.abs(prevEntB - sumB);
			
			//I have this boolean here to ensure values don't get added multiple times
			//because I have embedded if statements that make else ifs difficult
			boolean checked = true;
			
			//If the change in entropy is above a certain small threshold
			if (difY > 0.4 || (difR > 0.35 || difG > 0.35 || difB > 0.35)) {
				//Then if this change is significantly bigger than previous changes
				if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 100) || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 100) || 
						(prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 100) || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 100)) {
					//Add to shot borders list
					checked = false;
					borders.add(frame);
				}
			}
			//If the change in entropy is above a certain medium threshold
			if (difY > 0.5 || (difR > 0.5 || difG > 0.5 || difB > 0.5)) {
				//Then if this change is moderately bigger than previous changes
				if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 50) || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 50) || 
						(prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 50) || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 50)) {
					//If we haven't already added this value
					if (checked) {
						checked = false;
						borders.add(frame);
					}
				}
			}
			//If the change in entropy is above a certain large threshold
			if (difY > 0.7 || (difR > 0.7 || difG > 0.7 || difB > 0.7)) {
				//Then if this change is slightly bigger than previous changes
				if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 10) || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 10) || 
						(prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 10) || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 10)) {
					//If we haven't already added this value
					if (checked) {
						borders.add(frame);
					}
				}
			}
			
			//Set previous values
			prevEntY = entY;
			prevDifY = difY;
			prevDifR = difR;
			prevDifG = difG;
			prevDifB = difB;
			prevEntR = entR;
			prevEntG = sumG;
			prevEntB = sumB;
		}
		
		//Close the input stream like a responsible adult
		videoStream.close();
		
		//Last shot ends when video ends
		borders.add(frame);

		//N borders means N-1 shots
		Shot shots[] = new Shot[borders.size()-1];
		
		//Convert borders to shots
		int start = borders.get(0);
		for (int i = 1; i < borders.size(); ++i) {
			int end = borders.get(i);
			shots[(i-1)] = new Shot(start, end-1);
			
			start = end;
		}
		
		for (Shot s: shots) {
			if (s.length() < 120) {
				s.cat = Category.SPAM;
			}
			else if (s.length() > 300) {
				s.cat = Category.HAM;
			}
			else {
				s.cat = Category.EITHER;
			}
		}
		
		return shots;
	}
	
	/**
	 * Analyzes the audio stream that is matched with the video stream to get average amplitudes
	 * of shots for better distinguishing.
	 * @param audiopath The audio file
	 * @param shots NOTE: IT EDITS THIS PARAMETER BECAUSE I AM NOT A GOOD PERSON
	 * @return The passed in shots array
	 * @throws IOException If an IO exception occurs
	 * @throws UnsupportedAudioFileException If we get a bad audio file
	 */
	public static Shot[] analyzeAudio(String audiopath, Shot[] shots) throws UnsupportedAudioFileException, IOException {
		//Analyze Audio
		File audio = new File(audiopath);
		
		//Get info about audio file
		AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
		AudioFormat format = stream.getFormat();
		double framerate = format.getFrameRate();
		int framesize = format.getFrameSize();
		boolean bigend = format.isBigEndian();
		
		//Buffer to store samples
		byte[] buffer = new byte[framesize];
		
		int read = 0;
		int offset = 0;
		int x, y;
		int shotOffset = 0;
		
		int sign = 1;
		int signCount = 0;
		for (int curFrame = 0; (read = stream.read(buffer)) > 0; ++curFrame) {
			//If we don't read a full frame for some reason. I hope this doesn't happen because it's hard to test
			//if my logic here is right
			if (read != framesize) {
				offset = read;
				while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
					offset += read;
				}
			}
			
			if (!bigend) {
				x = buffer[1] << 8;
				y = buffer[0];
			}
			else {
				x = buffer[0] << 8;
				y = buffer[1];
			}
			double xy = x | y;
			
			//sample frame 0 maps to frame 1, sample frame 48000 (framerate) maps to frame 31
			int videoFrame = (int)((curFrame / framerate) * 30) + 1;
			
			//To be honest, shouldn't even be too big a deal if I'm off by a few sample frames
			if (videoFrame > shots[shotOffset].end) {
				
				shots[shotOffset].bootleg = signCount * 1.0 / shots[shotOffset].length();
				signCount = 0;
				sign = 1;
				++shotOffset;
			}
			
			shots[shotOffset].addSample(Math.abs(xy / Short.MAX_VALUE));
			if (sign * xy < 0) {
				sign = (xy < 0 ? -1 : 1);
				++signCount;
			}
		}

		shots[shotOffset].bootleg = signCount * 1.0 / shots[shotOffset].length();
		
		for (Shot s : shots) {
			s.avgSample();
		}
		
		return shots;
	}
	
	/**
	 * Cuts out the ad frames in a video
	 * @param videoIn The input video filename
	 * @param videoOut The output video filename
	 * @param shots The list of shots
	 * @throws IOException If IO goes wrong
	 */
	public static void cutVideo(String videoIn, String videoOut, Shot[] shots) throws IOException {
		//Cut Video
		
		//Turn file into path
		File f = new File(videoIn);
		InputStream videoStream = new FileInputStream(f);
		
		FileOutputStream outStream = new FileOutputStream(videoOut);
		
		//Some initial data for the for loop
		int numRead = 0;
		int frame = 1;
		int curShot = 0;
		//I just don't want to allocate more space each time
		byte bytes[] = new byte[3*WIDTH*HEIGHT];
		
		//Count frames each time and go until end of file (numRead == -1)
		//Note: I start on frame 1, not frame 0
		for (frame = 1; numRead != -1; ++frame) {
			
			//tbh idk why it's done this way, but I guess it's to ensure we read to the full buffer
			int offset = 0;
			while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}

			if (shots[curShot].end < frame) {
				++curShot;
				//If we somehow go past the end
				if (curShot == shots.length) {
					break;
				}
			}
			
			if (shots[curShot].cat == Category.HAM) {
				outStream.write(bytes);
			}
		}
		
		//Close the streams like a responsible adult
		videoStream.close();
		outStream.close();
	}
	
	/**
	 * Cuts out ad audio
	 * @param audioIn The input audio filename
	 * @param audioOut The output audio filename
	 * @param shots The list of shots
	 * @throws UnsupportedAudioFileException If we get a bad audio file
	 * @throws IOException If an IO exception occurs
	 */
	public static void cutAudio(String audioIn, String audioOut, Shot[] shots) throws UnsupportedAudioFileException, IOException {
		//Cut Audio
		File audio = new File(audioIn);
		
		//Get info about audio file
		AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
		AudioFormat format = stream.getFormat();
		double framerate = format.getFrameRate();
		int framesize = format.getFrameSize();
		
		//buffer holds a frame of audio
		byte buffer[] = new byte[(int)(framerate*framesize/30)];
		//Temporary audio out file
		FileOutputStream fout = new FileOutputStream(audioOut + ".temp");
		
		//Init variables for loop
		int read = 0;
		int offset = 0;
		int curShot = 0;
		int length = 0;
		for (int frame = 1; (read = stream.read(buffer)) > 0; ++frame) {
			//If we don't read a full frame for some reason. I hope this doesn't happen because it's hard to test
			//if my logic here is right
			if (read != framesize) {
				offset = read;
				while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
					offset += read;
				}
			}

			if (shots[curShot].end < frame) {
				++curShot;
				//If we somehow go past the end
				if (curShot == shots.length) {
					break;
				}
			}

			if (shots[curShot].cat == Category.HAM) {
				fout.write(buffer);
				length += (int)(framerate/30);
			}
		}
		fout.close();
		
		//Take the data in the temp file and write it to an actual file
		File out = new File(audioOut);
		FileInputStream fin = new FileInputStream(audioOut + ".temp");
		AudioInputStream as = new AudioInputStream(fin, format, length);
		AudioSystem.write(as, AudioFileFormat.Type.WAVE, out);
		fin.close();
		as.close();
	}
	
	public static void main(String[] args) throws Exception {
		//Get input file name
		String videopath = "dataset/Videos/data_test1.rgb";
		String audiopath = "dataset/Videos/data_test1.wav";
		String videoout = "video.rgb";
		String audioout = "audio.wav";
		if (args.length > 0) {
			videopath = args[0];
		}
		if (args.length > 1) {
			audiopath = args[1];
		}
		if (args.length > 2) {
			videoout = args[2];
		}
		if (args.length > 3) {
			audioout = args[3];
		}
		
		//Get shots from video by analyzing video and audio
		Shot[] shots = analyzeVideo(videopath);
		analyzeAudio(audiopath, shots);
		
		//Pass 1: look for isolated shots and join labels with neighbors
		for (int i = 0; i < shots.length; ++i) {
			Set<Category> neighbors = new HashSet<Category>();
			if (i != 0) {
				neighbors.add(shots[i-1].cat);
			}
			if (i != shots.length - 1) {

				neighbors.add(shots[i+1].cat);
			}
			
			//Decide eithers based on neighbors. Sometimes, we might mislabel ham as spam, so check that
			//We should never mislabel spam as ham.
			if (shots[i].cat == Category.EITHER || shots[i].cat == Category.SPAM) {
				//If both are ham
				if (neighbors.contains(Category.HAM) && neighbors.size() == 1) {
					shots[i].cat = Category.HAM;
				}
				//If both are spam
				else if (neighbors.contains(Category.SPAM) && neighbors.size() == 1) {
					shots[i].cat = Category.SPAM;
				}
			}
			
		}
		
		//Pass 2: Compare average amplitudes with neighbors. Choose closest neighbor
		for (int i = 0; i < shots.length; ++i) {
			if (i != 0 && shots[i].cat == Category.EITHER) {
				if (Math.abs(shots[i].avgAmp - shots[i-1].avgAmp) <= 0.01) {
					shots[i].cat = shots[i-1].cat;
				}
			}

			if (i != shots.length-1 && shots[i].cat == Category.EITHER) {
				if (Math.abs(shots[i].avgAmp - shots[i+1].avgAmp) <= 0.01) {
					shots[i].cat = shots[i+1].cat;
				}
			}
		}
		
		//Pass 3: Compare my bootleg frequencies.
		for (int i = 1; i < shots.length - 1; ++i) {
			if (shots[i].cat == Category.EITHER) {
				if (Math.abs(shots[i].bootleg - shots[i-1].bootleg) < Math.abs(shots[i].bootleg - shots[i+1].bootleg)) {
					shots[i].cat = shots[i-1].cat;
				}
				else {
					shots[i].cat = shots[i+1].cat;
				}
			}
		}
		
		//Pass 4: Just choose closest neighbor, prioritizing after arbitrarily
		//Really desperation mode here, probably will never reach
		for (int i = 0; i < shots.length; ++i) {
			if (shots[i].cat == Category.EITHER) {
				for (int j = 1; j <= shots.length; ++j) {
					if (i - j >= 0) {
						if (shots[j].cat != Category.EITHER) {
							shots[i].cat = shots[j].cat;
							break;
						}
					}
					if (i + j < shots.length) {
						if (shots[j].cat != Category.EITHER) {
							shots[i].cat = shots[j].cat;
							break;
						}
					}
				}
			}
		}
		
		/*
		for (Shot s : shots) {
			System.out.println("Shot: " + s.start + "-" + s.end + ", " + s.cat.name());
		}
		*/
		
		//Cut the video and audio
		cutVideo(videopath, videoout, shots);
		cutAudio(audiopath, audioout, shots);
	}

}