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
	private ImagePlus redChannel;
	private Integer averageIntensityGreen;
	private JFrame tutorialFrame;
	private JTextArea descriptionField;
	private int count;

	public void run(ImageProcessor ip) {
		//We need to check if the current image is RGB or not.
		if (ip.isGrayscale()) {
			IJ.showMessage("Neuromuscular Disease", "Debe seleccionar una imagen RGB.");
			return;
		}
		this.greenChannel = this.getGreenChannel(ip);
		this.redChannel = this.getRedChannel(ip);	
		this.greenChannel.show();
		this.redChannel.show();
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
	 * @param ImageProcessor
	 * @return ImagePlus
	 */
	private ImagePlus getRedChannel(ImageProcessor ip)
	{
		ColorProcessor cp = (ColorProcessor) ip;
		int width = ip.getWidth();
		int height = ip.getHeight();
		byte[] red = new byte[width*height];
		//Extract the red channel (number 1)
		red = cp.getChannel(1);
		ImagePlus redImage = NewImage.createByteImage("Red_Channel", width, height, 1, NewImage.FILL_WHITE);

		byte[] pixels = (byte[]) redImage.getProcessor().getPixels();
		
		//Copy green channel to the new image
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = red[i];
		}

		return redImage;
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
		IJ.run(this.greenChannel, "Enhance Contrast...", "saturated=0.4 equalize");
		//First thing we must to do is duplicate the main image
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

		//Set threshold
		IJ.setAutoThreshold(reconstructed, "Huang");
		Prefs.blackBackground = false;
		IJ.run(reconstructed, "Convert to Mask", "");
		this.sumSobel(reconstructed);
		IJ.setAutoThreshold(reconstructed, "Otsu");
		Prefs.blackBackground = false;
		IJ.run(reconstructed, "Convert to Mask", "");
	}
	
	/*
	 * This method enhance the image by applying some dilates and erodes
	 */
	private void imageEnhancement(ImagePlus ip)
	{
		IJ.selectWindow("hMinima transform");
		IJ.run("BinaryDilate ", "coefficient=2 iterations="+4+" white");
		IJ.run("BinaryErode ", "coefficient=2 iterations="+4+" white");
		IJ.run("BinaryDilate ", "coefficient=2 iterations="+4+" black");
		IJ.run("BinaryErode ", "coefficient=2 iterations="+4+" black");
		IJ.run("BinaryDilate ", "coefficient=2 iterations="+4+" white");
		IJ.run("BinaryErode ", "coefficient=2 iterations="+4+" white");
		IJ.run("BinaryDilate ", "coefficient=2 iterations="+4+" black");
		IJ.run("BinaryErode ", "coefficient=2 iterations="+4+" black");
	}
	
	/*
	 * This method apply sobel masks to the image pass through parameter
	 */
	private void sobel(ImagePlus ip)
	{
		int w = ip.getWidth();
        int h = ip.getHeight();
        //Sobel Mask
        int sobel_x[][] = {{-1,0,1},
                {-2,0,2},
                {-1,0,1}};
		int sobel_y[][] = {{-1,-2,-1},
		                {0,0,0},
		                {1,2,1}};
		int pixel_x;
		int pixel_y;
		ImageProcessor processor = ip.getProcessor();
        ImageProcessor copy = processor.duplicate();

        for (int x=1; x < w-2; x++) {
            for (int y=1; y < h-2; y++) {
                pixel_x = (sobel_x[0][0] * copy.getPixel(x-1,y-1)) + (sobel_x[0][1] * copy.getPixel(x,y-1)) + (sobel_x[0][2] * copy.getPixel(x+1,y-1)) +
                    (sobel_x[1][0] * copy.getPixel(x-1,y))   + (sobel_x[1][1] * copy.getPixel(x,y))   + (sobel_x[1][2] * copy.getPixel(x+1,y)) +
                    (sobel_x[2][0] * copy.getPixel(x-1,y+1)) + (sobel_x[2][1] * copy.getPixel(x,y+1)) + (sobel_x[2][2] * copy.getPixel(x+1,y+1));

                pixel_y = (sobel_y[0][0] * copy.getPixel(x-1,y-1)) + (sobel_y[0][1] * copy.getPixel(x,y-1)) + (sobel_y[0][2] * copy.getPixel(x+1,y-1)) +
                    (sobel_y[1][0] * copy.getPixel(x-1,y))   + (sobel_y[1][1] * copy.getPixel(x,y))   + (sobel_y[1][2] * copy.getPixel(x+1,y)) +
                    (sobel_y[2][0] * copy.getPixel(x-1,y+1)) + (sobel_y[2][1] * copy.getPixel(x,y+1)) + (sobel_x[2][2] * copy.getPixel(x+1,y+1));

                int val = (int)Math.sqrt((pixel_x * pixel_x) + (pixel_y * pixel_y));

                if(val < 0)
                {
                   val = 0;
                }

                if(val > 255)
                {
                   val = 255;
                }

                processor.putPixel(x,y,val);
            }
        }
	}
	
	/*
	 * This method sum the original image and the result obtained from applying sobel masks
	 */
	private void sumSobel(ImagePlus ip)
	{
		int w = ip.getWidth();
		int he = ip.getHeight();
		ImageProcessor original = ip.getProcessor();
		ImagePlus copy = this.greenChannel;
		ImageProcessor copy_processor = copy.getProcessor();
		this.sobel(copy);
		
		for (int i = 0; i < w; i++) {
			for (int j = 0; j < he; j++) {
				original.putPixel(i, j, (original.getPixel(i, j) + copy_processor.getPixel(i, j)));
			}
		}
		copy.changes = false;
		copy.close();
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
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
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
		frame.setResizable(true);
		this.tutorialFrame = frame;
	}

	public void actionPerformed(ActionEvent e) {
		this.count++;
		switch (this.count) {
		case 1:
			this.descriptionField.setText("Una vez obtenida la matriz" +
					" del canal verde, aplicamos el algoritmo H-Minima," + 
					" con un valor de h igual a 2/3 de la intensidad media del canal verde," + 
					" para obtener la separación de las células. Después se aplica una umbralización" + 
					" utilizando el algoritmo Huang puesto que este es el que mejores resultados obtiene. Tras esto," +
					" se le aplica las máscaras de Sobel a la imagen resultante y se vuelve a umbralizar con el algoritmo Otsu.");
			this.tutorialFrame.repaint();
			Integer h = this.getAverageIntensity(this.greenChannel);
			this.hMinima(this.greenChannel, (h)/2);
			break;
		case 2:
			this.descriptionField.setText("Tras la aplicación del algoritmo H-Minima, debemos mejorar la imagen" +
					" para reducir al máximo ruido, pequeñas discontinuidades etc." + 
					" Con este objetivo, aplicamos una serie de dilataciones y erorsiones utilizando el mismo elemento estructural." + 
					" Al utilizar el mismo elemento estructural para realizar una dilatación y a continuación una erosión," + 
					" se consigue tener la misma imagen salvo aquellas pequeñas discontinuidades que cubra el elemento estructural utilizado.");
			this.tutorialFrame.repaint();
			this.imageEnhancement(this.greenChannel);
			break;
		}
		
	}

}
