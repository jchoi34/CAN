package node;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.Map.Entry;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class Node implements Compute {
    private static final long serialVersionUID = 227L;
    public static final String bootstrapNode = "n0000";
    private HashSet<String> keywords;
    private HashMap<String, Neighbor> neighbors;
    private ArrayList<Zone> zones;
    private String name;
    private HashSet<String> nodes;
    private String ipAddress;

    public Node(String name) {
        super();
        this.keywords = new HashSet<>(50);
        this.neighbors = new HashMap<>(5);
        this.zones = new ArrayList<>(10);
        this.name = name;
        try {
            this.ipAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            this.ipAddress = "failed to get ip";
        }

        // if we are the bootstrap node acquire the first zone
        // and maintain a list of nodes believed to be in the system
        if (name.equals(bootstrapNode) && isBootstrap()) {
            bootstrap();
        }
    }

    // Only the bootstrap server should call this
    public void bootstrap() {
        System.out.println("Bootstrapping");
        this.nodes = new HashSet<>(10);
        System.out.println(this.name);
        this.nodes.add(this.name);
        this.zones.add(new Zone(0, 10, 0, 10));
    }

    // TODO: change this
    // just return true for now
    public boolean isBootstrap() {

        return true;
    }

    // Helper class for greedy routing
    private class ZoneComparator implements Comparator<Neighbor> {
        int[] coords;

        public ZoneComparator(int[] coords) {
            this.coords = coords;
        }

        // used by the min heap. Neighbors should be ordered by the
        // distance from their center to the coordinate
        @Override
        public int compare(Neighbor neighbor1, Neighbor neighbor2) {
            Zone tmp;
            int dist;
            int distance1 = Integer.MAX_VALUE;
            int distance2 = Integer.MAX_VALUE;
            Iterator<Zone> zoneIterator1 = neighbor1.getZones().iterator();
            Iterator<Zone> zoneIterator2 = neighbor2.getZones().iterator();

            // distance from the middle of a zone to a point in the zone
            // using the distance formula sqrt((x2 - x1)^2 + (y2 - y1)^2)
            while (zoneIterator1.hasNext()) {
                tmp = zoneIterator1.next();
                if (tmp.contains(this.coords))
                    return -1;
                dist = (int) Math.sqrt(Math.pow(coords[0] - ((tmp.x2 - tmp.x1) / 2), 2)
                        + Math.pow(coords[1] - ((tmp.y2 - tmp.y1) / 2), 2));
                if (dist < distance1) {
                    distance1 = dist;
                }
            }

            while (zoneIterator2.hasNext()) {
                tmp = zoneIterator2.next();
                if (tmp.contains(this.coords))
                    return 1;
                dist = (int) Math.sqrt(Math.pow(coords[0] - ((tmp.x2 - tmp.x1) / 2), 2)
                        + Math.pow(coords[1] - ((tmp.y2 - tmp.y1) / 2), 2));
                if (dist < distance2) {
                    distance2 = dist;
                }
            }

            if (distance1 < distance2)
                return -1;
            else if (distance1 > distance2)
                return 1;
            else
                return 0;
        }

        @Override
        public boolean equals(Object neighbor) {
            return this.equals(neighbor);
        }
    }

    // get the neighbors in order by distance to the coordinate
    public Iterator<Neighbor> checkNeighbors(int[] coords) {
        Neighbor neighbor;
        Iterator<Neighbor> iter = neighbors.values().iterator();
        PriorityQueue<Neighbor> closestNeighbors = new PriorityQueue<>(5, new ZoneComparator(coords));

        while (iter.hasNext()) {
            neighbor = iter.next();
            closestNeighbors.add(neighbor);
        }
        return closestNeighbors.iterator();
    }

    // search for the zone and insert the coordinate using depth first search but
    // with a greedy heuristic.
    public PathHistory insertKeyword(String keyword, int[] coords, PathHistory history) {
        Iterator<Neighbor> neighborIterator;
        Neighbor neighbor;
        Registry remote;
        Compute comp;

        // add keyword to database then send route back to original sender
        if (checkZones(coords)) {
            this.keywords.add(keyword);
            history.path.push("Destination: " + this.name + " (" + this.ipAddress + ")");
            history.result = "found";
            return history;
        } else { // route to nearest neighbor using modified df search
            history.visited.add(this.name);
            neighborIterator = checkNeighbors(coords);
            while (neighborIterator.hasNext()) {
                neighbor = neighborIterator.next();
                if (!history.visited.contains(neighbor.name)) {
                    history.visited.add(neighbor.name);
                    try {
                        remote = LocateRegistry.getRegistry(neighbor.name);
                        comp = (Compute) remote.lookup("Node");
                        history = comp.insertKeyword(keyword, coords, history);
                        if (history.result.equals("found")) {
                            history.path.push(this.name + " (" + this.ipAddress + ") -> ");
                            return history;
                        }
                    } catch (RemoteException | NotBoundException r) {
                        history.result = "not found";
                        return history;
                    }
                }
            }
            history.result = "not found";
            return history;
        }
    }

    // search for the zone that might contain the keyword using depth first search
    // but with a greedy heuristic.
    public PathHistory searchKeyword(String keyword, int[] coords, PathHistory history) {
        Iterator<Neighbor> neighborIterator;
        Neighbor neighbor;
        Registry remote;
        Compute comp;

        // search for keyword
        if (checkZones(coords)) {
            if (this.keywords.contains(keyword)) {
                history.path.push("Destination: " + this.name + " (" + this.ipAddress + ")");
                history.result = "found";
            } 
                return history;
        } else { // route to nearest neighbor using modified df search
            history.visited.add(this.name);
            neighborIterator = checkNeighbors(coords);
            while (neighborIterator.hasNext()) {
                neighbor = neighborIterator.next();
                if (!history.visited.contains(neighbor.name)) {
                    history.visited.add(neighbor.name);
                    try {
                        remote = LocateRegistry.getRegistry(neighbor.name);
                        comp = (Compute) remote.lookup("Node");
                        history = comp.searchKeyword(keyword, coords, history);
                        if (history.result.equals("found")) {
                            history.path.push(this.name + " (" + this.ipAddress + ") -> ");
                            return history;
                        } 
                    } catch (RemoteException | NotBoundException r) {
                        history.result = "not found";
                        return history;
                    }
                }
            }
            history.result = "not found";
            return history;
        }
    }

    // use a depth first search to find all peers in the system
    public PathHistory view(PathHistory history) {
        Registry remote;
        Compute comp;
        String s;
        Iterator<String> neighborIterator = this.neighbors.keySet().iterator();
        history.results.add(this.toString());
        history.visited.add(this.name);

        while (neighborIterator.hasNext()) {
            s = neighborIterator.next();
            if (!history.visited.contains(s)) {
                try {
                    remote = LocateRegistry.getRegistry(s);
                    comp = (Compute) remote.lookup("Node");
                    history = comp.view(history);

                } catch (RemoteException | NotBoundException r) {
                    r.printStackTrace();
                }
            }
        }
        return history;
    }

    // use depth first search to find the target node
    public PathHistory view(PathHistory visited, String receiver) {
        Registry remote;
        Compute comp;

        if (this.name.equals(receiver)) {
            visited.result = this.toString();
            return visited;
        } else {
            String s;
            Iterator<String> neighborIterator = this.neighbors.keySet().iterator();
            visited.add(this.name);

            while (neighborIterator.hasNext()) {
                s = neighborIterator.next();
                if (!visited.contains(s)) {
                    try {
                        remote = LocateRegistry.getRegistry(s);
                        comp = (Compute) remote.lookup("Node");
                        visited = comp.view(visited, receiver);
                        if (!visited.result.equals("Not found")) {
                            return visited;
                        }
                    } catch (RemoteException | NotBoundException r) {
                        r.printStackTrace();
                    }
                }
            }
            visited.result = "Not found";
            return visited;
        }
    }

    // make every possible host join
    public String join() {
        String result = "";
        String tmp = "";
        String[] nodes = {"n0000", "n0001", "n0002", "n0003", "n0004", "n0005", "n0006", "n0007", "n0008", "n0009"};
        for (String s : nodes) {
                if (tmp.equals("Join failed")) {
                    result += s + " failed to join. ";
                } else {
                    result += s + " Joined successfully. ";
                    tmp = this.join(s);
                }
        }
        return result;
    }

    // make the node "joining" send a join request to the bootstrap
    public synchronized String join(String joining) {
        Registry remote;
        Compute comp;
        Iterator<String> nodesIter;
        int random;
        String node = "";
        String result;

        if (!this.name.equals(bootstrapNode)) {
            try {
                remote = LocateRegistry.getRegistry(bootstrapNode);
                comp = (Compute) remote.lookup("Node");
                return comp.join(joining);
            } catch (RemoteException | NotBoundException r) {
                return "Join failed";
            }
        } else {
            try {
                remote = LocateRegistry.getRegistry(joining);
                comp = (Compute) remote.lookup("Node");
                if (comp.alreadyJoined()) {
                    return "already in the CAN";
                } else if (this.nodes.contains(joining)) {
                    return "already in the CAN";
                } else if (this.nodes.size() == 10) {
                    return "max capacity reached";
                }

                do {
                    // give the joining node a random node (maybe itself) to split its zone with
                    random = (int) (Math.random() * this.nodes.size());
                    int i = 0;
                    nodesIter = this.nodes.iterator();
                    while (nodesIter.hasNext() && i < random + 1) {
                        node = nodesIter.next();
                        i++;
                    }
                    remote = LocateRegistry.getRegistry(node);
                    comp = (Compute) remote.lookup("Node");
                } while (!comp.canSplit());

                if (!node.equals(this.name)) { // if we aren't splitting ourself
                    // make random node split with joining node
                    remote = LocateRegistry.getRegistry(node);
                    comp = (Compute) remote.lookup("Node");

                    result = comp.splitZone(joining);
                } else { // split our zone with the joining node
                    result = this.splitZone(joining);
                }
                if (!result.equals("Join failed")) {
                    this.nodes.add(joining);
                }
                return result;
            } catch (RemoteException | NotBoundException r) {
                return "Join failed";
            }
        }
    }

    public boolean canSplit() {
        Iterator<Zone> zoneIterator = this.zones.iterator();
        while(zoneIterator.hasNext()) {
            if(zoneIterator.next().getArea() > 2)
                return true;
        }
        return false;
    }

    public boolean isNeighbor(Zone zone, Neighbor neighbor) {
        Iterator<Zone> iter = neighbor.zones.iterator();
        Zone tmp;
        while (iter.hasNext()) {
            tmp = iter.next();
            // smaller or equal width zone is directly below new neighbor
            if (zone.x1 >= tmp.x1 && zone.x2 <= tmp.x2 && zone.y2 == tmp.y1) {
                return true;
            }
            // larger or equal width zone is directly below new neighbor
            else if (zone.x1 <= tmp.x1 && zone.x2 >= tmp.x2 && zone.y2 == tmp.y1) {
                return true;
            }
            // part of the zone's left side is below new neighbor
            else if (zone.x1 > tmp.x1 && zone.x1 < tmp.x2 && zone.y2 == tmp.y1) {
                return true;
            }
            // part of the zone's right side is below new neighbor
            else if (zone.x2 > tmp.x1 && zone.x2 < tmp.x2 && zone.y2 == tmp.y1) {
                return true;
            }
            // smaller or equal width zone is directly above new neighbor
            else if (zone.x1 >= tmp.x1 && zone.x2 <= tmp.x2 && zone.y1 == tmp.y2) {
                return true;
            }
            // larger or equal width zone is directly above new neighbor
            else if (zone.x1 <= tmp.x1 && zone.x2 >= tmp.x2 && zone.y1 == tmp.y2) {
                return true;
            }
            // part of the zone's left side is above new neighbor
            else if (zone.x1 > tmp.x1 && zone.x1 < tmp.x2 && zone.y1 == tmp.y2) {
                return true;
            }
            // part of the zone's right side is above new neighbor
            else if (zone.x2 > tmp.x1 && zone.x2 < tmp.x2 && zone.y1 == tmp.y2) {
                return true;
            }
            // shorter or equal height zone is to the left of new neighbor
            else if (zone.x1 == tmp.x2 && zone.y1 >= tmp.y1 && zone.y2 <= tmp.y2) {
                return true;
            }
            // taller or equal height zone is to the left of new neighbor
            else if (zone.x1 == tmp.x2 && zone.y1 <= tmp.y1 && zone.y2 >= tmp.y2) {
                return true;
            }
            // part of zone's bottom part is to the left of new neighbor
            else if (zone.x1 == tmp.x2 && zone.y1 > tmp.y1 && zone.y1 < tmp.y2) {
                return true;
            }
            // part of zone's top part is to the left of new neighbor
            else if (zone.x1 == tmp.x2 && zone.y2 > tmp.y1 && zone.y2 < tmp.y2) {
                return true;
            }
            // shorter or equal height zone is to the right of new neighbor
            else if (zone.x2 == tmp.x1 && zone.y1 >= tmp.y1 && zone.y2 <= tmp.y2) {
                return true;
            }
            // taller or equal height zone is to the right of new neighbor
            else if (zone.x2 == tmp.x1 && zone.y1 <= tmp.y1 && zone.y2 >= tmp.y2) {
                return true;
            }
            // part of zone's bottom part is to the right of new neighbor
            else if (zone.x2 == tmp.x1 && zone.y1 > tmp.y1 && zone.y1 < tmp.y2) {
                return true;
            }
            // part of zone's top part is to the right of new neighbor
            else if (zone.x2 == tmp.x1 && zone.y2 > tmp.y1 && zone.y2 < tmp.y2) {
                return true;
            }
        }
        return false;
    }

    // get a neighbor list for a new node and update those neighbors of the change
    public HashMap<String, Neighbor> calculateNewNeighbors(String node, Zone zone, boolean isNew) {
        HashMap<String, Neighbor> newNodeNeighbors = new HashMap<>(5);
        Iterator<Neighbor> iter = this.neighbors.values().iterator();
        Neighbor neighbor;
        Registry remote;
        Compute comp;

        while (iter.hasNext()) {
            neighbor = iter.next();
            if (isNeighbor(zone, neighbor)) {
                newNodeNeighbors.put(neighbor.name, neighbor);
                if (isNew) {
                    try {
                        remote = LocateRegistry.getRegistry(neighbor.name);
                        comp = (Compute) remote.lookup("Node");
                        comp.addNeighbor(new Neighbor(node, zone));
                    } catch (RemoteException | NotBoundException r) {
                        r.printStackTrace();
                    }
                }
            } else if (!isNew) {
                try {
                    remote = LocateRegistry.getRegistry(neighbor.name);
                    comp = (Compute) remote.lookup("Node");
                    comp.removeNeighbor(node);
                } catch (RemoteException | NotBoundException r) {
                    r.printStackTrace();
                }
            }
        }

        return newNodeNeighbors;
    }

    public void updateNeighbor(String name, Zone oldZone, Zone newZone) {
        Neighbor neighbor = this.neighbors.get(name);

        if(neighbor != null) {
            Iterator<Zone> iterator = neighbor.zones.iterator();
            Zone zone;
            while (iterator.hasNext()) {
                zone = iterator.next();
                if (zone.equals(oldZone)) {
                    zone.updateZone(newZone.x1, newZone.x2, newZone.y1, newZone.y2);
                    return;
                }
            }
        }
    }

    // split a zone between two nodes and update the neighbors
    public String splitZone(String joiningNode) {
        Zone zone;
        Registry remote;
        Compute comp;
        HashSet<String> newNodeKeywords = null;
        Zone newNodeZone;
        Iterator<String> iter;
        Iterator<Neighbor> iterN;
        HashMap<String, Neighbor> newNodeNeighbors;
        int oldX1, oldX2, oldY1, oldY2, newX1, newX2, newY1, newY2;

        if (this.zones.size() == 1) {
            zone = this.zones.get(0);
            if (zone.isSquare()) {
                oldX1 = zone.x1;
                oldX2 = ((zone.x2 - zone.x1) / 2) + zone.x1;
                oldY1 = zone.y1;
                oldY2 = zone.y2;
                newX1 = ((zone.x2 - zone.x1) / 2) + zone.x1;;
                newX2 = zone.x2;
                newY1 = zone.y1;
                newY2 = zone.y2;
            } else {
                oldX1 = zone.x1;
                oldX2 = zone.x2;
                oldY1 = zone.y1;
                oldY2 = ((zone.y2 - zone.y1) / 2) + zone.y1;
                newX1 = zone.x1;
                newX2 = zone.x2;
                newY1 = ((zone.y2 - zone.y1) / 2) + zone.y1;
                newY2 = zone.y2;
            }
            try {
                // give this new zone to the new node
                remote = LocateRegistry.getRegistry(joiningNode);
                comp = (Compute) remote.lookup("Node");
                newNodeZone = new Zone(newX1, newX2, newY1, newY2);
                comp.addZone(newNodeZone);

                // update this zone in our neighbors' neighbors list
                iterN = this.neighbors.values().iterator();
                while(iterN.hasNext()) {
                    remote = LocateRegistry.getRegistry(iterN.next().name);
                    comp = (Compute) remote.lookup("Node");
                    comp.updateNeighbor(this.name, zone, new Zone(oldX1, oldX2, oldY1, oldY2));
                }
                // update the zone of the split node
                zone.updateZone(oldX1, oldX2, oldY1, oldY2);

                iter = this.keywords.iterator();
                newNodeKeywords = new HashSet<>(50);

                // add the eligible key words
                String tmp;
                while (iter.hasNext()) {
                    if (newNodeZone.contains(this.hash((tmp = iter.next())))) {
                        newNodeKeywords.add(tmp);
                    }
                }
                remote = LocateRegistry.getRegistry(joiningNode);
                comp = (Compute) remote.lookup("Node");
                comp.setKeywords(newNodeKeywords);

                // calculate the new node's neighbors while telling them to add us
                newNodeNeighbors = this.calculateNewNeighbors(joiningNode, newNodeZone, true);
                newNodeNeighbors.put(this.name, new Neighbor(this.name, new Zone(oldX1, oldX2, oldY1, oldY2)));
                comp.setNeighbors(newNodeNeighbors);

                // remove its now old entries
                iter = newNodeKeywords.iterator();
                while (iter.hasNext()) {
                    this.keywords.remove(iter.next());
                }

                // calculate the new neighbor's for the now split zone while notifying them of
                // the change
                this.neighbors = this.calculateNewNeighbors(this.name, zone, false);
                if(!joiningNode.equals(this.name))
                    this.neighbors.put(joiningNode, new Neighbor(joiningNode, newNodeZone));

                return comp.getString();
            } catch (RemoteException | NotBoundException r) {
                return "Join failed";
            }
        } else if (this.zones.size() > 1) {
            zone = this.zones.remove(1);
            try {
                // give away this extra zone to the joining node
                remote = LocateRegistry.getRegistry(joiningNode);
                comp = (Compute) remote.lookup("Node");
                comp.addZone(zone);

                iter = this.keywords.iterator();

                HashSet<String> removeLater = new HashSet<>(50);

                // add the eligible key words
                String tmp;
                while (iter.hasNext()) {
                    tmp = iter.next();
                    if (comp.checkKeywordAndAdd(tmp)) {
                        removeLater.add(tmp);
                    }
                }

                // remove the now non eligible keywords
                iter = removeLater.iterator();
                while (iter.hasNext()) {
                    this.keywords.remove(iter.next());
                }

                // calculate the new node's neighbors while notifying its new neighbors of the
                // change
                iterN = this.neighbors.values().iterator();
                while (iterN.hasNext()) {
                    comp.checkAddNeighbor(iterN.next());
                }

                // calculate the new neighbor's for the now split zone while notifying them of
                // the change
                Neighbor neighbor;
                boolean valid;
                removeLater.clear();
                iterN = this.neighbors.values().iterator();
                Iterator<Zone> iterZ;
                while (iterN.hasNext()) {
                    iterZ = this.zones.iterator();
                    neighbor = iterN.next();
                    valid = false;
                    while (iterZ.hasNext()) {
                        if (isNeighbor(iterZ.next(), neighbor)) {
                            valid = true;
                            break;
                        }
                    }
                    if (!valid) {
                        removeLater.add(neighbor.name);
                    }
                }
                iter = removeLater.iterator();
                while (iter.hasNext()) {
                    tmp = iter.next();
                    this.neighbors.remove(tmp);
                    remote = LocateRegistry.getRegistry(tmp);
                    comp = (Compute) remote.lookup("Node");
                    comp.removeNeighbor(this.name);
                }

                remote = LocateRegistry.getRegistry(joiningNode);
                comp = (Compute) remote.lookup("Node");
                return comp.getString();
            } catch (RemoteException | NotBoundException r) {
                return "Join failed";
            }
        }
        return "Join failed";
    }

    // leave the CAN thereby giving up our zones, database, and neighbors to
    // eligible nodes
    public synchronized String leave() {
        if (this.zones.size() == 0)
            return "already left the CAN";
        Neighbor neighbor;
        Zone leavingZone;
        Iterator<Neighbor> neighbors;
        ArrayList<Entry<Neighbor, Zone>> arr;
        Iterator<Zone> neighborZones;
        Zone growingZone;
        int smallestZone;
        Iterator<Zone> leavingZones = this.zones.iterator();
        Iterator<String> leavingKeywords;
        int sum;
        Neighbor neighborToGrow = null;
        Registry remote;
        Compute comp;

        merged:
        while (leavingZones.hasNext()) {
            smallestZone = Integer.MAX_VALUE;
            leavingZone = leavingZones.next();
            neighbors = this.neighbors.values().iterator();
            while (neighbors.hasNext()) {
                neighbor = neighbors.next();
                neighborZones = neighbor.zones.iterator();
                sum = 0;
                while (neighborZones.hasNext()) {
                    growingZone = neighborZones.next();
                    if (mergeProducesValidZone(leavingZone, growingZone)) {
                        if (mergeZones(neighbor.name, leavingZone, growingZone))
                            continue merged;
                        else {
                            return "Leave failed";
                        }
                    } else { // calculate the smallest zone
                        sum += growingZone.getArea();
                        if (sum < smallestZone) {
                            smallestZone = sum;
                            neighborToGrow = neighbor;
                        }
                    }
                }
            }

            // if this is reached no neighbor zone can merge with this zone
            // make the neighbor with the smallest overall zone size hold this zone
            // add the zone to this neighbor and make it gain the new eligible neighbors and
            // keywords
            // new neighbors are updated of change
            if (neighborToGrow != null) {
                try {
                    remote = LocateRegistry.getRegistry(neighborToGrow.name);
                    comp = (Compute) remote.lookup("Node");
                    comp.addZone(leavingZone);
                    neighbors = this.neighbors.values().iterator();
                    while (neighbors.hasNext()) {
                        comp.checkAddNeighbor(neighbors.next());
                    }
                    leavingKeywords = this.keywords.iterator();
                    while (leavingKeywords.hasNext()) {
                        comp.checkKeywordAndAdd(leavingKeywords.next());
                    }
                } catch (RemoteException | NotBoundException r) {
                    return "Leave failed";
                }
            }
        }

        String result = "";
        // clean up this node's state and get info of affected peers
        Iterator<String> neighborNames = this.neighbors.keySet().iterator();
        while (neighborNames.hasNext()) {
            try {
                remote = LocateRegistry.getRegistry(neighborNames.next());
                comp = (Compute) remote.lookup("Node");
                comp.removeNeighbor(this.name);
                result += comp.getString() + "\n";
            } catch (RemoteException | NotBoundException r) {
                return "Leave failed";
            }
        }
        try {
            remote = LocateRegistry.getRegistry(bootstrapNode);
            comp = (Compute) remote.lookup("Node");
            comp.leaveTellBootstrap(this.name);
        } catch (RemoteException | NotBoundException r) {
            return "Leave failed";
        }
        this.neighbors.clear();
        this.zones.clear();
        this.keywords.clear();

        return result;
    }

    // give this node's zone, database, and neighbors to a neighbor
    public boolean mergeZones(String growingNode, Zone leavingZone, Zone growingZone) {
        Registry remote;
        Compute comp;
        Iterator<String> keywordsLeaving;
        Iterator<Neighbor> neighborIterator;
        boolean result;
        try {
            // give this node's zone to the neighbor
            // node's neighbors should be notified in the code below
            remote = LocateRegistry.getRegistry(growingNode);
            comp = (Compute) remote.lookup("Node");
            result = comp.updateZone(leavingZone, growingZone);

            // give this node's valid keywords to neighbor
            keywordsLeaving = this.keywords.iterator();
            while (keywordsLeaving.hasNext()) {
                comp.checkKeywordAndAdd(keywordsLeaving.next());
            }

            // give this node's valid neighbors to neighbor
            // and tell them to add the new neighbor
            neighborIterator = this.neighbors.values().iterator();
            while (neighborIterator.hasNext()) {
                result = comp.checkAddNeighbor(neighborIterator.next());
            }
            return result;
        } catch (RemoteException | NotBoundException r) {
            return false;
        }
    }

    // helper method for leave command
    public boolean checkKeywordAndAdd(String keyword) {
        if (checkZones(hash(keyword))) {
            this.keywords.add(keyword);
            return true;
        }
        return false;
    }

    // update the zone
    public boolean updateZone(Zone leavingZone, Zone zoneToUpdate) {
        Iterator<Zone> zones = this.zones.iterator();
        Zone tmp;
        while (zones.hasNext()) {
            tmp = zones.next();
            if (tmp.equals(zoneToUpdate)) {
                // grow to the right
                if (leavingZone.y2 == zoneToUpdate.y2 && leavingZone.x2 > zoneToUpdate.x2)
                    tmp.updateZone(zoneToUpdate.x1, leavingZone.x2, zoneToUpdate.y1, zoneToUpdate.y2);
                    // grow to the left
                else if (leavingZone.y2 == zoneToUpdate.y2 && leavingZone.x2 < zoneToUpdate.x2)
                    tmp.updateZone(leavingZone.x1, zoneToUpdate.x2, zoneToUpdate.y1, zoneToUpdate.y2);
                    // grow upwards
                else if (leavingZone.x2 == zoneToUpdate.x2 && leavingZone.y2 > zoneToUpdate.y2)
                    tmp.updateZone(zoneToUpdate.x1, zoneToUpdate.x2, zoneToUpdate.y1, leavingZone.y2);
                    // grow downwards
                else if (leavingZone.x2 == zoneToUpdate.x2 && leavingZone.y2 < zoneToUpdate.y2)
                    tmp.updateZone(zoneToUpdate.x1, zoneToUpdate.x2, leavingZone.y1, zoneToUpdate.y2);
                else
                    return false;

                Registry remote;
                Compute comp;
                Iterator<Neighbor> neighborIterator = this.neighbors.values().iterator();
                Neighbor neighbor;
                while (neighborIterator.hasNext()) {
                    neighbor = neighborIterator.next();
                    try {
                        remote = LocateRegistry.getRegistry(neighbor.name);
                        comp = (Compute) remote.lookup("Node");
                        comp.updateNeighbor(this.name, zoneToUpdate, tmp);
                    } catch (RemoteException | NotBoundException r) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // check if two zones can merge
    public boolean mergeProducesValidZone(Zone zone1, Zone zone2) {
        if (zone1.x1 == zone2.x2 && (zone1.y1 == zone2.y1) && (zone1.y2 == zone2.y2))
            return true;
        else if (zone1.x2 == zone2.x1 && (zone1.y1 == zone2.y1) && (zone1.y2 == zone2.y2))
            return true;
        else if (zone1.y2 == zone2.y1 && (zone1.x1 == zone2.x1) && (zone1.x2 == zone2.x2))
            return true;
        else if (zone1.y1 == zone2.y2 && (zone1.x1 == zone2.x1) && (zone1.x2 == zone2.x2))
            return true;
        else
            return false;
    }

    public void addNeighbor(Neighbor n) {
        if(!n.name.equals(this.name))
            this.neighbors.put(n.name, n);
    }

    public void removeNeighbor(String node) {
        this.neighbors.remove(node);
    }

    // check if valid neighbor before adding
    // also tell that neighbor to add us
    public boolean checkAddNeighbor(Neighbor n) {
        Registry remote;
        Compute comp;
        Iterator<Zone> zoneIterator = this.zones.iterator();
        Zone zone;

        while (zoneIterator.hasNext()) {
            if (isNeighbor((zone = zoneIterator.next()), n)) {
                if(!n.name.equals(this.name))
                    this.neighbors.put(n.name, n);
                try {
                    remote = LocateRegistry.getRegistry(n.name);
                    comp = (Compute) remote.lookup("Node");
                    comp.addNeighbor(new Neighbor(this.name, zone));
                    return true;
                } catch (RemoteException | NotBoundException r) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setNeighbors(HashMap<String, Neighbor> neighbors) {
        this.neighbors = neighbors;
    }

    public void setKeywords(HashSet<String> keywords) {
        this.keywords = keywords;
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public String getName() {
        return name;
    }

    // check if this node has the keyword's designated zone
    public boolean checkZones(int[] coords) {
        java.util.ListIterator<Zone> iter = this.zones.listIterator();
        Zone zone;
        while (iter.hasNext()) {
            zone = iter.next();
            if (zone.contains(coords))
                return true;
        }
        return false;
    }

    // compute the zone location of a keyword
    // using integers so some zones may not split completely evenly
    public int[] hash(String keyword) {
        int x = 0, y = 0;
        char[] string = keyword.toCharArray();
        for (int i = 0; i < string.length; i++) {
            if ((i + 1) % 2 == 1)
                x += string[i];
            else
                y += string[i];
        }
        int[] arr = {x % 10, y % 10};
        return arr;
    }

    @Override
    public String toString() {
        return "Node: " + this.name + " ip address: " + this.ipAddress + " Zones: " + getZones() + " Neighbors: " + getNeighbors()
                + " Keywords: " + keywords.toString();
    }

    public String getString() {
        return this.toString();
    }

    public boolean alreadyJoined() {
        return this.zones.size() > 0;
    }

    public String getNeighbors() {
        Set<String> names = neighbors.keySet();
        Iterator<String> iter = names.iterator();
        String result = "", name;
        while (iter.hasNext()) {
            name = iter.next();
            result += name;
            if (iter.hasNext())
                result += " ";
        }
        return result;
    }

    public void leaveTellBootstrap(String node) {
        this.nodes.remove(node);
    }

    public String getZones() {
        String s = "";
        java.util.ListIterator<Zone> iter = zones.listIterator();
        Zone zone;

        while (iter.hasNext()) {
            zone = iter.next();
            s += zone.toString();
            if (iter.hasNext())
                s += "; ";
        }

        return s;
    }
}
