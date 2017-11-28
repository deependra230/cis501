package cis501.parallelism;

public class Kernel {

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
}
