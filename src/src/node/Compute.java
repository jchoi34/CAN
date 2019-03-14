package node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

public interface Compute extends Remote {
    PathHistory insertKeyword(String keyword, int[] coords, PathHistory visited) throws RemoteException;
    PathHistory searchKeyword(String keyword, int[] coords, PathHistory visited) throws RemoteException;
    PathHistory view(PathHistory visited) throws RemoteException;
    PathHistory view(PathHistory visited, String receiver) throws RemoteException;
    String join() throws RemoteException;
    String join(String joining) throws RemoteException;
    String leave() throws RemoteException;
    void setKeywords(HashSet<String> keywords) throws RemoteException;
    void addZone(Zone zone) throws RemoteException;
    String splitZone(String node) throws RemoteException;
    void setNeighbors(HashMap<String, Neighbor> neighbors) throws RemoteException;
    void addNeighbor(Neighbor n) throws RemoteException;
    void removeNeighbor(String s) throws RemoteException;
    boolean updateZone(Zone zone, Zone zoneToUpdate) throws RemoteException;
    boolean checkKeywordAndAdd(String keyword) throws RemoteException;
    boolean checkAddNeighbor(Neighbor n) throws RemoteException;
    String getString() throws RemoteException;
    boolean alreadyJoined() throws RemoteException;
    void leaveTellBootstrap(String s) throws RemoteException;
    int[] hash(String keyword) throws RemoteException;
    void updateNeighbor(String name, Zone oldZone, Zone newZone) throws RemoteException;
    boolean canSplit() throws RemoteException;
}
