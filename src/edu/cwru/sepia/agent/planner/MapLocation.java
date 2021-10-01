package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;

public class MapLocation extends Position implements Comparable<MapLocation> {
	public Position goal;
	public MapLocation cameFrom;
	public int cost;

	public MapLocation(int x, int y, Position goal, MapLocation cameFrom) {
		super(x, y);
		this.goal = goal;
		this.cameFrom = cameFrom;
		if (cameFrom == null) cost = 0;
		else cost = cameFrom.cost + 1;
	}
	
	public MapLocation(Position pos, Position goal, MapLocation cameFrom){
		super(pos);
		this.goal = goal;
		this.cameFrom = cameFrom;
		if (cameFrom == null) cost = 0;
		else cost = cameFrom.cost + 1;
	}
	

	@Override
	public int compareTo(MapLocation o) {
		
		return (this.cost + chebyshevDistance(goal)) - (o.cost + o.chebyshevDistance(goal));
	}
	
	public boolean equals(Object o) {
		if(o instanceof MapLocation) {
			MapLocation loc = (MapLocation) o;
			return this.x == loc.x && this.y == loc.y;
			}
		else if(o instanceof Position) {
			Position pos = (Position) o;
			return this.x == pos.x && this.y == pos.y;
		}
		else return false;
	}
	
	
    public ArrayList<MapLocation> getAvailableNeighbors(int xExtent,int yExtent, List<Position> ResourceLocations){
		
		ArrayList<Position> Neighbors = new ArrayList<Position>();
		Neighbors = (ArrayList<Position>) this.getAdjacentPositions();
		
		
		ArrayList<MapLocation> availableNeighbors = new ArrayList<MapLocation>();
		for (Position neighbor: Neighbors) {
			int x = 0;
			if(neighbor.equals(goal)) availableNeighbors.add(new MapLocation(neighbor,goal,this));
			if(ResourceLocations.contains(neighbor)||neighbor.x < 0 || neighbor.y < 0 || neighbor.x > xExtent || neighbor.y > yExtent)
				continue;
			else availableNeighbors.add(new MapLocation(neighbor,goal,this));
		}
		return availableNeighbors;
    }
	
	
}
