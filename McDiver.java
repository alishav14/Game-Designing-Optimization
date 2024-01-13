package diver;

import datastructures.SlowPQueue;
import game.*;
import graph.ShortestPaths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/** This is the place for your implementation of the {@code SewerDiver}.
 */
public class McDiver implements SewerDiver {
    private ArrayList<Long> visitedSeek = new ArrayList<Long>();

    /** See {@code SewerDriver} for specification. */
    @Override
    public void seek(SeekState state) {
        assert state != null;
        if (state.distanceToRing() != 0) {
            dfsSearch(state);
        } else {
            return;
        }
    }

    /**
     * Find the tile with the ring on it and move from the current tile's location to that tile in
     * an optimized number of steps. At every tile, the neighboring tiles are iterated over and the
     * neighbor closest to the ring is where the current tile's location is moved to. This is done
     * recursively until all the tiles the current tile progressively moves to has its neighbors
     * iterated over
     * Precondition: state != null
     * Postcondition: McDiver has found the tile with the ring.
     */
    private void dfsSearch(SeekState state) {
        assert state != null;
        SlowPQueue<NodeStatus> nodes = new SlowPQueue<>();
        if (state.distanceToRing() == 0) {
            seek(state);
        }
        long current = state.currentLocation();
        visitedSeek.add(current);
        //inv: "visitedSeek" contains the visited neighbors. nodes is a priority queue containing
        //the neighbors and the distance from that specific neighbor to the ring.
        for (NodeStatus neighbor : state.neighbors()) {
            if (!visitedSeek.contains(neighbor.getId())) {
                nodes.add(neighbor, neighbor.getDistanceToRing());
            }
        }
        //inv: "nodes" contains unsettled neighbors of the current tile
        while (!nodes.isEmpty()) {
            NodeStatus min = nodes.extractMin();
            if (state.distanceToRing() != 0) {
                state.moveTo(min.getId());
                // recursively calls dfsSearch on the current state
                // (so that the next closest neighboring tile can be found)
                dfsSearch(state);
                if (state.distanceToRing() != 0) {
                    state.moveTo(current);
                }
            }
        }
    }

    /**
     * Precondition: "state" != null
     * Find the best way to optimize the number of remaining steps, while moving to tiles with high
     * score values from the current location. A score for each tile is computed by the formula:
     * -1*(coin value)/(distance from current tile to that tile + distance from
     * that tile to the exit). Arbitrary weights are assigned to each of these components that make
     * up the tile's average score. These scores are stored in a priority queue, corresponding to
     * each tile's score. Move to the tile with the best score as long as there are
     * just enough steps remaining to move to the exit.
     * Returns boolean signifying that we must return to exit; returns false if otherwise
     * Postcondition: McDiver has collected the coins and needs to return back to the exit due
     * to steps remaining (call the scramReturn function).
     */
    private boolean dfsSearch(ScramState state) {
        assert state != null;
        SlowPQueue<Node> nodes = new SlowPQueue<>();
        List<Edge> bestToExit;
        List<Edge> bestToNode;
        int lengthToExit = Integer.MIN_VALUE;
        //inv: "state.stepsToGo" signifies how many steps are left for McDiver; lengthToExit
        //represents the distance to the exit for McDiver.
        while (state.stepsToGo() >= lengthToExit) {
            lengthToExit = 0;
            //inv: "nodes" contains the unsettled neighbors of the current state, with the priority
            // being the score of each tile
            for (Node node : state.allNodes()) {
                Maze maze = new Maze((Set<Node>) state.allNodes());
                ShortestPaths<Node, Edge> path = new ShortestPaths<Node, Edge>(maze);
                path.singleSourceDistances(state.currentNode());
                int totalToNode = 0;
                int totalToExit = 0;
                bestToNode = path.bestPath(node);
                //inv: "totalToNode" stores the length of the best path from the current node
                // to the node "node" in the for loop by summing the edges' lengths.
                for (Edge e : bestToNode) {
                    totalToNode += e.length();
                }
                path.singleSourceDistances(node);
                bestToNode = path.bestPath(state.exit());
                //inv: "totalToExit" stores the length of the best path from the node "node"
                // in the for loop to the exit by summing the edges' lengths.
                for (Edge e : bestToNode) {
                    totalToExit += e.length();
                }
                try {
                    nodes.add(node, -1 * ((
                            (node.getTile().coins())) / ((4.2) * (totalToNode) +
                            ((double) 1 / (state.stepsToGo() + 1) * (totalToExit)) + 1)));
                } catch(IllegalArgumentException e) {
                    nodes.changePriority(node, -1 * ((
                            (node.getTile().coins())) / ((4.2) * (totalToNode) +
                            ((double) 1 /(state.stepsToGo()+1) * (totalToExit))+1)));
                }
            }
            Node goTo = nodes.extractMin();
            Maze maze = new Maze((Set<Node>) state.allNodes());
            ShortestPaths<Node, Edge> pathToExit = new ShortestPaths<Node, Edge>(maze);
            ShortestPaths<Node, Edge> pathToNode = new ShortestPaths<Node, Edge>(maze);
            pathToNode.singleSourceDistances(state.currentNode());
            bestToNode = pathToNode.bestPath(goTo);
            //inv: "lengthToExit" stores the length from the node to the exit,
            //     "bestToExit" contains the edges for the best path to the exit from the current
            //     node. "bestToNode" contains the best path to the node we desire to go to.
            for (Edge e : bestToNode) {
                lengthToExit = e.length();
                pathToExit.singleSourceDistances(maze.dest(e));
                bestToExit = pathToExit.bestPath(state.exit());
                //inv: "lengthToExit" contains the length of the path to the exit from the
                // current node
                for (Edge edge : bestToExit) {
                    lengthToExit += edge.length();
                }
                if (state.stepsToGo() > lengthToExit) {
                    state.moveTo(maze.dest(e));
                } else {
                    scramReturn(state);
                    return true;
                }
            }
        }
        return false;
    }

    /** See {@code SewerDriver} for specification. */
    @Override
    public void scram(ScramState state) {
        assert state != null;
        dfsSearch(state);
        if (scramReturn(state)) {
            return;
        }
    }

    /**
     * Precondition: "state" != null
     * Finds the best path to take to get to the destination tile, and moves there from the tile's
     * current location.
     * Returns boolean signifying scramReturn is complete (McDiver has reached exit); returns false
     * otherwise
     * Postcondition: Scram is officially complete.
     */
    private boolean scramReturn(ScramState state) {
        assert state != null;
        List<Edge> best;
        Maze maze = new Maze((Set<Node>) state.allNodes());
        ShortestPaths path = new ShortestPaths(maze);
        path.singleSourceDistances(state.currentNode());
        best = path.bestPath(state.exit());
        //inv: "best" contains the edges for the best path from the source to the exit tile
        for (Edge e: best) {
            if (maze.dest(e).equals(state.exit())) {
                state.moveTo(maze.dest(e));
                return true;
            }
            state.moveTo(maze.dest(e));
        }
        return false;
    }
}
