package cis501.parallelism;

public interface IConvolutionRunner {

    /** @return the names of the group members for this assignment. */
    public String[] groupMembers();

    /**
     * Perform a parallel convolution
     *
     * @param kernel   the matrix for the kernel to use
     * @param src      the input image pixels
     * @param startRow the starting row of the image
     * @param numRows  the number of rows to process
     * @param dst      the output image pixels
     * @return ChannelValues for all pixels processed by the blur
     */
    ChannelValues parallelConvolution(float[][] kernel, Int2D src, int startRow, int numRows, Int2D dst);

    /**
     * Perform a sequential convolution
     *
     * @param kernel the matrix for the kernel to use
     * @param src    the input image pixels
     * @param dst    the output image pixels
     * @return ChannelValues for all pixels processed by the blur
     */
    ChannelValues sequentialConvolution(float[][] kernel, Int2D src, Int2D dst);

}
