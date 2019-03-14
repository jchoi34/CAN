package node;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    public static void main(String args[]) {
        Registry remote;
        Compute node;
        String name;

        name = args[0];
        node = new Node(name);
        try {
            Compute stub = (Compute) UnicastRemoteObject.exportObject(node, 1099);
            remote = LocateRegistry.createRegistry(1099);
            remote.rebind("Node", stub);
            System.out.println(remote.toString());
        } catch (RemoteException r) {
            r.printStackTrace();
        }
    }
}
