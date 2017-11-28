package cis501.parallelism;

/** wrapper class to provide a 2D view of a 1D int array */
public class Int2D {
    public final int[] dataArray;
    public final int width;
    public final int height;

    public Int2D(int[] dataArray, int width, int height) {
        assert dataArray.length == width * height;
        this.dataArray = dataArray;
        this.width = width;
        this.height = height;
    }

    public int get(int x, int y) {
        return dataArray[(y * width) + x];
    }

    public void set(int x, int y, int value) {
        dataArray[(y * width) + x] = value;
    }
}
