package se.sandos.mosaicr;

import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

@SuppressWarnings("serial")
public class MainPane extends JFrame
{
    final static int numRows = 40;
    final static int numCols = 54;

    static Vector<BufferedImage> images = new Vector<BufferedImage>();
    static Vector<String> names = new Vector<String>();
    static int[][] tileSources;
    static int[] foundTiles;
    static Map<String, BufferedImage> tileSourceCache = new HashMap<String, BufferedImage>();

    static Vector<JLabel> icons = new Vector<JLabel>();
    
    public MainPane()
    {
        setLayout(new GridLayout(numRows, numCols));
        setTitle("Simple example");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        for (int i = 0; i < numRows*numCols; i++)
        {
            ImageIcon image = new ImageIcon();
            icons.add(new JLabel(image));
            add(icons.get(icons.size()-1));
        }
    }

    public static int[] loadImage(String url) throws Exception
    {
        BufferedImage image = ImageIO.read(new FileInputStream(url));
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    public static void main(String[] args) throws Exception
    {
        File seri = new File("serialization.bin");
//        File dir = new File("Z:\\projs\\mosaicr");
        File dir = new File("C:\\temp\\mosaicr-bilder\\mosaicr\\");

        File[] listFiles = null;

        System.out.println("Listing files");
        listFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith(".png") && (filename.contains("8x8"));
            }
        });

        if (!seri.exists())
        {
            System.out.println("Loading tile sources from disk...");
            System.out.println("Number of files " + listFiles.length);

            tileSources = new int[listFiles.length][];

            for (int i = 0; i < listFiles.length; i++)
            {
            	System.out.println("Loading " + i);
                tileSources[i] = loadImage(listFiles[i].toString());
                names.add(listFiles[i].toString());
            }

            System.out.println("Loaded images from disk, serializing");
            // Save

            Kryo kryo = new Kryo();
            // ...
            Output output = new Output(new FileOutputStream("serialization.bin"));
            kryo.writeObject(output, tileSources);
            output.close();
            
        }
        else
        {
            Kryo kryo = new Kryo();
            
            for (int i = 0; i < listFiles.length; i++)
            {
                names.add(listFiles[i].toString());
            }

            Input input = new Input(new FileInputStream("serialization.bin"));
            tileSources = kryo.readObject(input, int[][].class);
            input.close();
            System.out.println("Loaded images from serialization, " + tileSources.length + " images loaded");
        }

        foundTiles = new int[numRows * numCols];

        final MainPane ex = new MainPane();
        ex.setVisible(true);

        final DCT dct = new DCT(1);
        final int[][][][] dcts = new int[tileSources.length][][][];
        for(int i=0; i<tileSources.length; i++)
        {
        	System.out.println("DCT " + i);
        	dcts[i] = dct.forwardDCT(tileSources[i]);
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
        	public Void doInBackground()
        	{
                try
                {
                    BufferedImage image = ImageIO.read(new FileInputStream("C:\\temp\\mosaicr2\\mosaikorig.jpg"));
                    float rowHeight = image.getHeight() / numRows;
                    float colWidth = image.getWidth() / numCols;

                    long[] avgR, avgB, avgG;

                    avgR = new long[numRows * numCols];
                    avgG = new long[numRows * numCols];
                    avgB = new long[numRows * numCols];

                    // Stupid error diffusion
                    int error = 0;

                    for (int row = 0; row < numRows; row++)
                    {
                        for (int col = 0; col < numCols; col++)
                        {
                            System.out.println("At pixel " + row + " " + col);
                            int startY = (int) (row * rowHeight);
                            int endY = (int) ((row + 1) * rowHeight);
                            int startX = (int) (col * colWidth);
                            int endX = (int) ((col + 1) * colWidth);

//                            for (int x = startX; x < endX; x++)
//                            {
//                                for (int y = startY; y < endY; y++)
//                                {
//                                    int pixel = image.getRGB(x, y);
//                                    avgR[row + col * numRows] += (0xff0000 & pixel) >> 16;
//                                    avgG[row + col * numRows] += (0x00ff00 & pixel) >> 8;
//                                    avgB[row + col * numRows] += (0x0000ff & pixel);
//                                }
//                            }
//
//                            long numPixels = ((endX - startX) * (endY - startY));
//                            avgR[row + col * numRows] = (long) (((float) avgR[row + col * numRows]) / numPixels);
//                            avgG[row + col * numRows] = (long) (((float) avgG[row + col * numRows]) / numPixels);
//                            avgB[row + col * numRows] = (long) (((float) avgB[row + col * numRows]) / numPixels);

                            // Create subimage
                            // System.out.println("endx" + endX + " startx " +
                            // startX);
                            // System.out.println("avgR" + avgR[row + col *
                            // numRows]);
                            // System.out.println("endx" + endY + " startx " +
                            // startY);
                            BufferedImage tile = new BufferedImage(endX - startX, endY - startY, BufferedImage.TYPE_INT_RGB);
                            tile.setRGB(0, 0, endX - startX, endY - startY,
                                    image.getRGB(startX, startY, endX - startX, endY - startY, (int[]) null, 0, endX - startX), 0, endX
                                            - startX);
                            tile.setData(image.getData(new Rectangle(startX, startY, endX - startX, endY - startY)));
                            Image scaledInstance = getScaledInstance(tile, 8, 8, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
                            // scaledInstance =
                            // getScaledInstance((BufferedImage) scaledInstance,
                            // 300, 300,
                            // RenderingHints.VALUE_INTERPOLATION_BICUBIC,
                            // false);
                            images.add((BufferedImage) scaledInstance);

                            long smallestDiff = Long.MAX_VALUE;
                            int iDiff = 0;
                            long realError = 0;
                            int result = -1;
                            
                            int[] srcRGB = ((BufferedImage) scaledInstance).getRGB(0, 0, 8, 8, null, 0, 8);
                            int[][][] forwardDCT = dct.forwardDCT(srcRGB);
                            for (int i = 0; i < tileSources.length; i++)
                            {
//                            	System.out.println("i " + i);
//                                long imageDiff = imageDiff(srcRGB, tileSources[i], error);
                            	long imageDiff = dctDiff(forwardDCT, dcts[i]);
                                if (Math.abs(imageDiff) < smallestDiff)
                                {
                                    smallestDiff = Math.abs(imageDiff);
                                    iDiff = (int) imageDiff;
                                    // System.out.println("New smallest diff " + imageDiff + " at " + i);
                                    result = i;
                                }
                            }
                            if (result != -1)
                            {
                                // error -= iDiff / 15;
                                foundTiles[images.size() - 1] = result;

                                String pngFilename = names.get(result);
                                String jpegFilename = pngFilename.substring(0, pngFilename.length() - 8);

                                // Set minified tile
                                // BufferedImage bi = new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB);
                                // bi.setData(Raster.createRaster(bi.getSampleModel(), new DataBufferInt(
                                // tileSources[i], 9 * 3, 0), new Point(0, 0)));
                                //
                                // bi = getScaledInstance(bi, 6, 6, RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);

                                final JLabel i = icons.get(images.size()-1);
                                if (tileSourceCache.containsKey(pngFilename))
                                {
                                	BufferedImage bi = tileSourceCache.get(names.get(result));
                                    final BufferedImage res = getScaledInstance(bi, 22, 22,  RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
                                    images.set(images.size() - 1, res);
                                    SwingUtilities.invokeLater(new Runnable(){
                                    	public void run()
                                    	{
                                            ((ImageIcon)i.getIcon()).setImage(res);
                                            i.invalidate();
                                            ex.invalidate();
                                    	}
                                    });                                
                                }
                                else
                                {
//                                	System.out.println(jpegFilename);
                                    BufferedImage bi = ImageIO.read(new FileInputStream(jpegFilename));
                                    final BufferedImage res = getScaledInstance(bi, 22, 22,  RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
                                    tileSourceCache.put(pngFilename, res);
                                    images.set(images.size() - 1, res);
                                    
                                    SwingUtilities.invokeLater(new Runnable(){
                                    	public void run()
                                    	{
                                            ((ImageIcon)i.getIcon()).setImage(res);
                                            i.invalidate();
                                            ex.invalidate();
                                            ex.doLayout();
                                    	}
                                    });
                                    
                                }
                            }
                            else
                            {
                            	System.out.println("Did not find!");
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
				return null;
            }
        };
        
        worker.execute();
    }
    
    public static long dctDiff(int[][][] dct1, int[][][] dct2)
    {
    	long diff = 0;
    	
    	diff += Math.abs(dct1[0][0][0] - dct2[0][0][0]);
    	diff += Math.abs(dct1[0][1][0] - dct2[0][1][0]);
    	diff += Math.abs(dct1[0][0][1] - dct2[0][0][1]);
//    	diff += Math.abs(dct1[0][0][3] - dct2[0][0][3]);
//    	diff += Math.abs(dct1[0][0][4] - dct2[0][0][4]);

    	diff += Math.abs(dct1[1][0][0] - dct2[1][0][0]);
    	diff += Math.abs(dct1[1][1][0] - dct2[1][1][0]);
    	diff += Math.abs(dct1[1][0][1] - dct2[1][0][1]);
//    	diff += Math.abs(dct1[1][0][3] - dct2[1][0][3]);
//    	diff += Math.abs(dct1[1][0][4] - dct2[1][0][4]);

    	diff += Math.abs(dct1[2][0][0] - dct2[2][0][0]);
    	diff += Math.abs(dct1[2][1][0] - dct2[2][1][0]);
    	diff += Math.abs(dct1[2][0][1] - dct2[2][0][1]);
//    	diff += Math.abs(dct1[2][0][3] - dct2[2][0][3]);
//    	diff += Math.abs(dct1[2][0][4] - dct2[2][0][4]);

    	
    	return diff;
    }

    public static long imageDiff(int[] one, int[] two, int error)
    {
//    	DCT dct = new DCT(1);
//    	int[][] forwardDCT = dct.forwardDCT(one, 0);
    	
        float sum = 0;
        for (int pixel = 0; pixel < 9; pixel++)
        {
            int midPixelR = (one[pixel] & 0xff0000) >> 16 + error;
            int midPixelG = (one[pixel] & 0xff00) >> 8 + error;
            int midPixelB = one[pixel] & 0xff + error;

            int searchPixelR = (two[pixel] & 0xff0000) >> 16;
            int searchPixelG = (two[pixel] & 0xff00) >> 8;
            int searchPixelB = two[pixel] & 0xff;

            long diff = (midPixelR - searchPixelR) * (midPixelR - searchPixelR);
            // if (diff < 0)
            // {
            // diff = -diff;
            // }
            // if (midPixelG > searchPixelG)
            // {
            diff += (midPixelG - searchPixelG) * (midPixelG - searchPixelG);
            // }
            // else
            // {
            // diff += searchPixelG - midPixelG;
            // }
            // if (midPixelB > searchPixelB)
            // {
            diff += (midPixelB - searchPixelB) * (midPixelB - searchPixelB);
            // }
            // else
            // {
            // diff += searchPixelB - midPixelB;
            // }
            // sum += Math.signum(diff) * diff * diff;
            sum += diff;
        }

        return (long) sum;
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
     * 
     * @param img
     *            the original image to be scaled
     * @param targetWidth
     *            the desired width of the scaled instance, in pixels
     * @param targetHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality
     *            if true, this method will use a multi-step scaling technique that provides higher quality than the usual one-step
     *            technique (only useful in downscaling cases, where {@code targetWidth} or {@code targetHeight} is smaller than the
     *            original dimensions, and generally only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality)
        {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        }
        else
        {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do
        {
            if (higherQuality && w > targetWidth)
            {
                w /= 2;
                if (w < targetWidth)
                {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight)
            {
                h /= 2;
                if (h < targetHeight)
                {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}
