/* Segmentation.java
 * 
 * https://github.com/imagej/imagej
 * https://github.com/fiji/fiji
 * 
 * http://fiji.sc/Developing_Fiji#Writing_plugins
 * http://fiji.sc/Developing_Fiji_in_Eclipse
 * http://fiji.sc/Introduction_into_Developing_Plugins
 * 
 * Look into: 
 *   http://rsbweb.nih.gov/ij/docs/guide/146-29.html
 *   https://github.com/imagej/minimal-ij1-plugin/blob/master/src/main/java/Process_Pixels.java
 * 2014-10-01
*/


package neuron;

import CED.Canny_Edge_Detector;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Plot;
import ij.ImageStack;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
//import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


public class Segmentation
	implements PlugIn
	//implements PlugInFilter
{
	private ImagePlus impOriginal, imp, impHisto;
	private ImageProcessor ip;
	private ImageStack is;
	
	private int stackSize;


	public static void main(String[] args)
	{
		// ImageJ arguments.
		String[] ijArgs = {
				//"-Dplugins.dir=...Fiji.app/fiji/plugins",
				//"-Dmacros.dir=...Fiji.app/fiji/macros"
		};
		ij.ImageJ.main(ijArgs);

		ImagePlus testData = IJ.openImage("test-data/pyramidalcells.tif");
		testData.show();

		// Without getting back a pointer, and automatically showing it:
		//IJ.open("pyramidalcells.tif");

		Segmentation a = new Segmentation();
		//a.setup("pyramidalcells", testData);
		//a.run(testData.getProcessor());
		a.run(null);
	}


	/*private void preprocess(){
		preprocess("");
	}
	
	
	private void preprocess(String cmd)
	{
		IJ.log(" --- NOT IMPLEMENTED ---");
		switch (cmd){
			case "denoise":
			case "denoise-stack":
				IJ.run(this.imp, "Despeckle", "stack");
				break;
			case "denoise-single":
				IJ.run(this.imp, "Despeckle", "slice");
				IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Bright");
				IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Dark");
				break;
			case "blur":
			case "blur-stack":
				IJ.run(this.imp, "Smooth (3D)", "method=Gaussian sigma=1.000 use");
				IJ.run(this.imp, "Median 3D...", "x=2 y=2 z=2");
				break;
			case "blur-single":
				break;
			default:
				IJ.log(" --- invalid preprocess command: " + cmd + " ---");
		}
	}*/
	

	public void prepocessSecondAttempt()
	{
		this.impOriginal = IJ.getImage();
		
		// CLAHE parameters.
		int blocksize = 127;
		int histogram_bins = 256;
		int maximum_slope = 3;
		String mask = "*None*";
		boolean fast = false;//true;
		//boolean process_as_composite = true;
		 
		//getDimensions(width, height, channels, slices, frames);
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
		
		IJ.run(this.imp, "Bilateral Filter", "spatial=3 range=50");
		this.imp = IJ.getImage();
		this.ip = this.imp.getProcessor();
		this.impOriginal.hide();
		
		IJ.run(this.imp, "Despeckle", "stack");
		IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Bright");
		IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Dark");
		//IJ.run(imp, "Enhance Contrast...", "saturated=0.4 equalize process_all");
		for (int i = 1; i <= this.imp.getStackSize(); ++i){
			this.imp.setSlice(i);
			this.ip.setSliceNumber(i);
			//this.ip = is.getProcessor(i);
			//this.ip = this.imp.getProcessor();
			
			IJ.log("    current slice: " + this.imp.getCurrentSlice());
			//IJ.log("    stack slice: " + this.is.);
			//IJ.log("    processor slice: " + this.ip.getSliceNumber());
			
			// http://fiji.sc/Enhance_Local_Contrast_(CLAHE)
			IJ.run(this.imp, "Enhance Local Contrast (CLAHE)", parameters);
			
			//IJ.run(imp, "Anisotropic Diffusion 2D", "number=20 smoothings=1 keep=20 a1=0.50 a2=0.90 dt=20 edge=5");
			
			IJ.run(this.imp, "Despeckle", "slice");
			IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Bright");
			IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Dark");
			//this.ip.medianFilter(); // long form: ip.filter(ImageProcessor.MEDIAN_FILTER);
			//this.ip.findEdges();
		}
		//IJ.run(this.imp, "Median 3D...", "x=2 y=2 z=2"); // --- ?? ---
		IJ.run(this.imp, "Invert", "stack"); //IJ.run(this.imp, "Invert LUT", "");
		IJ.run(this.imp, "Make Binary", "method=Default background=Default calculate list");
		//IJ.run(imp, "Find Edges", "stack");
		//run.(outline
		//IJ.run(imp, "CED", "gaussian=2 low=2.5 high=7.5"); // Canny_Edge_Detector
		for (int i = 1; i <= this.imp.getStackSize(); ++i){
			this.imp.setSlice(i);
			this.ip.setSliceNumber(i);
			//this.ip.findEdges();
			IJ.run(imp, "CED", "gaussian=2 low=2.5 high=7.5"); // Canny_Edge_Detector
		}
	}
	
	
	/** 
	 * Third'ish attempt.
	 * Image preprocessing for cell segmentation.
	 * Steps:
	 *   Duplicate the image (twice, keep one as original and other as histo).
	 *   Canny Edge detection on the duplicate image.
	 *   Subtract background from duplicate.
	 *   Add duplicate to the one to be histogrammed.
	 *   Run histogram equalization (maybe via CLAHE or Enhance Contrast, or separate Equalize Histogram plugin?).
	 *     http://svg.dmi.unict.it/iplab/imagej/Plugins/Forensics/Histogram%20Equalization/HistogramEqualization.html
	 *
	 *   blur and edge detect again.
	*/
	public void preprocess()
	{
		this.impOriginal = IJ.getImage();
		this.stackSize = this.impOriginal.getStackSize();
		
		// Create a duplicate image, to be processed.
		this.imp = new Duplicator().run(this.impOriginal);
		//this.impHisto = new Duplicator().run(this.impOriginal);
		this.imp.show();
		this.impOriginal.hide();
		this.ip = this.imp.getProcessor();
		//this.is = this.imp.getImageStack();

		for (int i = 1; i <= this.stackSize; ++i){
			this.imp.setSlice(i);
			this.ip.setSliceNumber(i);
			//this.ip.findEdges();
			IJ.run(imp, "CED", "gaussian=2 low=2.5 high=7.5"); // Canny_Edge_Detector
		}

		IJ.run(this.imp, "Subtract Background...", "rolling=50 stack");
		
		ImageCalculator ic = new ImageCalculator();
		this.impOriginal.show();
		this.impHisto = ic.run("Add create stack", this.impOriginal, this.imp);
		this.impOriginal.hide();
		this.imp.hide();
		this.impHisto.show();
		this.ip = this.impHisto.getProcessor();
		
		/*for (int i = 1; i <= this.stackSize; ++i){
			this.impHisto.setSlice(i);
			this.ip.setSliceNumber(i);
			IJ.log("    current slice: " + this.impHisto.getCurrentSlice());
			
			// http://fiji.sc/Enhance_Local_Contrast_(CLAHE)
			IJ.run(this.impHisto, "Enhance Local Contrast (CLAHE)", parameters);
		}*/
		// http://homepages.inf.ed.ac.uk/rbf/HIPR2/pixmult.htm
	}
	
	
	
	@Override
	public void run(String s)
	{
		// Preprocessing attempts
		preprocess();
		
		
		
		// Region of Interest (ROI)
		
		// ...
		//equalize histogram
		//IJ.run(this.imp"Invert", "");
		//IJ.run(this.imp, "Invert LUT", "");
		//IJ.run(this.imp, "Make Binary", "method=Default background=Default calculate list");
		
		// Active Contour
		// http://fiji.sc/Level_Sets
		//IJ.run(imp, "Level Sets", "");
		// http://imagejdocu.tudor.lu/doku.php?id=plugin:segmentation:active_contour:start
		// http://bigwww.epfl.ch/jacob/software/SplineSnake/
		// http://bigwww.epfl.ch/algorithms/esnake/
		
		/*
		// Overlay
//		IJ.run(this.imp, "Invert", "stack");
		String overlayName = this.imp.getTitle(); // The name of the duplicate image that was just processed.
		// switch to original image
		this.impOriginal.show();
		IJ.log("Original title: " + this.impOriginal.getTitle());
		IJ.log("Duplicate's title: " + this.imp.getTitle());
	//	for (int i = 1; i <= this.imp.getStackSize(); ++i){
	//		this.impOriginal.setSlice(i);
	//		this.imp.setSlice(i);
	//		IJ.run("Add Image...", overlayName + " x=0 y=0 opacity=100 zero");
	//	} // http://knolskiranakumarap.wordpress.com/2011/07/03/reconstructing-solid-model-from-2d-3rc2kfwq179j2-5/
		//ImageCalculator ic = new ImageCalculator();
		//ImagePlus imp1 = WindowManager.getImage("pyramidalcells.tif");
		//ImagePlus imp2 = WindowManager.getImage("ced_preprocess_pyramidalcells.tif-3.0-50.0.tif");
		//ImagePlus imp3 = ic.run("Transparent-zero create stack", imp1, imp2);
		//ImagePlus imp3 = ic.run("Transparent-zero create stack", this.impOriginal, this.imp);
		*/
	}


/*
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
		//////IJ.run(this.imp, "Make Binary", null);
		//IJ.run(this.imp, "Despeckle", "stack");
		//IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Bright");
		//IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Dark");
		////IJ.run(imp, "Smooth (3D)", "method=Gaussian sigma=1.000 use");

		
		IJ.run(this.imp, "Despeckle", "stack");
		IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Bright");
		IJ.run(this.imp, "Remove Outliers...", "Radius=2.0, Threshold=50, 'Which outliers'=Dark");
		//IJ.run(imp, "Smooth (3D)", "method=Gaussian sigma=1.000 use");
		
		//
		ip.medianFilter(); // long form: ip.filter(ImageProcessor.MEDIAN_FILTER);
		ip.findEdges();
		
		// http://fiji.sc/Introduction_into_Developing_Plugins#Frequently_used_operators
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
	}*/


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
	
	
	// http://fiji.sc/Introduction_into_Developing_Plugins#Working_with_the_pixels.27_values
	void process()
	{
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
