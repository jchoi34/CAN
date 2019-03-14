package node;

import java.io.Serializable;

public class Zone implements Serializable {
    public int x1, x2, y1, y2;

    public Zone(int x1, int x2, int y1, int y2) {
    	super();
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }
    
    public boolean contains(int[] coords) {
    	if(coords[0] >= x1 && coords[0] <= x2 && coords[1] >= y1 && coords[1] <= y2)
    		return true;
    	return false;
    }
    
    @Override
    public String toString() {
    	return "x1: " + x1 + " x2: " + x2 + " y1: " + y1 + " y2: " + y2;
    }
    
    public void updateZone(int x1, int x2, int y1, int y2) {
    	this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }

    public boolean equals(Zone zone) {
        if(this.x1 == zone.x1 && this.x2 == zone.x2 && this.y1 == zone.y1 && this.y2 == zone.y2)
            return true;
        return false;
    }

    public boolean isSquare() {
        return x2 - x1 == y2 - y1;
    }

    public int getArea() {
        return (x2 - x1) * (y2 - y1);
    }
}
