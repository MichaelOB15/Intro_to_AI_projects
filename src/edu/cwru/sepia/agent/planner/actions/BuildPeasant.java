package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Peasant;
import edu.cwru.sepia.agent.planner.Position;

public class BuildPeasant implements StripsAction{
	int duration;

	public BuildPeasant () {
		duration = 1;
	}
	
	public boolean preconditionsMet(GameState state) {
		
		boolean haveEnoughFood = false;
		boolean haveEnoughGold = false;
		
		// checking if we have enough gold
		if (state.currentGold >= 400) haveEnoughGold = true;
		
		// checking if we have enough food
		if (state.numFood > 1) haveEnoughFood = true;
		
		return (haveEnoughGold && haveEnoughFood);
	}

	@Override
	public GameState apply(GameState state) {
		Peasant p = new Peasant(new Position(state.townHall.x-1,state.townHall.y),state.peasants.size() + 1);

		state.prevAction.townHallAction = this;
		return new GameState(state, p, -400 + state.currentGold , state.currentWood, state.prevAction, -1 + state.numFood);
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public void setDuration(int duration) {
		this.duration = duration;
	}
	public StripsAction clone() {
		return null;
	}
	public String toString() {
        return "Building Peasant";
	}
	
}
