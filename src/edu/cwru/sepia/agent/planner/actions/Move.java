package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.MapLocation;

public class Move{
	public GameState state;
	public GameState.Peasant peasant;
	public Position positionToMove;
	public int duration;
	public Position nextToFinal;
	
	/**
	 * Constructor for Move
	 * 
	 * @param state this is the state being updated
	 * @param peeasant this is the peasant being applied to
	 * @param position position to move the peasant to
	 */
	public Move(GameState state, GameState.Peasant peasant, Position position) {
		this.state = state;
		this.peasant = peasant;
		positionToMove = position;
		
		List <Resource> goldMines = state.goldMines;
		List <Resource> trees = state.forests;
		List <Resource> totalResource = new ArrayList<Resource>(goldMines);
		List <Position> resourceLocations = new ArrayList<Position>();
		totalResource.addAll(trees);
		
		for (Resource res: totalResource) {
			resourceLocations.add(res.position);
		}
		resourceLocations.add(state.townHall);
		
		Stack<MapLocation> path = AstarSearchPathFinding(new MapLocation(peasant.position, positionToMove, null), state.xExtent,state.yExtent, resourceLocations);

		nextToFinal = path.get(0);
		duration = path.size();
	}
	
	/**
	 * Checks the preconditions for move
	 * 
	 * @param state this is the state being applied to
	 * @return returns true if preconditions are met and false if they are not met 
	 */
	public boolean preconditionsMet(GameState state) {
		boolean hasPath = false;
		boolean isIdle = false;
		
		// checks if there is an available path
		if (nextToFinal != null) hasPath = true;
		
		// checking if the peasant is idle
		if (peasant.currentAction == null || peasant.currentAction.getDuration() <= 0) isIdle = true;
				
		return (isIdle && hasPath);
	}

	public int getDuration() {
		return duration;
	}
	
	/**
	 * Finds the path for the peasant to move 
	 * 
	 * @param start this is the start position
	 * @param xExtent x length of the map
	 * @param yExtent y length of the map
	 * @param ResourceLocations list of locations of resources
	 * @return returns thee stack of map locations that the peasant will travel on
	 */
    private Stack<MapLocation> AstarSearchPathFinding(MapLocation start, int xExtent, int yExtent, List<Position> ResourceLocations) {
    	Stack<MapLocation> path = new Stack<MapLocation>(); //initialize the path
    	PriorityQueue<MapLocation> openList = new PriorityQueue<MapLocation>(); // initialize the open list
    	openList.add(start); //starting node on here
    	Set<MapLocation> closedList = new HashSet<MapLocation>(); // initialize the closed list
    
    	while(!openList.isEmpty()) { // while the open list is not empty
    		
    		MapLocation current = openList.remove(); // get node with lowest function value
    		
    		closedList.add(current);
    		
    		if(current.equals(start.goal)) { //goal check
    			path = buildPath(current,path);
    			break;
    		}
    		
    		ArrayList<MapLocation> neighbors = current.getAvailableNeighbors(xExtent, yExtent, ResourceLocations);// generate successors and set their parents to q
    			for(MapLocation neighbor : neighbors) {// for each succesor
    				
	    			if(closedList.contains(neighbor))
	    				continue;
	    			if(openList.contains(neighbor)) { //if location has been seen, update to min cost
	    				if(openList.removeIf( 
	    					(e) -> neighbor.equals(e) && neighbor.cost + neighbor.chebyshevDistance(neighbor.goal) < 
	    					 							 e.cost + e.chebyshevDistance(e.goal))) {
	    					openList.add(neighbor);
	    				}
	    			}
	    			else openList.add(neighbor); //add to open list
	    		}
	    }
    	
    	if(path.isEmpty()) {
    		System.out.println("##################\n No Available Path \n##################");
    		System.exit(0);
    	}

    	return path;
    }
    
    private Stack<MapLocation> buildPath(MapLocation loc, Stack<MapLocation> stack){
    	MapLocation prev = loc.cameFrom;
    	while(prev != null) {
    		stack.push(prev);
    		prev = prev.cameFrom;
    	}
    	return stack;
    }
	
}
