package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.BuildPeasant;
import edu.cwru.sepia.agent.planner.actions.Deposit;
import edu.cwru.sepia.agent.planner.actions.Harvest;
import edu.cwru.sepia.agent.planner.actions.Move;
import edu.cwru.sepia.agent.planner.actions.ParallelAction;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.awt.Desktop.Action;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
  * Note that SEPIA saves the townhall as a unit. Therefore when you create a GameState instance,
 * you must be able to distinguish the townhall from a peasant. This can be done by getting
 * the name of the unit type from that unit's TemplateView:
 * state.getUnit(id).getTemplateView().getName().toLowerCase(): returns "townhall" or "peasant"
 * 
 * You will also need to distinguish between gold mines and trees.
 * state.getResourceNode(id).getType(): returns the type of the given resource
 * 
 * You can compare these types to values in the ResourceNode.Type enum:
 * ResourceNode.Type.GOLD_MINE and ResourceNode.Type.TREE
 * 
 * You can check how much of a resource is remaining with the following:
 * state.getResourceNode(id).getAmountRemaining()
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
	
	public ParallelAction prevAction;
	public GameState parent;
	public List<GameState> states;
    public List<Peasant> peasants;
    public List<Resource> goldMines;
    public List<Resource> forests;
    public Position townHall;
    public int xExtent;
    public int yExtent;
    public int requiredGold;
    public int requiredWood;
    public int currentGold;
    public int currentWood;
    public int numFood;


    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {
    	parent = null;
    	peasants = new ArrayList<>();
    	goldMines = new ArrayList<>();
    	forests = new ArrayList<>();
    	prevAction = null;
    	numFood = 3;
    	
    	xExtent = state.getXExtent();
    	yExtent = state.getYExtent();
    	this.requiredGold = requiredGold;
    	this.requiredWood = requiredWood;
    	currentGold = 0;
    	currentWood = 0;
    	
    	
    	List<Integer> playerUnits = state.getUnitIds(0);
    	List<ResourceView> mines = state.getResourceNodes(ResourceNode.Type.GOLD_MINE);
    	List<ResourceView> trees = state.getResourceNodes(ResourceNode.Type.TREE);
    	
    	for(Integer id : playerUnits) {
    		UnitView u = state.getUnit(id);
    		if(u.getTemplateView().getName().toLowerCase().equals("townhall")) {
    			townHall = new Position(u.getXPosition(), u.getYPosition());
    		}
    		else {
    			Position pos = new Position(u.getXPosition(), u.getYPosition());
    			peasants.add(new Peasant(pos, id));
    		}
    	}
    	
    	for(ResourceView mine : mines) {
    		goldMines.add(new Resource(new Position(mine.getXPosition(), mine.getYPosition()), mine.getID(), mine.getType(), mine.getAmountRemaining()));
    	}
    	
    	for(ResourceView tree : trees) {
    		forests.add(new Resource(new Position(tree.getXPosition(), tree.getYPosition()),tree.getID(), tree.getType(), tree.getAmountRemaining()));
    	}
    	
    }
    
    public GameState(GameState state, Peasant p, int CurrentGold, int CurrentWood, ParallelAction previous, int numFood) {
    	parent = state;

    	List<Peasant> peezeys = new ArrayList<>();
    	for(Peasant pez : state.peasants) {
    		if(p.id != pez.id) {
    			peezeys.add(pez);
    		}
    	}
    	this.prevAction = previous;
    	peezeys.add(p);
    	peasants = peezeys;
    	goldMines = state.goldMines;
    	forests = state.forests;
    	townHall = state.townHall;
    	xExtent = state.xExtent;
    	yExtent = state.yExtent;
    	requiredGold = state.requiredGold;
    	requiredWood = state.requiredWood;
    	currentGold = CurrentGold;
    	currentWood = CurrentWood;
    	this.numFood = numFood;
    	
    }
    
    public GameState(GameState state, Resource r, int CurrentGold, int CurrentWood, ParallelAction previous, int numFood) {
    	parent = state.parent;
    	List<Resource> res = new ArrayList<>();
    	List<Resource> list;
    	ResourceNode.Type type = r.resource;
    	if(type == ResourceNode.Type.GOLD_MINE) list = state.goldMines; else list = state.forests;
    	for(Resource resource : list) {
    		if(r.id != resource.id) {
    			res.add(resource);
    		}
    	}
    	res.add(r);
    	this.prevAction = previous;
    	peasants = state.peasants;
    	
    	if(r.resource == ResourceNode.Type.GOLD_MINE) {
    		goldMines = res;
    		forests = state.forests;
    	} else {
    		goldMines = state.goldMines;
    		forests = res;
    	}
    	townHall = state.townHall;
    	xExtent = state.xExtent;
    	yExtent = state.yExtent;
    	requiredGold = state.requiredGold;
    	requiredWood = state.requiredWood;
    	currentGold = CurrentGold;
    	currentWood = CurrentWood;
    	this.numFood = numFood;
    	
    }
    
    public GameState(GameState state) {
    	prevAction = state.prevAction;
    	parent = state.parent;
    	ArrayList<Peasant> peeps = new ArrayList<>();
    	for(Peasant peasant : state.peasants) {
    		Peasant p = new Peasant(peasant.position, peasant.id, peasant.currentAction, peasant.typeOfResourceHolding);
    		if(p.currentAction != null) {
    			if(p.currentAction instanceof Harvest) {
    				Harvest h = (Harvest) p.currentAction;
    				p.currentAction = new Harvest(this,p,h.positionToMove,h.duration,h.m,h.resource);
    			} else {
    				Deposit d = (Deposit) p.currentAction;
    				p.currentAction = new Deposit(this,p,d.positionToMove,d.duration,d.m);
    			}
    		}
    		peeps.add(peasant);
    	}
        peasants = peeps;
        
        List<Resource> mines = new ArrayList<>();
        List<Resource> treez = new ArrayList<>();
        
        for(Resource mine : state.goldMines) {
        	mines.add(new Resource(mine.position, mine.id, mine.resource, mine.remaining));
        }
        for(Resource tree : state.forests) {
        	treez.add(new Resource(tree.position, tree.id, tree.resource, tree.remaining));
        }
        
        goldMines = mines;
        forests = treez;
        townHall = state.townHall;
        xExtent = state.xExtent;
        yExtent = state.yExtent;
        requiredGold = state.requiredGold;
        requiredWood = state.requiredWood;
        currentGold = state.currentGold;
        currentWood = state.currentWood;
        numFood = state.numFood;
    	
    }
    
    public static class Peasant{
    	public Position position;
    	public Integer id;
    	public ResourceType typeOfResourceHolding;
    	public StripsAction currentAction;
    	
    	public Peasant(Position pos, Integer id) {
    		this.id = id;
    		position = pos;
    		typeOfResourceHolding = null;
    		currentAction = null;
    	}
    	
    	public Peasant(Position pos, Integer id, StripsAction action, ResourceType typeofResourceHolding) {
    		position = pos;
    		this.id = id;
    		currentAction = action;
    		this.typeOfResourceHolding = typeofResourceHolding;
    	}
    	
    	
    	public boolean hasAction() {
    		return currentAction != null && currentAction.getDuration() > 0;
    	}
    	
    	public boolean isHolding() {
    		return typeOfResourceHolding != null;
    	}
    	
    	public boolean equals(Object o) {
    		Peasant p = (Peasant) o;
    		return  p.id == id;
    	}
    	
    	public int hashCode() {
    		return position.hashCode() * 17 + id;
    	}
    	
    }
    
    public static class Resource{
    	public Position position;
    	public ResourceNode.Type resource;
    	public int remaining;
    	public int id;
    	
    	public Resource(Position pos, int id, ResourceNode.Type type, int remaining) {
    		this.id = id;
    		position = pos;
    		resource = type;
    		this.remaining = remaining;
    	}
    	
    	public boolean equals(Object o) {
    		Resource r = (Resource) o;
    		return 	r.id == id &&
    				r.resource == resource &&
    				r.remaining == remaining &&
    				r.position.equals(position);
    	}
    	
    	public int hashCode() {
    		return position.hashCode() * 17 + remaining - id;
    	}

    }
    

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return currentWood == requiredWood && currentGold == requiredGold;
    }
    
    public List<Peasant> generatePeasantActions(Peasant peasant){
    	List<Peasant> peasants = new ArrayList<>();
    	
    	for(Resource mine : goldMines) {
    		Harvest harvest = new Harvest(this, peasant, mine);
    		if(harvest.preconditionsMet(this)) peasants.add(new Peasant(peasant.position, peasant.id, harvest, peasant.typeOfResourceHolding));
    		
    	}
    	for(Resource tree : forests) {
    		Harvest harvest = new Harvest(this, peasant, tree);
    		if(harvest.preconditionsMet(this)) peasants.add(new Peasant(peasant.position, peasant.id, harvest, peasant.typeOfResourceHolding));
    	}
    	Deposit deposit = new Deposit(this, peasant);
    	if(deposit.preconditionsMet(this)) peasants.add(new Peasant(peasant.position, peasant.id, deposit, null));
    	
    	return peasants;
    	
    	
    }
    
    public List<ParallelAction> generateAllActions(List<List<Peasant>> peasantLists,List<Peasant> workingPeasants, StripsAction townHallAction){
    	List<ParallelAction> output = new ArrayList<>();
    	
    	if(townHallAction != null) {
    		for(List<Peasant> tuple : peasantLists) {
    			ArrayList<Peasant> newList = new ArrayList<Peasant>(workingPeasants);
    			output.add(new ParallelAction(newList, tuple, townHallAction));
    			output.add(new ParallelAction(newList, tuple, null));
    		}
    	}
    	else {
    		for(List<Peasant> tuple : peasantLists) {
    			ArrayList<Peasant> newList = new ArrayList<Peasant>(workingPeasants);
    			output.add(new ParallelAction(newList, tuple, null));
    		}
    	}
    	
    	return output;
    }
    
    public List<List<Peasant>> makeTuples(List<List<Peasant>> input){
    	List<List<Peasant>> result = new ArrayList<>();
    	List<Peasant> prefix = new ArrayList<>();
    	recurse(0,input,prefix,result);
    	return result;
    	
    }
    
    //this stuff is super cool
    
    public void recurse(int index, List<List<Peasant>> input, List<Peasant> prefix, List<List<Peasant>> output){
    	if (index >= input.size()) {
    		output.add(new ArrayList<Peasant>(prefix));
    	}
    	else {
    		List<Peasant> next = input.get(index++);
    		for(Peasant peasant : next) {
    			
    			Peasant clone = new Peasant (peasant.position, peasant.id, peasant.currentAction.clone(), peasant.typeOfResourceHolding);
    			prefix.add(clone);
    			recurse(index, input, prefix, output);
    			prefix.remove(clone);
    		}
    	}
    }
    

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
    	List<GameState> children = new ArrayList<>();
    	
    	List<List<Peasant>> peasantList = new ArrayList<>();
    	for(Peasant idle : getIdlePeasants()) {
    		peasantList.add(generatePeasantActions(idle));
    	}
    	    	
    	
    	List<ParallelAction> parallelActionList = new ArrayList<>();
    	
    	BuildPeasant build = new BuildPeasant();
    	if(build.preconditionsMet(this)) {
    		parallelActionList = generateAllActions(makeTuples(peasantList),getWorkingPeasants(), build);
    	}
    	else {
    		parallelActionList = generateAllActions(makeTuples(peasantList),getWorkingPeasants(), null);
    	}

    	for(ParallelAction action : parallelActionList) {
    		children.add(action.apply(this));
    	}
    	
    	return children;

    }
    
 
    public ArrayList<Peasant> getIdlePeasants(){
    	ArrayList<Peasant> idle = new ArrayList<>();
    	for(Peasant peasant : peasants) {
    		if(peasant.currentAction == null || peasant.currentAction.getDuration() <= 0) idle.add(peasant);
    	}
    	return idle;
    }
    
    public ArrayList<Peasant> getWorkingPeasants(){	
    	ArrayList<Peasant> working = new ArrayList<>();
    	for(Peasant peasant : peasants) {
    		if(peasant.currentAction != null && peasant.currentAction.getDuration() > 0) working.add(peasant);
    	}
    	return working;
    }
    
    
    public Position closestResource(ResourceType type) {
    	Position resource = new Position(0,0);
    	double closest = Double.MAX_VALUE;
    	List<Resource> resources = (type == ResourceType.GOLD)? goldMines : forests;
    	for(Resource rv : resources) {
    		double dist = rv.position.chebyshevDistance(townHall);
    		if(dist < closest) {
    			closest = dist;
    			resource = rv.position;
    		}
    	}
    	return resource;
    }
    
    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     * 
     * 
     * 
     * This heuristic finds the total path time it would take to finish if the peasants could go to the nearest resource to finish
     * 
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {
    	if(this.isGoal()) return 0;
    	
    	double mineDist;
    	if (numFood > 2)
    		mineDist = 2*closestResource(ResourceType.GOLD).chebyshevDistance(townHall);
    	else
    		mineDist = closestResource(ResourceType.GOLD).chebyshevDistance(townHall);

    	double treeDist = closestResource(ResourceType.WOOD).chebyshevDistance(townHall);
    	double mineTrips = (requiredGold - totalGoldHeld())/100;
    	if ((requiredGold - totalGoldHeld()) < 0)
    		mineTrips = 100;
    	double treeTrips = Math.abs(requiredWood - totalWoodHeld())/100;
    	if ((requiredWood - totalWoodHeld()) < 0)
    		treeTrips = 100;
    	double numPeas = peasants.size();
    	double totalDistFromTownHall = 0.0;
    	for (Peasant p : peasants) {
    		totalDistFromTownHall += p.position.chebyshevDistance(townHall);
    	}

    	return 50*((2*(mineDist*mineTrips + treeDist*treeTrips)+totalDistFromTownHall)/numPeas);
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
    	if(prevAction == null) return 0;
    	if(parent == null) return prevAction.getDuration();
    	return parent.getCost() + prevAction.getDuration();
    }
    
    
    public Peasant getPeasantById(int id) {
    	for(Peasant p : peasants) {
    		if (p.id == id) return p;
    	}
    	return null;
    }
    
    public void updateResourceAmount (int id, int amount) {
    	for (Resource R: goldMines) {
    		if (R.id == id) R.remaining = amount;
    	}
    	for (Resource R: forests) {
    		if (R.id == id) R.remaining = amount;
    	}
    }
    
    public int totalGoldHeld() {
    	int sum = currentGold;
    	for(Peasant peasant: peasants) {
    		if(peasant.isHolding() && peasant.typeOfResourceHolding == ResourceType.GOLD)
    			sum += 100;
    			
    	}
    	return sum;
    }
    
    public int totalWoodHeld() {
    	int sum = currentWood;
    	for(Peasant peasant: peasants) {
    		if(peasant.isHolding() && peasant.typeOfResourceHolding == ResourceType.WOOD)
    			sum += 100;
    			
    	}
    	return sum;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
    	return (int) ((getCost() + heuristic()) - (o.getCost() + o.heuristic()));
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
    	if(!(o instanceof GameState)) return false;
    	GameState state = (GameState) o;
    	boolean cost = this.getCost() == state.getCost();
    	boolean parent = false;
    	if(this.parent != null && state.parent != null) {
    		parent = this.parent.equals(state.parent);
    	}
    	boolean peasant = true;
    	boolean mines = true;
    	boolean trees = true;
    	boolean gold = this.currentGold == state.currentGold;
    	boolean wood = this.currentWood == state.currentWood;
    	boolean town = townHall.equals(state.townHall);
    	
    	for(Peasant p : peasants) {
    		if(!state.peasants.contains(p)) peasant = false;
    		Peasant newP = state.getPeasantById(p.id);
			if(newP == null) return false;
    		if(p.currentAction != null) {
    			if(newP.currentAction == null) return false;
    			if(!newP.currentAction.equals(p.currentAction)) return false;
    		}
    		if(newP.currentAction == null) return false;
    	}
    	
    	for(Resource r : goldMines) {
    		if(!state.goldMines.contains(r)) mines = false;
    	}
    	
    	for(Resource r : forests) {
    		if(!state.forests.contains(r)) trees = false;
    	}
    	
    	boolean food = numFood == state.numFood;
    	
    	return parent&& cost && peasant && mines && trees && gold && wood && town && food;
    }
    
    public GameState clone() {
    	return new GameState(this);
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        int sum = 0;
        for(Peasant p : peasants) {
        	sum += p.hashCode();
        }
        
        return sum;
        
    }
    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	for(Peasant peasant : peasants) {
    		int duration = (peasant.currentAction == null)? 0:peasant.currentAction.getDuration();
    		sb.append("Peasant id = "+peasant.id);
    		sb.append("\n\tAction = "+ peasant.currentAction);
    		sb.append("\n\tDuration = "+duration);
    		sb.append("\n\tLocation = "+peasant.position.toString());
    		sb.append("\n\tHolding = "+peasant.typeOfResourceHolding + "\n");
    	}
    	/*
    	for (Resource gold : goldMines) {
    		sb.append("Resource id = "+gold.id);
    		sb.append("\n\tResource type = "+gold.resource);
    		sb.append("\n\tResource locatiion = "+gold.position);
    		sb.append("\n\tResource remainig = "+ gold.remaining+"\n");
    	}
    	*/
    	/*
    	for (Resource tree : forests) {
    		sb.append("Resource id = "+tree.id);
    		sb.append("\n\tResource type = "+tree.resource);
    		sb.append("\n\tResource locatiion = "+tree.position);
    		sb.append("\n\tResource remainig = "+ tree.remaining +"\n");
    	}
    	*/
    	double cost = getCost();
    	double heuristic = heuristic();
    	double total = cost + heuristic;
    	return "\nCost:"+cost+"\nHeuristic: "+heuristic+"\nTotal: "+total+"\nCurrent Wood = "+currentWood+"\nCurrent Gold = "+ currentGold+"\n"+sb.toString();
    }
}
