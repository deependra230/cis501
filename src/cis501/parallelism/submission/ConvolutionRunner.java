package cis501.parallelism.submission;

// code borrowed from https://docs.oracle.com/javase/tutorial/essential/concurrency/forkjoin.html

import cis501.parallelism.ChannelValues;
import cis501.parallelism.IConvolutionRunner;
import cis501.parallelism.Int2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ConvolutionRunner implements IConvolutionRunner {

    private final static int KERNEL_SIZE = 31;

    /** @return the names of the group members for this assignment. */
    public String[] groupMembers() {
        return new String[] {"your", "names"};
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            printUsage();
            return;
        }

        final String mode = args[0];
        final String srcName = args[1];
        final String dstName = args[2];

        File srcFile = new File(srcName);
        BufferedImage srcImage = ImageIO.read(srcFile);

        System.out.println("Source image: " + srcName);

        int w = srcImage.getWidth();
        int h = srcImage.getHeight();

        int[] src = srcImage.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];
        Int2D srcPixels = new Int2D(src, w, h);
        Int2D dstPixels = new Int2D(dst, w, h);

        {
            final ChannelValues pv;
            float[][] gk = createGaussianKernel(KERNEL_SIZE);
            final ConvolutionRunner cr = new ConvolutionRunner();

            final long startTime = System.currentTimeMillis();
            switch (mode) {
                case "s":  // sequential blur
                    pv = cr.sequentialConvolution(gk, srcPixels, dstPixels);
                    break;
                case "p":  // parallel blur
                    pv = cr.parallelConvolution(gk, srcPixels, 0, h, dstPixels);
                    break;
                default:
                    printUsage();
                    return;
            }
            final long endTime = System.currentTimeMillis();
            System.out.format("Performed %s convolution in %d ms%n", mode, endTime - startTime);
            double totalPixelValues = pv.red + pv.green + pv.blue;
            System.out.format("%% R/G/B = %.2f / %.2f / %.2f%n",
                    (pv.red / totalPixelValues) * 100.0,
                    (pv.green / totalPixelValues) * 100.0,
                    (pv.blue / totalPixelValues) * 100.0);
        }

        BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        dstImage.setRGB(0, 0, w, h, dst, 0, w);

        File dstFile = new File(dstName);
        ImageIO.write(dstImage, "jpg", dstFile);

        System.out.println("Wrote output image: " + dstName);
    }

    private static void printUsage() {
        System.out.println("Usage: [sp] input-file output-file");
    }

    /** Creates a square Gaussian kernel with size^2 elements. */
    public static float[][] createGaussianKernel(final int size) {
        // initialize kernel
        assert (size > 0) && ((size & 1) == 1) : "Kernel must have odd size";

        final float[][] kernelMatrix = new float[size][size];

        // calculate gaussian blur matrix
        double sigma = 5.0;
        double mean = size / 2;
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                float v = (float) (Math.exp(-0.5 * (Math.pow((x - mean) / sigma, 2.0) + Math.pow((y - mean) / sigma, 2.0)))
                        / (2 * Math.PI * sigma * sigma));
                kernelMatrix[x][y] = v;
            }
        }

        // normalize matrix values
        double sum = 0.0;
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                sum += kernelMatrix[x][y];
            }
        }
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                kernelMatrix[x][y] /= sum;
            }
        }

        return kernelMatrix;
    }

    /**
     * Perform a parallel convolution
     *
     * @param src      the input image pixels
     * @param startRow the starting row of the image
     * @param numRows  the number of rows to process
     * @param dst      the output image pixels
     * @return ChannelValues for all pixels processed by the blur
     */
    public ChannelValues parallelConvolution(float[][] kernel, Int2D src, int startRow, int numRows, Int2D dst) {

        // TODO: fill in code here...
        return null;
    }

    /**
     * Perform a sequential convolution
     *
     * @param src the input image pixels
     * @param dst the output image pixels
     * @return ChannelValues for all pixels processed by the blur
     */
    public ChannelValues sequentialConvolution(float[][] kernel, Int2D src, Int2D dst) {
        ConvolutionTask ic = new ConvolutionTask(kernel, src, 0, src.height, dst);
        return ic.convolute();
    }
}
