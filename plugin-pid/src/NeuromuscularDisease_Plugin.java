import java.awt.BorderLayout;
import java.awt.TextArea;

import javax.swing.JButton;
import javax.swing.JFrame;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;


public class NeuromuscularDisease_Plugin implements PlugInFilter {
	
	private ImagePlus imp;
	private ImagePlus greenChannel;
	private Integer averageIntensityGreen;
	private JFrame tutorialFrame;

	public void run(ImageProcessor ip) {
		this.greenChannel = this.getGreenChannel(ip);
		Integer h = this.getAverageIntensity(this.greenChannel);
		
		this.greenChannel.show();
		//this.createTutorialFrame();
		//this.tutorialFrame.setVisible(true);
		this.hMinima(this.greenChannel, h/2);
	}
	
	/*
	 * @param ImageProcessor
	 * @return ImagePlus
	 */
	private ImagePlus getGreenChannel(ImageProcessor ip)
	{
		ColorProcessor cp = ip.convertToColorProcessor();
		int width = ip.getWidth();
		int height = ip.getHeight();
		byte[] green = new byte[width*height];
		//Extract the green channel (number 2)
		green = cp.getChannel(2);
		ImagePlus greenImage = NewImage.createByteImage("Green_Channel", width, height, 1, NewImage.FILL_WHITE);

		byte[] pixels = (byte[]) greenImage.getProcessor().getPixels();
		
		//Copy green channel to the new image
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = green[i];
		}

		return greenImage;
	}
	
	/*
	 * @param ImagePlus
	 * @return Integer
	 */
	private Integer getAverageIntensity(ImagePlus image)
	{
		int width = image.getWidth();
		int height = image.getHeight();
		Integer intensity = 0;
		
		byte[] pixels = (byte[]) image.getProcessor().getPixels();
		
		//Copy green channel to the new image
		for (int i = 0; i < pixels.length; i++) {
			intensity += pixels[i];
		}
		
		this.averageIntensityGreen = (intensity/(width*height));
		
		return this.averageIntensityGreen;
	}
	
	private void hMinima(ImagePlus ip, int h)
	{
		ImagePlus _b = ip.duplicate();
		System.out.println(ip.getTitle());
		_b.setTitle("_b");
		_b.show();
		System.out.print(h);
		IJ.selectWindow("_b");
		IJ.run("Subtract...", "value="+50);
		IJ.run("GreyscaleReconstruct ", "mask=Green_Channel seed=_b create");
		IJ.selectWindow("Green_Channel");
		IJ.run("Invert");
		IJ.selectWindow("Reconstructed");
		IJ.run("Invert");
	}
	
	/*
	 * @return void
	 */
	private void createTutorialFrame()
	{
		JFrame frame = new JFrame("Tutorial");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		/** Textarea to explain the algorithm */
		TextArea ta = new TextArea(10, 25);
		
		/** Button to perform algorithm step by step */
		JButton next = new JButton("Siguiente paso");
		
		frame.getContentPane().add(ta, BorderLayout.CENTER);
		frame.getContentPane().add(next, BorderLayout.CENTER);
		frame.pack();
		this.tutorialFrame = frame;		
	}

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;

		return DOES_ALL;
	}

}
