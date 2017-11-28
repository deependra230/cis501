package cis501.parallelism;

public class ChannelValues {
    public long red = 0, green = 0, blue = 0;

    public void merge(ChannelValues cv) {
        assert this != cv : "can't merge with myself!";
        this.red += cv.red;
        this.green += cv.green;
        this.blue += cv.blue;
    }
}
