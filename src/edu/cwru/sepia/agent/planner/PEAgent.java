package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<Integer, Integer>();
        this.plan = plan;
        System.out.println("agent created\n");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(unitId, unitId);
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
	 * To check a unit's progress on the action they were executing last turn, you can use the following:
     * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1).get(unitID).getFeedback()
     * This returns an enum ActionFeedback. When the action is done, it will return ActionFeedback.COMPLETED
     *
     * Alternatively, you can see the feedback for each action being executed during the last turn. Here is a short example.
     * if (stateView.getTurnNumber() != 0) {
     *   Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     *   for (ActionResult result : actionResults.values()) {
     *     <stuff>
     *   }
     * }
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        List<Integer> ids = stateView.getAllUnitIds();
    	//System.out.println(ids);
        for(int i = 1; i < ids.size(); i++) {
    		peasantIdMap.put(i, ids.get(i));
        }
    	
    	Map<Integer,Action> map = new HashMap<Integer,Action>();
    	StripsAction currentAction = null ;
    	
    	//get previous action results
    	Map<Integer, ActionResult> prevActionResults = historyView.getCommandFeedback(playernum,stateView.getTurnNumber() - 1);
    	    	
    	//if no results, get next action
    	if(prevActionResults.isEmpty()) { 
    		currentAction = plan.pop();
    	}
    	
    	//if there are results
    	else {
    		//loop through the results
    		for(ActionResult actionResult : prevActionResults.values()) {
    			//get the feedback
    			ActionFeedback feedback = actionResult.getFeedback(); 
    			if(feedback == ActionFeedback.FAILED || feedback == ActionFeedback.INCOMPLETE)
    				
    				//if actions did not complete, or failed, try again
    				map.put(actionResult.getAction().getUnitId(),actionResult.getAction()); 
    			
    			//if an action completed, get the next action and break since we are at new gamestate
    			else {
    				if (plan.size() != 1)
    					currentAction = plan.pop();
    				break;
    			}
    		}
    	}
    	//if everything failed or is incomplete, everything will keep going
    	if(currentAction == null) 
    		return map;
    	//otherwise add all new actions to the map
    	else { 
    		for(Action action : generateActions(currentAction)) {
    			//System.out.println(" Action: "+ action);
        		map.put(action.getUnitId(),action);
        	}
    	}
        return map;
    }
    
    
    private ArrayList<Action> generateActions(StripsAction parallelAction){
    	ParallelAction actions = (ParallelAction) parallelAction;
    	ArrayList<Action> result = new ArrayList<>();
    	for(StripsAction action : actions.newActions.values()) {
    		result.add(createSepiaAction(action));
    	}
    	if(actions.townHallAction != null) result.add(createSepiaAction(actions.townHallAction));
    	return result;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     * 
     * Hint:
     * peasantId could be found in peasantIdMap
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action) {
    	int actionType;
    	if(action instanceof Harvest) actionType = 0;
    	else if(action instanceof Deposit) actionType = 1;
    	else actionType = 2; //action is BuildPeasant

    	Action result = null;
    	
    	switch(actionType) {
    		case 0:
    			Harvest h = (Harvest)action;
    			result = Action.createCompoundGather(peasantIdMap.get(h.peasant.id),h.resource.id);
    			break;
    		case 1:
    			Deposit d = (Deposit)action;
    			result = Action.createCompoundDeposit(peasantIdMap.get(d.peasant.id), townhallId);
    			break;
    		case 2:
    			BuildPeasant b = (BuildPeasant)action;
    			result = Action.createCompoundProduction(townhallId, 26);
    			break;
    	}
    	return result;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
