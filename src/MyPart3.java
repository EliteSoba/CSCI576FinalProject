import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class MyPart3 {
	public static final int WIDTH = 480;
	public static final int HEIGHT = 270;

	public static int getRed(int color) {
		int red = color & 0x00ff0000;
		red >>= 16;
		return red;
	}
	
	public static int getGreen(int color) {
		int green = color & 0x0000ff00;
		green >>= 8;
		return green;
	}
	
	public static int getBlue(int color) {
		int blue = color & 0x000000ff;
		return blue;
	}

	
	/**
	 * Floods a pic with pink. Modifies the pic matrix
	 * @param pic The pic matrix of colors
	 * @param x X starting point
	 * @param y y starting point
	 * @param c The color key
	 */
	public static void iterativeFloodFill(int[][] pic, int x, int y, int c) {
		Queue<Point> queue = new LinkedList<Point>();
		queue.add(new Point(x, y));
		
		while (!queue.isEmpty()) {
			Point p = queue.poll();
			int color = pic[p.x][p.y];
			
			if (color == c) {
				pic[p.x][p.y] = 0xffff00ff;
				
				if (p.x != 0) {
					queue.add(new Point(p.x-1, p.y));
				}
				if (p.y != 0) {
					queue.add(new Point(p.x, p.y-1));
				}
				if (p.x != WIDTH-1) {
					queue.add(new Point(p.x+1, p.y));
				}
				if (p.y != HEIGHT-1) {
					queue.add(new Point(p.x, p.y+1));
				}
			}
		}
	}
	
	/**
	 * Converts a rgb file into a 2D int array of colors.
	 * Assumes image to be of 480x720 because no real reason to assume otherwise
	 * @param filename The file to read
	 * @return A 2d array of pixels 
	 * @throws IOException
	 */
	public static int[][] rgbToByteMatrix(String filename) throws IOException {
		FileInputStream f = new FileInputStream(filename);
		
		int img[][] = new int[WIDTH][HEIGHT];
		
		byte bytes[] = new byte[WIDTH*HEIGHT*3];
		
		int offset = 0, numRead = 0;
		while (offset < bytes.length && (numRead = f.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		
		int ind = 0;
		for (int y = 0; y < HEIGHT; ++y) {
			for (int x = 0; x < WIDTH; ++x) {
				int r = bytes[ind] & 0xff;
				int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
				int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;
				
				img[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				++ind;
			}
		}
		
		f.close();
		
		return img;
	}
	/**
	 * Special processing for the McDonalds picture because it's a bad rgb image
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static int[][] mcdToByteArray(String filename) throws IOException {
		FileInputStream f = new FileInputStream(filename);
		
		int img[][] = new int[WIDTH][HEIGHT];
		
		byte bytes[] = new byte[WIDTH*HEIGHT*3];
		
		int offset = 0, numRead = 0;
		while (offset < bytes.length && (numRead = f.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		
		int ind = 0;
		for (int y = 0; y < HEIGHT; ++y) {
			for (int x = 0; x < WIDTH; ++x) {
				int r = bytes[ind] & 0xff;
				int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
				int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;
				//Only want these yellows/oranges
				if (!(r > 150 && g > 150 && b < 128)) {
					r = 255;
					g = 0;
					b = 255;
				}
				
				img[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				++ind;
			}
		}
		
		f.close();
		
		return img;
	}
	
	/**
	 * Return an array of matrices of the logo images
	 * 0 is starbucks, 1 is subway, 2 is nfl, 3 is mcdonalds
	 * @return
	 * @throws IOException
	 */
	public static int[][][] readLogos() throws IOException{
		String starbucks_path = "dataset/Brand Images/starbucks_logo.rgb";
		String subway_path = "dataset/Brand Images/subway_logo.rgb";
		String nfl_path = "dataset2/Brand Images/nfl_logo.rgb";
		String mcd_path = "dataset2/Brand Images/Mcdonalds_logo.raw";
		
		int[][] stb = rgbToByteMatrix(starbucks_path);
		int[][] sbw = rgbToByteMatrix(subway_path);
		int[][] nfl = rgbToByteMatrix(nfl_path);
		int[][] mac = mcdToByteArray(mcd_path);
		
		//iterativeFloodFill(stb, 0, 0, stb[0][0]);
		iterativeFloodFill(sbw, 0, 0, sbw[0][0]);
		iterativeFloodFill(nfl, 0, 0, nfl[0][0]);
		
		int[][][] logos = {stb, sbw, nfl, mac};
		return logos;
	}
	
	/**
	 * Calcualte MSE between two frames. Ignores pink masked pixels in logo
	 * @param logo The logo to compare against
	 * @param frame The frame to compare against
	 * @return The MSE between the two
	 */
	public static double calcMSE(int[][] logo, int[][] frame) {
		double sum = 0;
		int h = Math.min(logo.length, frame.length);
		int w = Math.min(logo[0].length, frame[0].length);
		
		for (int i = 0; i < h; ++i) {
			for (int j = 0; j < w; ++j) {
				//Mask out the pink
				if (logo[i][j] != 0xffff00ff) {
					
					int rdif = getRed(logo[i][j]) - getRed(frame[i][j]);
					int gdif = getGreen(logo[i][j]) - getGreen(frame[i][j]);
					int bdif = getBlue(logo[i][j]) - getBlue(frame[i][j]);
					
					sum += rdif*rdif + gdif*gdif + bdif*bdif;
				}
			}
		}
		
		return sum / (h*w);
	}
	
	/**
	 * Downscales the logo and checks a subsection of the frame
	 * @param logo The logo
	 * @param frame The frame
	 * @param scale The scaling factor
	 * @param offX The x subframe offset
	 * @param offY The y subframe offset
	 * @return The MSE between the two
	 */
	public static double calcSubMSE(int[][] logo, int[][] frame, int scale, int offX, int offY) {
		double sum = 0;
		int h = logo.length / scale;
		int w = logo[0].length / scale;
		
		for (int i = 0; i < h; ++i) {
			for (int j = 0; j < w; ++j) {
				if (logo[i*scale][j*scale] != 0xffff00ff) {
					
					int rdif = getRed(logo[i*scale][j*scale]) - getRed(frame[i+offX*scale][j+offY*scale]);
					int gdif = getGreen(logo[i*scale][j*scale]) - getGreen(frame[i+offX*scale][j+offY*scale]);
					int bdif = getBlue(logo[i*scale][j*scale]) - getBlue(frame[i+offX*scale][j+offY*scale]);
					
					sum += rdif*rdif + gdif*gdif + bdif*bdif;
				}
			}
		}
		
		return sum / (h*w);
	}
	
	public static void analyzeVideo(String videopath, int[][][] logos) throws IOException {
		//Turn file into path
		File f = new File(videopath);
		InputStream videoStream = new FileInputStream(f);

		//FileOutputStream out = new FileOutputStream("out.txt");
		double min = Double.MAX_VALUE;
		int fmin = 0;
		int scalemin = 0, xmin = 0, ymin = 0;
		
		//Some initial data for the for loop
		int numRead = 0;
		int curFrame = 1;
		//I just don't want to allocate more space each time
		byte bytes[] = new byte[3*WIDTH*HEIGHT];
		int[][] frame = new int[WIDTH][HEIGHT];
		
		//Count frames each time and go until end of file (numRead == -1)
		//Note: I start on frame 1, not frame 0
		for (curFrame = 1; numRead != -1; ++curFrame) {
			
			//Ensure we read to the full buffer
			int offset = 0;
			while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}

			//tbh I probably don't need ind and can probably use (WIDTH*y + x)
			int ind = 0;
			for(int y = 0; y < HEIGHT; y++){
				for(int x = 0; x < WIDTH; x++){
					//Read RGB from buffer for frame
					int r = bytes[ind] & 0xff;
					int g = bytes[ind+HEIGHT*WIDTH] & 0xff;
					int b = bytes[ind+HEIGHT*WIDTH*2] & 0xff;
					r = Math.max(0, r);
					r = Math.min(255, r);
					g = Math.max(0, g);
					g = Math.min(255, g);
					b = Math.max(0, b);
					b = Math.min(255, b);

					frame[x][y] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					
					ind++;
				}
			}
			if (curFrame >= 5551 && curFrame <= 6000 || curFrame >= 2401 && curFrame <= 2850) {
				continue;
			}
			
			//String o = curFrame + ": " + mse + "\n";
			//out.write(o.getBytes());
			
			for (int scale = 1; scale <= 5; ++scale) {
				
				for (int x = 0; x < scale; ++x) {
					for (int y = 0; y < scale; ++y) {
						double mse = calcSubMSE(logos[0], frame, scale, x, y);
						if (min > mse) {
							min = mse;
							fmin = curFrame;
							scalemin = scale;
							xmin = x;
							ymin = y;
						}
					}
				}
				
			}
		}
		//out.close();
		System.out.println(fmin + ": " + min + ". " + scalemin + " | " + xmin + ", " + ymin);
		//Close the input stream like a responsible adult
		videoStream.close();
	}
	
	public static void testImage(int[][] image) {
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < WIDTH; ++i) {
			for (int j = 0; j < HEIGHT; ++j) {
				img.setRGB(i, j, image[i][j]);
			}
		}
		
		JLabel j = new JLabel(new ImageIcon(img));
		JFrame f = new JFrame("aaa");
		f.add(j);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
	}
	public static void main(String[] args) throws IOException {
		int[][][] logos = readLogos();
		analyzeVideo("dataset/Videos/data_test1.rgb", logos);
		//testImage(logos[0]);
	}

}
