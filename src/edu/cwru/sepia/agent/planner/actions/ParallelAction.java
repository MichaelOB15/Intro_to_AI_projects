package edu.cwru.sepia.agent.planner.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState.Peasant;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;

public class ParallelAction implements StripsAction{
	public HashMap<Peasant,StripsAction> peasantActions;
	public HashMap<Peasant,StripsAction> newActions;
	public StripsAction townHallAction = null;
	public int duration;
	
	public ParallelAction(List<Peasant> oldPeasants, List<Peasant> newPeasants, StripsAction townHallAction) {
		HashMap<Peasant,StripsAction> peasantActions = new HashMap<>();
		HashMap<Peasant,StripsAction> newActions = new HashMap<>();
		for(Peasant peasant : oldPeasants) {
			peasantActions.put(peasant, peasant.currentAction.clone());
		}
		
		for(Peasant peasant : newPeasants) {
			peasantActions.put(peasant, peasant.currentAction.clone());
			newActions.put(peasant, peasant.currentAction.clone());
		}
		this.townHallAction = townHallAction;
		this.peasantActions = peasantActions;
		this.newActions = newActions;
		
		//this line is pretty cool
		StripsAction minAction = peasantActions.values().stream().min((StripsAction action1, StripsAction action2) -> action1.getDuration() - action2.getDuration()).orElse(null);
		
		if(townHallAction != null) {
			duration = 1;
		}
		else {
			duration = minAction.getDuration();
		}
		
		
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		for(Peasant peasant : peasantActions.keySet()) {
			if (!peasantActions.get(peasant).preconditionsMet(state)) return false;
		}
		if(townHallAction != null && !townHallAction.preconditionsMet(state)) return false;
		return true;
	}

	@Override
	public GameState apply(GameState state) {
		GameState copy = state.clone();
		List<Peasant> newPeas = new ArrayList<>();
		List<Peasant> changedPeasants = new ArrayList<>();
		
		boolean bool = false;
		
		
		if(townHallAction != null)
			if(townHallAction.preconditionsMet(copy.parent)) {
				duration = 1;
				bool = true;
			}
		
		for(Peasant peasant : copy.peasants) {
			if(peasant.hasAction())
				duration = Math.min(duration, peasant.currentAction.getDuration());
		}
		for(Peasant peasant : copy.peasants) {
			if(peasant.currentAction != null) {
				if(peasant.currentAction.getDuration() == 0) {
					StripsAction a = peasantActions.get(peasant).clone();
					ResourceType type = null;
					if(a instanceof Harvest) {
						Harvest h = (Harvest) a;
						if(h.resource.resource == ResourceNode.Type.GOLD_MINE) type = ResourceType.GOLD; else type = ResourceType.WOOD;
					}
					Peasant pp = new Peasant(peasant.position, peasant.id, a, type);
					pp.currentAction.setDuration(pp.currentAction.getDuration() - duration);
					changedPeasants.add(pp);
				}
				else {
					peasant.currentAction.setDuration(peasant.currentAction.getDuration() - duration);
					newPeas.add(peasant);
				}
			}
			else {
				ResourceType type = null;
				if(newActions.get(peasant) instanceof Harvest) {
					Harvest harv = (Harvest) newActions.get(peasant);
					if(harv.resource.resource == ResourceNode.Type.GOLD_MINE) type = ResourceType.GOLD; else type = ResourceType.WOOD;
				}
				Peasant pooPeasant = new Peasant(peasant.position, peasant.id, newActions.get(peasant), type);
				pooPeasant.currentAction.setDuration(pooPeasant.currentAction.getDuration() - duration);
				changedPeasants.add(pooPeasant);
			}
		}
		newPeas.addAll(changedPeasants);
		copy.peasants = newPeas;

		for(Peasant p : changedPeasants) {
			//System.out.println("Before: "+p.position);
			copy = p.currentAction.apply(copy);
			//System.out.println("After: "+p.position);
		}
		
		if(bool) {
			copy = townHallAction.apply(copy);
			townHallAction = null;
		}
		copy.prevAction = this;
		copy.parent = state;
		return copy;
	}

	@Override
	public int getDuration() {
		return duration;
	}

	@Override
	public void setDuration(int duration) {
	}
	
	public StripsAction clone() {
		return null;
	}
	public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Peasant peas : newActions.keySet()) {
                sb.append(peas.currentAction.toString());
                sb.append(" AND ");
        }
        if(townHallAction != null) sb.append(townHallAction.toString());
        return (sb.length()==0)? "No new actions":sb.substring(0,sb.length()-5).toString();
}

	

}
