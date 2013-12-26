import java.util.Vector;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.ChannelSplitter;
import ij.plugin.HyperStackReducer;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.RGBStackSplitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;


public class NeuromuscularDiseasePlugin implements PlugInFilter {
	
	private ImagePlus imp;

	public void run(ImageProcessor ip) {
		ImagePlus greenChannel = this.getGreenChannel(ip);
		greenChannel.show();
	}
	
	/*
	 * @param ImageProcessor
	 * @return ImagePlus
	 */
	private ImagePlus getGreenChannel(ImageProcessor ip) {
		ColorProcessor cp = ip.convertToColorProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();
		byte[] green = new byte[width*height];
		//Extract the green channel (number 2)
		green = cp.getChannel(2);
		ImagePlus greenImage = NewImage.createByteImage("Green Channel", width, height, 1, NewImage.FILL_WHITE);

		byte[] pixels = (byte[]) greenImage.getProcessor().getPixels();
		
		//Copy green channel to the new image
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = green[i];
		}

		return greenImage;
	}

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;

		return DOES_ALL;
	}

}
