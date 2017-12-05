
public class Segment {

	private final int quality;
	private final String seg;
	private final int dimension;
	
	public Segment(int quality, String seg, int dimension) {
		
		this.quality = quality;
		this.seg = seg;
		this.dimension = dimension;
	}
	
	public int getQuality() {
		return quality;
	}
	
	public String getSeg() {
		return seg;
	}
	
	public int getDimension() {
		return dimension;
	}
	
}
