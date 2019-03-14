package node;

import java.io.Serializable;
import java.util.ArrayList;

public class Neighbor implements Serializable {
	public String name;
    public ArrayList<Zone> zones;

    public Neighbor(String name) {
    	super();
        this.zones = new ArrayList<Zone>(4);
        this.name = name;
    }

    public Neighbor(String name, Zone zone) {
        super();
        this.zones = new ArrayList<Zone>(4);
        this.zones.add(zone);
        this.name = name;
    }

    public ArrayList<Zone> getZones() {
        return this.zones;
    }

    
 // check if this node has the keyword's designated zone
    public boolean checkZones(int[] coords) {
    	java.util.ListIterator<Zone> iter = zones.listIterator();
    	Zone zone;
    	while(iter.hasNext()) {
    		zone = iter.next();
    		if(zone.contains(coords))
    			return true;
    	}
    	return false;
    }
}
