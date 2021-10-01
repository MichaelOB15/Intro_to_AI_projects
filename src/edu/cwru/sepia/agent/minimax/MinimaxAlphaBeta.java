package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.minimax.GameState.Player;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
        //numPlys = 1;
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {
    	if(depth <= 0) 
    		return node;
    	
    	Double value = (node.state.max)? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    	List<GameStateChild> children = orderChildrenWithHeuristics(node.state.getChildren());
    	GameStateChild best = node;
    	
    	for(GameStateChild child : children) {
    		GameStateChild current = alphaBetaSearch(child, --depth, alpha, beta);
    		
    		// if footmans turn
    		if(node.state.max) {
    			value = Math.max(value, current.state.getUtility());
    			alpha = Math.max(value,alpha);
    		}
    		
    		// if archers turn
    		else{
    			value = Math.min(value, current.state.getUtility());
    			beta = Math.min(value, beta);
    		}
    		
    		if(alpha == current.state.getUtility()) 
    			best = current;
    		
    		// prunes the search tree
    		if(beta < alpha)
				break;
		}
    	return best;
    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
    	// insertion sort
    	for (int j = 1; j < children.size(); j++) {
    		GameStateChild current = children.get(j);
    		int i = j-1;
    		
            while ((i > -1) && (estimate(children.get(i)) > estimate(current))) {
                children.set(i+1, children.get(i));
                i--;
            }
            children.set(i+1, current);
        }

    	return children;
    }
    
    /*
     * Estimates the best node to search for first such that the 
     * most nodes in the search tree are pruned
     * 
     * @param child 
     * @return double estimating the best node for a heuristic
     */
    public double estimate (GameStateChild child) {
    	ArrayList<Player> playerUnitIDs = child.state.friends;
    	ArrayList<Player> enemyUnitIDs = child.state.foes;
    	double estimate = 0.0;
    	
    	for (Player player: playerUnitIDs) {
    		double smallest = Double.POSITIVE_INFINITY;
    		for (Player enemy: enemyUnitIDs) {
    			// manhattan distance
    			smallest = Math.min(Math.abs(player.x-enemy.x) + Math.abs(player.y-enemy.y), smallest);
    		}
    		estimate += smallest;
    	}
    	return estimate;
    }
}
