package node;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Stack;

public class PathHistory implements Serializable {
    HashSet<String> visited;
    String result;
    HashSet<String> results;
    Stack<String> path;

    // use for view single peer
    public PathHistory () {
        super();
        this.visited = new HashSet<String>(10);
        this.result = "";
    }

    // use for view all peers
    public PathHistory (HashSet<String> results) {
        super();
        this.visited = new HashSet<String>(10);
        this.results = results;
    }

    // use for search and insert
    public PathHistory (HashSet<String> results, Stack<String> path) {
        super();
        this.visited = new HashSet<String>(10);
        this.result = "";
        this.path = path;
    }


    public void add(String s) {
        this.visited.add(s);
    }

    public boolean contains(String s) {
        return this.visited.contains(s);
    }
}
