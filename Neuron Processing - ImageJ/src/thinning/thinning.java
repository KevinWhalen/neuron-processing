package thinning;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
//import ij.io.FileSaver;
//import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
//import ij.process.BinaryProcessor;

/*import java.awt.Color;
 import java.awt.image.BufferedImage;
 import java.io.File;
 import java.io.FileOutputStream;
 import java.io.IOException;*/

//import javax.imageio.ImageIO;

//import thinning.thinning.WaitCoord;

//import coordinates.Coordinates;

//import microvesselAnalysis.TDRgrowing.TDRgrowing;

import java.util.LinkedList;

/**
 *
 * @author nayef
 */
public class thinning implements PlugInFilter {

	public class WaitCoord {
		Coordinates cord;
		int color;

		WaitCoord(int x, int y, int c) {
			cord = new Coordinates(x, y);
			color = c;
		}
	}


	LinkedList<WaitCoord> FIFOList = new LinkedList<WaitCoord>();
	public ImagePlus imp;
	static String Title;
	int height, width;
	int Colors[];
	int BoarderColor;


	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		thinning ob = new thinning();
		ImagePlus im = IJ.openImage("/Users/shuchang/Documents/workspace/ImageThinning/src/thinning/b.tif");
		// ImagePlus im = IJ.openImage("C:\\Image\\Microvessel_3D applied.tif");
		ob.imp = im;
		im.show();
		ob.run(ob.imp.getProcessor());
		IJ.log("Done");
	}


	@Override
	public void run(ImageProcessor ip) {
		// ip.bin(shrinkFactor)
		initcolors();
		height = ip.getHeight();
		width = ip.getWidth();
		// int [][]Values = new int[width][height];
		
		ImageStack stack = imp.getStack();
		ImageStack SkeletonBH = imp.createEmptyStack();
		ImageStack SkeletonColor = imp.createEmptyStack();
		ImageStack StackColored = imp.createEmptyStack();
		
		int st = 1;
		int stacksize = stack.getSize();
		while (st <= stacksize) {
			ByteProcessor B = (ByteProcessor) stack.getProcessor(st).duplicate();
			B.invert();
			B.skeletonize();
			ByteProcessor DupB = (ByteProcessor) B.duplicate();
			ColorProcessor cp = (ColorProcessor) B.convertToRGB();
			ColorProcessor orgCp = (ColorProcessor) stack.getProcessor(st).duplicate().convertToRGB();
			// ImagePlus im1 = new ImagePlus("colored_skeleton",cp);
			ImagePlus imorg = new ImagePlus("colored_skeleton", orgCp);

			int i = 0, j = 0, found = 0;
			while (true) {
				found = 0;

				for (j = 0; j < DupB.getHeight() && found == 0; j++)
					for (i = 0; i < DupB.getWidth(); i++) {
						// IJ.log
						if (DupB.getPixel(i, j) == 0) {
							found = 1;
							break;
						}
						// cp.set(i,j,Colors[2]);
					}

				if (i == DupB.getWidth() || j == DupB.getHeight()) {
					IJ.log("No black Pixels found");
					break;
				}
				LinkedList<Coordinates> branchCoords = 
					ColorBranches(orgCp, cp,DupB, i, j - 1);
				LinkedList<Coordinates> BoundaryCoordinates = 
					FindBoundary(stack.getProcessor(st).duplicate(), i, j - 1);

				imorg.show();
				imorg.updateAndDraw();
				try {
					ColorGrowing(imorg, orgCp, stack.getProcessor(st),
							branchCoords);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			imorg.close();
			SkeletonBH.addSlice(B);
			SkeletonColor.addSlice(cp);
			StackColored.addSlice(orgCp);

			st++;
		}

		ImagePlus SkelBH = new ImagePlus("Black and White Skeleton", SkeletonBH);
		SkelBH.show();
		SkelBH.updateAndDraw();
		ImagePlus SkelColor = new ImagePlus("Colored Skeleton", SkeletonColor);
		SkelColor.show();
		SkelColor.updateAndDraw();
		ImagePlus ColoredImage = new ImagePlus("Colored Image", StackColored);
		ColoredImage.show();
		ColoredImage.updateAndDraw();

		// FileSaver FS = new FileSaver(im1);
		// FS.saveAsTiff("C:\\Image\\Microvessel-Th_color.tif");
	}

	
	private LinkedList<Coordinates> FindBoundary(
		ImageProcessor duplicate,
		int i,
		int j
	){
		LinkedList<Coordinates> BoundaryNodes = new LinkedList<Coordinates>();
		LinkedList<Coordinates> NodeList = new LinkedList<Coordinates>();
		LinkedList<Coordinates> l = new LinkedList<Coordinates>();
		NodeList.add(new Coordinates(i, j));
		Coordinates temp = null;
		ByteProcessor B = duplicate.convertToByteProcessor();
		B.invert();
		ByteProcessor BD = (ByteProcessor) B.duplicate();
		// ByteProcessor BC;

		ImagePlus umg = new ImagePlus("TestImage", B);
		umg.show();
		umg.updateAndDraw();

		while (!NodeList.isEmpty()) {
			temp = NodeList.poll();
			// BC = B;
			// LinkedList<Coordinates> l = new LinkedList<Coordinates>();
			int p, q;
			for (int m = -1; m < 2; m++) {
				for (int n = -1; n < 2; n++) {
					p = temp.getX() + m;
					q = temp.getY() + n;
					// IJ.log("How");
					// Coordinates temp = new Coordinates(p,q );
					if ((i == 0) && (j == 0))
						continue;
					if (p < 0 || p >= width || q < 0 || q >= height)
						continue;
					if (B.getPixel(p, q) == 0) {

						l.add(new Coordinates(p, q));
						// ip.putPixel(p, q,255);
					}
				}
			}

			int l_size = 0;
			int n_neigh = l.size();
			Coordinates C;
			while (!l.isEmpty()) {
				C = l.poll();
				if (BD.getPixel(C.getX(), C.getY()) == 0) {
					BD.putPixel(C.getX(), C.getY(), 255);
					NodeList.add(C);
				}
			}
			// NodeList.addAll(l);
			if (n_neigh < 8) {
				BoundaryNodes.add(temp);
				IJ.log("B X = " + temp.getX() + "  Y= " + temp.getY() + " j ="
						+ j + " pixel value"
						+ B.getPixel(temp.getX(), temp.getY()));
			}
		}
		return BoundaryNodes;
	}


	public void initcolors() {
		Colors = new int[4];
		int Colors_set[][] = {{255, 0, 0}, {0, 255, 0}, {0, 0, 255}};
		BoarderColor = 0 << 16 | 100 << 8 | 200;
		for (int i = 0; i < Colors_set.length; i++) {
			Colors[i] = Colors_set[i][0] << 16 | Colors_set[i][1] << 8 | Colors_set[i][2];
		}
	}


	/**
	 * @return A new list of coordinates that surround the given point in the given processor.
	 */
	public LinkedList<Coordinates> getNeighbours(int x, int y, ByteProcessor ip) {
		LinkedList<Coordinates> l = new LinkedList<Coordinates>();
		int p, q;
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				p = x + i;
				q = y + j;
				// Coordinates temp = new Coordinates(p,q );
				if ((i == 0) && (j == 0))
					continue;
				if (p < 0 || p >= width || q < 0 || q >= height)
					continue;
				if (ip.getPixel(x + i, y + j) == 0) {
					l.add(new Coordinates(p, q));
					ip.putPixel(p, q, 255);

				}
			}
		}
		return l;
	}


	// Has the logic to count number of branches.
	public LinkedList<Coordinates> ColorBranches(
		ColorProcessor orgCp,
		ColorProcessor cp, 
		ByteProcessor ip, 
		int startx, 
		int starty
	){
		int numberOfbranch = 1;
		LinkedList<Coordinates> branchCoords = new LinkedList<Coordinates>();
		int currentColorN = 0;
		WaitCoord temp;
		Coordinates TempCord = null;// = new Coordinates();
		cp.set(startx, starty, Colors[currentColorN]);
		branchCoords.add(new Coordinates(startx, starty));
		orgCp.set(startx, starty, Colors[currentColorN]);
		if (starty < 5)
			IJ.log("start pixel =  X = " + startx + "  y " + starty);
		ip.putPixel(startx, starty, 255);

		LinkedList<Coordinates> l = new LinkedList<Coordinates>();
		l = this.getNeighbours(startx, starty, ip);
		if (l.size() == 1) {
			TempCord = l.poll();
			FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), currentColorN));
			// IJ.log("Started 1");
		} else if (l.size() == 2) {
			TempCord = l.poll();
			FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), currentColorN));
			TempCord = l.poll();
			FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), currentColorN));
			// IJ.log("Started 2" + TempCord.getX());
		} else if (l.size() == 3) {
			TempCord = l.poll();
			FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), currentColorN));
			TempCord = l.poll();
			currentColorN = (currentColorN + 1) % 3;
			numberOfbranch++;
			FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), currentColorN));
			TempCord = l.poll();
			currentColorN = (currentColorN + 1) % 3;
			numberOfbranch++;
			FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), currentColorN));
			// IJ.log("Started 3");
		}

		// FIFOList.add(new WaitCoord(startx,starty, Colors[currentColor]));
		// IJ.log("Fifo size =" + FIFOList.size());
		while (!FIFOList.isEmpty()) {
			temp = (WaitCoord) FIFOList.removeLast();
			// IJ.log("Fifo size =" + FIFOList.size());
			currentColorN = temp.color;
			ip.putPixel(temp.cord.getX(), temp.cord.getY(), 255);
			cp.set(temp.cord.getX(), temp.cord.getY(), Colors[currentColorN]);
			branchCoords.add(temp.cord);
			orgCp.set(temp.cord.getX(), temp.cord.getY(), Colors[currentColorN]);
			l.clear();
			l = this.getNeighbours(temp.cord.getX(), temp.cord.getY(), ip);
			// IJ.log("list size =" + l.size());
			while (l.size() == 1) {
				TempCord = l.poll();
				int tx = TempCord.getX(), ty = TempCord.getY();
				ip.putPixel(tx, ty, 255);
				cp.set(tx, ty, Colors[currentColorN]);
				branchCoords.add(TempCord);
				orgCp.set(tx, ty, Colors[currentColorN]);
				l.clear();
				l = this.getNeighbours(tx, ty, ip);
				// IJ.log("list size  In loop=" + l.size());
			}
			if (l.size() == 2) {
				Coordinates C1 = l.poll();
				Coordinates C2 = l.poll();
				/* checks if two new neighbours are adjacent, then */
				if ((
						(C1.getX() == C2.getX()) && 
						(Math.abs(C1.getY() - C2.getY()) == 1)
					) || (
							(C1.getY() == C2.getY()) && 
							(Math.abs(C1.getX() - C2.getX()) == 1)
					)
				){
					IJ.log("Yes  X =" + TempCord.getX() + " Y = "
							+ TempCord.getY());
					if (C1.getX() == TempCord.getX() || C1.getY() == TempCord.getY()) {
						TempCord = C1;
						ip.putPixel(C2.getX(), C2.getY(), 0);
					} else {
						TempCord = C2;
						ip.putPixel(C1.getX(), C1.getY(), 0);
					}

					FIFOList.add(new WaitCoord(TempCord.getX(),
							TempCord.getY(), currentColorN));
				} else {
					currentColorN = (currentColorN + 1) % 3;
					numberOfbranch++;
					// TempCord = l.poll();
					FIFOList.add(new WaitCoord(C1.getX(), C1.getY(),
							currentColorN));
					currentColorN = (currentColorN + 1) % 3;
					numberOfbranch++;
					// TempCord = l.poll();
					FIFOList.add(new WaitCoord(C2.getX(), C2.getY(),
							currentColorN));
				}
			}
			if (l.size() == 3) {
				int pcolor = currentColorN;
				int pCordx = TempCord.getX();
				int pCordy = TempCord.getY();
				int c;
				TempCord = l.poll();
				if (pCordx == TempCord.getX() || pCordy == TempCord.getY())
					c = pcolor;
				else {
					currentColorN = (currentColorN + 1) % 3;
					c = currentColorN;
					numberOfbranch++;
				}
				FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), c));

				TempCord = l.poll();
				if (pCordx == TempCord.getX() || pCordy == TempCord.getY())
					c = pcolor;
				else {
					currentColorN = (currentColorN + 1) % 3;
					c = currentColorN;
					numberOfbranch++;
				}
				FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), c));

				TempCord = l.poll();
				if (pCordx == TempCord.getX() || pCordy == TempCord.getY())
					c = pcolor;
				else {
					currentColorN = (currentColorN + 1) % 3;
					c = currentColorN;
					numberOfbranch++;
				}
				FIFOList.add(new WaitCoord(TempCord.getX(), TempCord.getY(), c));
				// IJ.log("Started 3");
			}
		}
		IJ.log("number of branches = " + numberOfbranch);
		return branchCoords;

	}

	private void ColorGrowing(
		ImagePlus imorg, 
		ColorProcessor orgCp,
		ImageProcessor ip, 
		LinkedList<Coordinates> branchCoords
	)
		throws InterruptedException
	{
		LinkedList<Coordinates> neighbours = new LinkedList<Coordinates>();

		Coordinates temp;
		int pixelColor;
		ByteProcessor dupip = (ByteProcessor) ip.duplicate();
		dupip.invert();
		Coordinates El;
		int k = branchCoords.size() - 1;
		while (k >= 0) {
			El = branchCoords.get(k);
			dupip.putPixel(El.getX(), El.getY(), 255);
			k--;
		}
		// ImagePlus dupimg = new ImagePlus("Duptlitate IP",orgCp);
		k = 0;
		while (!branchCoords.isEmpty()) {
			temp = branchCoords.poll();
			pixelColor = orgCp.get(temp.getX(), temp.getY());
			// IJ.log("Color" + pixelColor+ " Red "+ Colors[0]);
			dupip.putPixel(temp.getX(), temp.getY(), 255);
			neighbours = this.getNeighbours(temp.getX(), temp.getY(), dupip);

			// if(neighbours.size()<3)//boundary pixel
			// orgCp.set(temp.getX(), temp.getY(), BoarderColor);

			while (!neighbours.isEmpty()) {
				temp = neighbours.poll();
				if (temp.getX() != width && temp.getY() != height) {
					// IJ.log("Cordx= "+ temp.getX()+ "  CordY "+temp.getY());
					orgCp.set(temp.getX(), temp.getY(), pixelColor);
					imorg.show();
					imorg.updateAndDraw();
					// Thread.sleep(1);
					branchCoords.add(temp);
				}
			}
		}
		// dupimg.close();
		IJ.log("one branch done");

	}

	@Override
	public int setup(String arg0, ImagePlus im) {
		this.imp = im;
		Title = imp.getTitle().toString();
		return DOES_ALL;
	}
}