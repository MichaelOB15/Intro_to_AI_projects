package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.agent.planner.GameState.Peasant;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.util.Direction;

public class Harvest implements StripsAction{
	public GameState state;
	public GameState.Peasant peasant;
	public Position positionToMove;
	public int duration;
	public Move m;
	public Resource resource;
	
	/**
	 * Constructor for Harvest
	 * 
	 * @param state this is the state to update
	 * @param peeasant peasant that is being applied to
	 * @Param resource being update
	 */
	public Harvest (GameState state, GameState.Peasant peasant, Resource resource) {
		this.state = state;
		this.peasant = peasant;
		this.resource = resource;
		m = new Move(state, peasant, resource.position);
		duration = m.getDuration()+1;
		positionToMove = m.positionToMove;
	}
	
	/**
	 * Constructor for Harvest
	 * 
	 * @param state this is the state to update
	 * @param peasant peasant that is being applied to
	 * @Param resource being update
	 * @param pos position we are moving to
	 * @duration time takes to complete the action
	 * @m move action that is being used in harvest
	 */
	public Harvest (GameState state, Peasant peasant, Position pos, int duration, Move m, Resource resource) {
		this.state = state;
		this.peasant = peasant;
		this.positionToMove = pos;
		this.duration = duration;
		this.m = m;
		this.resource = resource;
	}
	
	/**
	 * Checks the preconditions for harvest
	 * 
	 * @param state state to be updated
	 * @return returns true if preconditions are met
	 */
	public boolean preconditionsMet(GameState state) {
		
		boolean movePreconditionsAreGood = false;
		boolean locHasResource = false;
		boolean isHolding = true;
		boolean isIdle = false;
		boolean properResourceID = false;
		
		List <Resource> goldMines = state.goldMines;
		List <Resource> trees = state.forests;

		// gets the resource from the resource ID
		for (Resource mine: goldMines) {
			if (mine.position.equals(resource.position)) {
				resource = mine;
				break;
			}
		}
		for (Resource tree: trees) {
			if (tree.position.equals(resource.position)) {
				resource = tree;
				break;
			}
		}
		
		// checking if its a valid resource ID
		if (resource != null) properResourceID = true;
		
		// checking if resource has resources
		if (resource.remaining > 0) locHasResource = true;
		
		// checking if the peasant is idle
		if (peasant.currentAction == null || peasant.currentAction.getDuration() <= 0) isIdle = true;
		
		// checking if the peasant is holding something
		if (peasant.typeOfResourceHolding == null) isHolding = false;
		
		// checking if move preconditions are met
		if (m.preconditionsMet(state)) movePreconditionsAreGood = true;

		return (properResourceID && locHasResource && isIdle && !isHolding && movePreconditionsAreGood);

	}
	
	/**
	 * Applys the effects of harvest
	 * 
	 * @state Gamestate to apply the harvest on
	 * @return returns the updated gamestate
	 */
	public GameState apply(GameState state) {
		ResourceType type;
		if(resource.resource == ResourceNode.Type.GOLD_MINE) type = ResourceType.GOLD; else type = ResourceType.WOOD;

		Peasant p = new Peasant(m.nextToFinal, peasant.id, this, type);
		
		Resource r = new Resource(resource.position, resource.id, resource.resource, -100 + resource.remaining);
		
		GameState g = new GameState(state, r, state.currentGold, state.currentWood, state.prevAction, state.numFood);
		GameState g1 = new GameState(g, p, state.currentGold, state.currentWood, state.prevAction, state.numFood);
		
		//System.out.println(g1);
		return g1;
	}
	
	public Harvest clone() {
		return new Harvest(state.clone(), new Peasant(peasant.position, peasant.id, this, peasant.typeOfResourceHolding), positionToMove, duration, m, new Resource(resource.position, resource.id,resource.resource,resource.remaining));
	}
	

	/**
	 * compares two harvest actions
	 * 
	 * @Object direction object
	 * @return returns true if the harvests are equal and false if they are not equal
	 */
	public boolean equals(Object o) {
		if (o instanceof Harvest) {
			Harvest h = (Harvest) o;
			return 	peasant.equals(h.peasant) 
					&& duration == h.duration
					&& positionToMove.equals(h.positionToMove)
					&& resource.equals(resource);
		}
		else return false;
	}
	
	public int hashCode() {
		return peasant.hashCode() * 13 + duration;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public String toString() {
        return "Harvesting "+resource.resource.toString()+" at "+resource.position.toString();
}
}
