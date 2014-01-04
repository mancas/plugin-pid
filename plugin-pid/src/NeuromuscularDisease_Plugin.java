import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.NewImage;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;


public class NeuromuscularDisease_Plugin implements PlugInFilter, ActionListener {
	
	private ImagePlus imp;
	private ImagePlus greenChannel;
	private Integer averageIntensityGreen;
	private JFrame tutorialFrame;
	private JTextArea descriptionField;
	private int count;

	public void run(ImageProcessor ip) {
		//We need to check if the current image is RGB or not.
		if (imp.getBitDepth() != 64) {
			IJ.showMessage("Neuromuscular Disease", "Debe seleccionar una imagen RGB.");
		}
		this.greenChannel = this.getGreenChannel(ip);		
		this.greenChannel.show();
		this.createTutorialFrame();
		this.tutorialFrame.setVisible(true);
	}
	

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		this.count = 0;

		return DOES_ALL;
	}

	
	/*
	 * @param ImageProcessor
	 * @return ImagePlus
	 */
	private ImagePlus getGreenChannel(ImageProcessor ip)
	{
		ColorProcessor cp = (ColorProcessor) ip;
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
		_b.setTitle("_b");
		_b.show();
		IJ.selectWindow("_b");
		IJ.run("Subtract...", "value="+h);
		IJ.run("GreyscaleReconstruct ", "mask=Green_Channel seed=_b create");
		ImagePlus reconstructed = WindowManager.getImage("Reconstructed");
		IJ.selectWindow("Reconstructed");
		reconstructed.setTitle("hMinima transform");
		IJ.run("Invert");
		//These lines allow us to close the temp img without been saved
		_b.changes = false;
		_b.close();
		//IJ.run("Domes ", "height=1 basins");
		//IJ.setThreshold(1, 255);
		//IJ.run("Convert to Mask");
		//IJ.run("Threshold", "method=Percentile mode=B&W");
		//Set threshold
		IJ.setAutoThreshold(reconstructed, "Percentile");
		Prefs.blackBackground = false;
		IJ.run(reconstructed, "Convert to Mask", "");
	}
	
	/*
	 * @return void
	 */
	private void createTutorialFrame()
	{
		JFrame frame = new JFrame("Tutorial");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		/** Main panel which contains the textarea and the next step button */
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setOpaque(true);
		
		/** Textarea to explain the algorithm */
		this.descriptionField = new JTextArea(10, 25);
		this.descriptionField.setText("El primer paso es obtener la matriz del canal verde, ya que es la que más contraste ofrece.");
		this.descriptionField.setEditable(false);
		this.descriptionField.setWrapStyleWord(true);
		this.descriptionField.setLineWrap(true);
		
		JScrollPane scroller = new JScrollPane(this.descriptionField);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	
		
		/** Button to perform algorithm step by step */
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		JButton next = new JButton("Siguiente paso");
		next.addActionListener(this);
		
		//Adding elements to their correspond panels
		topPanel.add(scroller);
		buttonPanel.add(next);
		topPanel.add(buttonPanel);
		
		frame.getContentPane().add(BorderLayout.CENTER, topPanel);
		frame.pack();
		frame.setResizable(false);
		this.tutorialFrame = frame;
	}

	public void actionPerformed(ActionEvent e) {
		this.count++;
		switch (this.count) {
		case 1:
			this.descriptionField.setText("Una vez obtenida la matriz del canal verde, aplicamos el algoritmo H-Minima extendido para obtener la separación de las células.");
			Integer h = this.getAverageIntensity(this.greenChannel);
			this.hMinima(this.greenChannel, (2*h)/3);
			break;
		}
		
	}

}
