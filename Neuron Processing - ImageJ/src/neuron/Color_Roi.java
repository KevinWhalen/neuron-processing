/* Color_Roi.java
 * 2014-10-30
*/


package neuron;

import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.plugin.filter.PlugInFilter;
//import ij.plugin.frame.RoiManager;
import ij.Prefs;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;


/**
 * Color Region of Interest
 *
 * Changes the white or black pixels with a RoI to the specified color.
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 */
public class Color_Roi
	implements PlugInFilter
{
	protected ImagePlus image;
	protected ImagePlus coloredImp;

	// Image property members.
	private int width;
	private int height;

	// Plugin parameters.
	private int value = 0;
	//public String name;
	public boolean useNew;
	public boolean blackBackground;
	// http://rsb.info.nih.gov/ij/developer/api/ij/gui/Roi.html
	public Roi roi;
	//roi.isArea() 


	/**
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp)
	{
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB;
	}

	
	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip)
	{
		// Get image properties.
		width = ip.getWidth();
		height = ip.getHeight();
		
		if (showDialog()){
			if (this.blackBackground) this.value = 1;
			if (this.useNew){
				/*ImageProcessor ip2 = ip.duplicate();
				new ImagePlus("Colored", ip2).show();
				ImagePlus tempImp = IJ.getImage();
				this.old = this.image;
				this.image = tempImp;
				this.roi = this.image.getRoi();
				process(ip2);
				coloredImp.updateAndDraw();*/
				this.image = new Duplicator().run(this.image);
				this.image.show();
			} /*else {
				this.roi = this.image.getRoi();
				process(this.image);
				//process(ip);
				this.image.updateAndDraw();
			}*/
			this.roi = this.image.getRoi();
			//this.image.setType(ImagePlus.COLOR_RGB);
			ImageConverter convert = new ImageConverter(this.image);
			convert.convertToRGB();
			IJ.log("Image type after conversion: " + this.image.getType());
			process(this.image);
			this.image.updateAndDraw();
		}
	}


	private boolean showDialog()
	{
		// Preferences.
		this.useNew = Prefs.get("CR_newImage.boolean", false);
		this.blackBackground = Prefs.get("CR_blackBackground.boolean", false);
		
		GenericDialog gd = new GenericDialog("Process pixels");

		// Create dialog fields.
		// Default value is 0.00, 2 digits right of the decimal point.
		//gd.addNumericField("value", 0.00, 2);
		//gd.addStringField("name", "SOME NAME");
		gd.addCheckbox("Use a new image", this.useNew);
		gd.addCheckbox("Black background", this.blackBackground);

		gd.showDialog();
		if (gd.wasCanceled()) return false;

		// Get submitted values.
		//value = gd.getNextNumber();
		//name = gd.getNextString();
		this.useNew = gd.getNextBoolean();
		this.blackBackground = gd.getNextBoolean();
		
		Prefs.set("CR_useNew.boolean", this.useNew);
		Prefs.set("CR_blackBackground.boolean", this.blackBackground);

		return true;
	}


	/**
	 * Process an image.
	 *
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 *
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public void process(ImagePlus image) {
		// Slice numbers start with 1 for historical reasons.
		int size = image.getStackSize();
		for (int i = 1; i <= size; i++){
			IJ.showProgress(i, size);
			process(image.getStack().getProcessor(i));
		}
	}


	// Select processing method depending on image type
	public void process(ImageProcessor ip) {
		int type = image.getType();
		// FIXME: switch to only processing those pixels that are within the RoI.
		if (type == ImagePlus.GRAY8)
			process( (byte[]) ip.getPixels() );
		else if (type == ImagePlus.GRAY16)
			process( (short[]) ip.getPixels() );
		else if (type == ImagePlus.GRAY32)
			process( (float[]) ip.getPixels() );
		else if (type == ImagePlus.COLOR_RGB)
			process( (int[]) ip.getPixels() );
		else {
			throw new RuntimeException("Image type is not supported!");
		}
	}


	// processing of GRAY8 images
	public void process(byte[] pixels) {
		// y is a horizontal line
		for (int y = 0; y < height; y++) {
			// x is the position in the line
			for (int x = 0; x < width; x++) {
				// Process each pixel of the line.
				
				// http://fiji.sc/Introduction_into_Developing_Plugins#Working_with_the_pixels.27_values
				int v = pixels[x + y * width] & 0xff;

				if (v == this.value)
				IJ.log(Integer.toString(v));
			}
		}
	}


	// processing of GRAY16 images
	public void process(short[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (short)value;
			}
		}
	}


	// processing of GRAY32 images
	public void process(float[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (float)value;
			}
		}
	}


	// processing of COLOR_RGB images
	public void process(int[] pixels) {
		/*for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (int)value;
			}
		}*/

		// http://fiji.sc/Introduction_into_Developing_Plugins#Working_with_the_pixels.27_values
		// Get all pixels of a ColorProcessor.
		// FIXME: switch to only processing those pixels that are within the RoI.
		for (int y = 0; y < height; y++){
		    for (int x = 0; x < width; x++){
		    	if (this.roi.contains(x, y)){
			        int rgb = pixels[x + y * width]; // value is a bit-packed RGB value.
			        IJ.log(Integer.toBinaryString(rgb));
			        int red = rgb & 0xff;
			        int green = (rgb >> 8) & 0xff;
			        int blue = (rgb >> 16) & 0xff;
			        IJ.log("rgb(" + red + ", " + green  + ", " + blue + ")");
			        rgb = red;
			        rgb = (rgb << 8) + green;
			        rgb = (rgb << 8) + blue;
			        pixels[x + y * width] = rgb;
			        IJ.log(Integer.toBinaryString(rgb));
		    	}
		    }
		}
	}


	void showAbout() {
		IJ.showMessage("Color_Roi",
			"Changes the white or black pixels with a RoI to the specified color."
		);
	}
}
