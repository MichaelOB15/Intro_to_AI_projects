package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Peasant;
import edu.cwru.sepia.agent.planner.GameState.Resource;
import edu.cwru.sepia.agent.planner.Position;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.util.Direction;

public class Deposit implements StripsAction{
	public GameState state;
	public GameState.Peasant peasant;
	public Position positionToMove;
	public int duration;
	public Move m;
	
	/**
	 * Constructor for Deposit
	 * 
	 * @param state this is the state to update
	 * @param peeasant peasant that is being applied to
	 */
	public Deposit (GameState state, GameState.Peasant peasant) {
		this.state = state;
		this.peasant = peasant;
		m = new Move(state, peasant, state.townHall);
		duration = m.getDuration()+1;
		positionToMove = m.positionToMove;
	}	
	
	/**
	 * Constructor for Harvest
	 * 
	 * @param state this is the state to update
	 * @param peasant peasant that is being applied to
	 * @param pos position we are moving to
	 * @duration time takes to complete the action
	 * @m move action that is being used in harvest
	 */
	public Deposit (GameState state, Peasant peasant, Position pos, int duration, Move m) {
		this.state = state;
		this.peasant = peasant;
		this.positionToMove = pos;
		this.duration = duration;
		this.m = m;
	}
	
	/**
	 * Checks the preconditions for deposit
	 * 
	 * @param state state to check if preconditions are meet
	 * @return returns true if preconditions are met and false if they are not met 
	 */
	public boolean preconditionsMet(GameState state) {
		
		boolean movePreconditionsAreGood = false;
		boolean isHolding = false;
		boolean isIdle = false;
		
		// checking if the peasant is idle
		if (peasant.currentAction == null || peasant.currentAction.getDuration() <= 0) isIdle = true;
		
		// checking if the peasant is holding something
		if (peasant.typeOfResourceHolding != null) isHolding = true;
		
		// checking if move preconditions are met
		if (m.preconditionsMet(state)) movePreconditionsAreGood = true;

		//return (isHolding && nextToTownHall && correctDirectionToTownHall);
		return (isIdle && isHolding && movePreconditionsAreGood);
	}

	/**
	 * Applys the effects of deposit
	 * 
	 * @state Gamestate to apply the deposit on
	 * @return returns the updated gamestate
	 */
	public GameState apply(GameState state) {

		Peasant p = new Peasant(m.nextToFinal, peasant.id, this, null);
		
		if (peasant.typeOfResourceHolding == ResourceType.GOLD)
			return new GameState(state, p, 100+state.currentGold, state.currentWood, state.prevAction, state.numFood);
		
		else
			return new GameState(state, p, state.currentGold, 100+state.currentWood, state.prevAction, state.numFood);
		
	}
	
	public Deposit clone() {
		return new Deposit(state.clone(), new Peasant(peasant.position, peasant.id, this, peasant.typeOfResourceHolding), positionToMove, duration, m);
	}

	/**
	 * compares two deposit actions
	 * 
	 * @Object direction object
	 * @return returns true if the deposits are equal and false if they are not equal
	 */
	public boolean equals(Object o) {
		if (o instanceof Deposit) {
			Deposit d = (Deposit) o;
			return 	peasant.equals(m.peasant) 
					&& duration == d.duration
					&& positionToMove.equals(d.positionToMove);
		}
		else return false;
	}
	
	public int hashCode() {
		return peasant.hashCode() * 13 + duration;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public void setDuration(int duration) {
		this.duration = duration;	
	}
	
	 public String toString() {
         return "Depositing "+peasant.typeOfResourceHolding.toString();
	 }
}
