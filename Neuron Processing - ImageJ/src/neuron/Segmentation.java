package neuron;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.gui.Plot;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Segmentation
	implements PlugInFilter
{
	//private ImagePlus imp;
	public ImagePlus imp;


	/*public static void main(String[] args)
	{
		// ImageJ arguments.
		String[] ijArgs = {
				//"-Dplugins.dir=...Fiji.app/fiji/plugins",
				//"-Dmacros.dir=...Fiji.app/fiji/macros"
				
				//"-Dplugins.dir=C:\\Users\\whalenk\\Fiji.app\\plugins",
				//"-Dplugins.dir=C:\\Users\\whalenk\\Fiji.app\\macros"
				
				//"-Dplugins.dir=C/Users/whalenk/Fiji.app/plugins",
				//"-Dplugins.dir=C/Users/whalenk/Fiji.app/macros"
		};
		ij.ImageJ.main(ijArgs);

		ImagePlus testData = IJ.openImage("test-data/pyramidalcells.tif");
		testData.show();

		// Without getting back a pointer, and automatically showing it:
		//IJ.open("pyramidalcells.tif");

		Segmentation a = new Segmentation();
		a.setup("pyramidalcells", testData);
		a.run(testData.getProcessor());
	}*/


	// Required for PlugInFilter, which requires an image.
	@Override
	public void run(ImageProcessor ip)
	{
		// Log to a console window in ImageJ.
		IJ.log("method: run");
		IJ.log("");
		IJ.log("image PROCESSOR attributes: ");
		IJ.log("    height: " + ip.getHeight());
		IJ.log("    width: " + ip.getWidth());
		IJ.log("    bit depth: " + ip.getBitDepth());
		IJ.log("    background value: " + ip.getBackgroundValue());

		// Built-in processing
		//IJ.run(this.imp, "Make Binary", null);
		//IJ.run(this.imp, "Make Binary", "Method=Default Background=Default 'Black background (of binary masks)'");// 'Calculate threshold for each image'");
		// http://albert.rierol.net/imagej_programming_tutorials.html#How to automate an ImageJ dialog
		
		//ImageStack is = this.imp.getImageStack();
		//is.
		IJ.log("");
		IJ.log("image PLUS attributes: ");
		IJ.log("    stack size: " + this.imp.getStackSize());
		IJ.log("    current slice: " + this.imp.getCurrentSlice());
		IJ.log("    slice: " + this.imp.getSlice());

		// Pre-process image.
		//IJ.run(this.imp, "Make Binary", null);
		IJ.run(this.imp, "Despeckle", "stack");
		IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Bright");
		IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Dark");
		IJ.run(imp, "Smooth (3D)", "method=Gaussian sigma=1.000 use");

		// --- attempt to use CLAHE ---
		int blocksize = 127;
		int histogram_bins = 256;
		int maximum_slope = 3;
		String mask = "*None*";
		boolean fast = false;//true;
		//boolean process_as_composite = true;
		 
		//getDimensions( width, height, channels, slices, frames );
		//boolean isComposite = channels > 1;
		String parameters =
				"blocksize=" + blocksize +
				" histogram=" + histogram_bins +
				" maximum=" + maximum_slope +
				" mask=" + mask;
		if (fast) parameters += " fast_(less_accurate)";
		//if (isComposite && process_as_composite){
		//	parameters += " process_as_composite";
		//	channels = 1;
		//}


		IJ.log("    parameters: ");
		IJ.log("        " + parameters);
		IJ.log("        blocksize=127 histogram=256 maximum=3 mask=*None*");
		//IJ.run(this.imp, "Enhance Local Contrast (CLAHE)", parameters);
		// CLAHE cannot run until the image is unlocked.
		this.imp.unlock();
		//IJ.run(this.imp, "Enhance Local Contrast (CLAHE)", "blocksize=127 histogram=256 maximum=3 mask=*None*");
		for (int i = 1; i <= this.imp.getStackSize(); ++i){
			this.imp.setSlice(i);
			IJ.log("    current slice: " + this.imp.getCurrentSlice());
			//IJ.log("    slice: " + this.imp.getSlice());
			// http://fiji.sc/Enhance_Local_Contrast_(CLAHE)
			IJ.run(this.imp, "Enhance Local Contrast (CLAHE)", parameters);
		}
		
		//double[] a = {1,2,3};
		//plot(a);
		//resultsExample();
		
		// Put the segmentation overlay back over the original image.
		//this.imp.setOverlay(overlay);
	}


	// Required for PlugInFilter, which requires an image's metadata.
	@Override
	public int setup(String s, ImagePlus imp)
	{
		IJ.log("method: setup");
		//IJ.log(imp.getInfoProperty());
		this.imp = imp;
		return DOES_ALL;
	}


	// Plots
	void plot(double[] values)
	{
		double[] x = new double[values.length];
		for (int i = 0; i < x.length; i++)
			x[i] = i;
		Plot plot = new Plot("Plot window", "x", "values", x, values);
		plot.show();
	}
	/*void plot(double[] values, double[] values2)
	{
		double[] x = new double[values.length];
		for (int i = 0; i < x.length; i++)
			x[i] = i;
		Plot plot = new Plot("Plot window", "x", "values", x, values);
		plot.setColor(Color.RED);
		plot.draw();
		plot.addPoints(x, values2, Plot.LINE);
		plot.show();
	}
	void plot(double[] values)
	{
	// ...
	PlotWindow plotWindow = plot.show();
	// ...
	Plot plot = new Plot("Plot window", "x", "values", x, values);
	plotWindow.drawPlot(plot);
	}*/
	
	
	// Results
	void resultsExample()
	{
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt == null) {
			rt = new ResultsTable();
			Analyzer.setResultsTable(rt);
		}
		for (int i = 1; i <= 10; i++) {
			rt.incrementCounter();
			rt.addValue("i", i);
			rt.addValue("log", Math.log(i));
		}
		rt.show("Results");
	}
} // End of Segmentation class.


/* Image types.
static int DOES_16
The plugin filter handles 16 bit grayscale images.
static int DOES_32
The plugin filter handles 32 bit floating point grayscale images.
static int DOES_8C
The plugin filter handles 8 bit color images.
static int DOES_8G
The plugin filter handles 8 bit grayscale images.
static int DOES_ALL
The plugin filter handles all types of images.
static int DOES_RGB
The plugin filter handles RGB images.
static int DOES_STACKS
The plugin filter supports stacks, ImageJ will call it for each slice in a stack.
static int DONE
If the setup method returns DONE the run method will not be called.
static int NO_CHANGES
The plugin filter does not change the pixel data.
static int NO_IMAGE_REQUIRED
The plugin filter does not require an image to be open.
static int NO_UNDO
The plugin filter does not require undo.
static int ROI_REQUIRED
The plugin filter requires a region of interest (ROI).
Static int STACK_REQUIRED
The plugin filter requires a stack.
static int SUPPORTS_MASKING
Plugin filters always work on the bounding rectangle of the ROI. If this flag is set
and there is a non-rectangular ROI, ImageJ will restore the pixels that are inside
the bounding rectangle but outside the ROI.
*/
