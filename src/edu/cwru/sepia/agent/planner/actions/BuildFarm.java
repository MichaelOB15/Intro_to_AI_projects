package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Position;

public class BuildFarm implements StripsAction{
	public GameState.Peasant peasant;
	public Position Farmposition;
	public GameState state;
	public int duration;
	public Move m;
	
	public BuildFarm(GameState state, GameState.Peasant peasant, Position position) {
		this.state = state;
		this.peasant = peasant;
		Farmposition = position;
		m = new Move(state, peasant, position);
		duration = m.getDuration() + 1;
	}
	
	public boolean preconditionsMet(GameState state) {
		boolean isIdle = false;
		boolean pathToFarmLoc = false;
		boolean enoughGold = false;
		boolean enoughWood = false;
		// can we move to the place we want to build the farm
		
		// checking if the peasant is idle
		if (peasant.currentAction == null || peasant.currentAction.getDuration() <= 0) isIdle = true;
			
		// checking if we have enough gold
		if (state.currentGold >= 500) enoughGold = true;
		
		// checking if we have enough wood
		if (state.currentWood >= 250) enoughWood = true;
		
		// checking if move preconditions are met
		Move m = new Move(state, peasant, Farmposition);
		if (m.preconditionsMet(state)) pathToFarmLoc = true;
		
		return (isIdle && pathToFarmLoc && enoughGold && enoughWood);
	}
	
	public StripsAction clone() {
		return null;
	}

	@Override
	public GameState apply(GameState state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public void setDuration(int duration) {
		this.duration = duration;
	}

}
