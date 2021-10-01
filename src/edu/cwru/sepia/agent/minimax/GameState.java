package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.minimax.GameState.Player;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
	static final Direction[] directions = {Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST};
	boolean max;
    ArrayList<MapLocation> resources;
    ArrayList<Player> friends;
    ArrayList<Player> foes;
    int xDim;
    int yDim;
    
    public class MapLocation{
    	int x;
    	int y;
    	
    	public MapLocation(int x, int y) {
    		this.x = x;
    		this.y = y;
    	}
    	
    	@Override
    	public boolean equals(Object o) {
    		if (o instanceof MapLocation) {	
    			MapLocation loc = (MapLocation)o;
    			return (this.x == loc.x && this.y == loc.y);
    		}
    		else return super.equals(o);
    			
    	}
    	
		@Override
		public int hashCode() {
			return this.x*this.x + this.y*this.y;
		}
    }
    
    public class Player extends MapLocation{
    	
    	int health;
    	int damage;
    	int id;
    	int range;
    	public Player(int health, int damage, int id, int range, int x, int y) {
    		super(x,y);
    		this.health = health;
    		this.damage = damage;
    		this.range = range;
    		this.id = id;
    	}
    }
	
	

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs beloning to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
    	xDim = state.getXExtent();
    	yDim = state.getYExtent();
    	max = true;
    	
    	List<Integer> playerUnitIDs = state.getUnitIds(0);
    	List<Integer> enemyUnitIDs = state.getUnitIds(1);
   
    	resources = new ArrayList<MapLocation>();
    	friends = new ArrayList<Player>();
    	foes = new ArrayList<Player>();
    	
    	for (ResourceView r : state.getAllResourceNodes()) {
    		resources.add(new MapLocation(r.getXPosition(), r.getYPosition()));
    	}
    	
    	for(Integer unit : playerUnitIDs) {
    		UnitView u = state.getUnit(unit);
    		friends.add(new Player(u.getHP(),u.getTemplateView().getBasicAttack(),unit,u.getTemplateView().getRange(),u.getXPosition(),u.getYPosition()));
    	}
    	
    	for(Integer unit : enemyUnitIDs) {
    		UnitView u = state.getUnit(unit);
    		foes.add(new Player(u.getHP(),u.getTemplateView().getBasicAttack(),unit,u.getTemplateView().getRange(),u.getXPosition(),u.getYPosition()));
    	}
    	
    
    }
    
    public GameState(ArrayList<Player> friends, ArrayList<Player> foes, ArrayList<MapLocation> resources, int xDim, int yDim, boolean max){
    	this.friends = friends;
    	this.foes = foes;
    	this.resources = resources;
    	this.xDim = xDim;
    	this.yDim = yDim;
    	this.max = max;

    }
    
    
    

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
    	double distFromArcher = distanceFromArcher();
    	double centerOfBoard = centerOfBoard();
    	double foeHealth = foeHealth();
    	
    	double linCombination = distFromArcher + foeHealth + centerOfBoard;
    	return (linCombination);
    	
    }
    
    public double distanceFromArcher() {
    	ArrayList<Player> playerUnitIDs;
    	ArrayList<Player> enemyUnitIDs;
    	
    	if (this.max) {
        	playerUnitIDs = this.friends;
        	enemyUnitIDs = this.foes;
    	}
    	else {
        	playerUnitIDs = this.foes;
        	enemyUnitIDs = this.friends;
    	}
    	

    	double estimate = 0.0;
    	
    	// current heuristic is the combined manhattan distance for all players against all enemies
    	for (Player player: playerUnitIDs) {
    		double smallest = Double.POSITIVE_INFINITY;
    		for (Player enemy: enemyUnitIDs)  {
    			smallest = Math.min(Math.max(Math.abs(player.x-enemy.x), Math.abs(player.y-enemy.y)), smallest);
    		}
    		estimate += smallest;
    	}
    	return (estimate);

    }
    
    public double centerOfBoard() {
    	ArrayList<Player> enemyUnitIDs;
    	
    	if (this.max) {
        	enemyUnitIDs = this.foes;
    	}
    	else {
        	enemyUnitIDs = this.friends;
    	}
    	
    	double estimate = 0.0;
    	for (Player enemy: enemyUnitIDs) {
    		double dx = Math.abs(enemy.x - 12);
    		double dy = Math.abs(enemy.y - 9.5);
    		estimate += Math.sqrt(dx*dx+dy*dy);
    	}
    	return (1/estimate);
    }
    
    public double foeHealth() {
    	ArrayList<Player> enemyUnitIDs;
    	
    	if (this.max) {
        	enemyUnitIDs = this.foes;
    	}
    	else {
    		enemyUnitIDs = this.friends;
    	}
    	
    	double health = 0.0;
    	for (Player enemy: enemyUnitIDs) {
    		health += enemy.health;
    	}
    	return (1/health);
    }

    
    public GameState update(Action action, GameState state) {
    	int unit = action.getUnitId();
    	Player doer = null;
    	ArrayList<Player> buddies = new ArrayList<>();
    	ArrayList<Player> enemies = new ArrayList<>();
    	for(Player p : state.friends) {
    		if(p.id == unit) {
    			doer = new Player(p.health,p.damage,p.id,p.range, p.x, p.y);
    		}
    		else buddies.add(p);
    	}
    	if(action.getType() == ActionType.PRIMITIVEMOVE) {
    		Direction d =((DirectedAction)action).getDirection();
    		doer.x -= d.xComponent();
    		doer.y -= d.yComponent();
    	}

		Player victim = null;
		for(Player p : state.foes) {
			if(action.getType() == ActionType.PRIMITIVEATTACK) {
				if(((TargetedAction)action).getTargetId() == p.id) {
					victim = new Player(p.health - doer.damage,p.damage,p.id,p.range,p.x,p.y);
					if(victim.health > 0) {
						enemies.add(victim);
					}
				}
			}
			else enemies.add(p);
		}
    		
    	buddies.add(doer);
    	return new GameState(buddies, enemies, state.resources, state.xDim, state.yDim, state.max);
    }
    
    public boolean inBounds(Player p, Direction d) {
    	int x = p.x + d.xComponent();
    	int y = p.y + d.yComponent();
    	return x <= xDim && y <= yDim && x >= 0 && y >= 0;
    }
    
    public boolean isResource(Player p, Direction d) {
    	int x = p.x + d.xComponent();
    	int y = p.y + d.yComponent();
    	for(Player friend : friends)
    		if(friend.x == x && friend.y == y) return true;
    	return false;
    }
    
    public boolean isFriendlyOccupied(Player p, Direction d) {
    	int x = p.x + d.xComponent();
    	int y = p.y + d.yComponent();
    	for(Player friend : friends) {
    		if (friend.x == x && friend.y == y) return true;
    	}
    	return false;
    }
    
    public boolean isFoeOccupied(Player p, Direction d) {
    	int x = p.x + d.xComponent();
    	int y = p.y + d.yComponent();
    	for(Player foe : foes) {
    		if (foe.x == x && foe.y == y) return true;
    	}
    	return false;
    }
    
    public boolean canMakeAction(Player p, Direction d) {
    	return inBounds(p,d) && !isResource(p,d) && !isFriendlyOccupied(p,d);
    }
    
    public Player getEnemy(Player p, Direction d) {
    	int x = p.x + d.xComponent();
    	int y = p.y + d.yComponent();
    	for(Player enemy : foes)
    		if(enemy.x == x && enemy.y == y) return enemy;
    	return null;
    	
    }
    
    public ArrayList<Action> createFootmanActions(Player p) {
    	ArrayList<Action> actions = new ArrayList<>();
    	for(int i = 0; i < 4; i++) {
    		if(canMakeAction(p,directions[i])){
    			if(getEnemy(p,directions[i]) == null) {
    				actions.add(Action.createPrimitiveMove(p.id, directions[i]));
    			} else {
    				actions.add(Action.createPrimitiveAttack(p.id,getEnemy(p,directions[i]).id));
    			}
    		}
    	}
    	return actions;
    }
    
    public ArrayList<Action> createArcherActions(Player p) {
    	ArrayList<Action> actions = new ArrayList<>();
    	for(int i = 0; i < 4; i++) {
    		if(canMakeAction(p,directions[i])){
    			actions.add(Action.createPrimitiveMove(p.id, directions[i]));
    		}
    	}
    	for(Player enemy : foes) {
    		double dist = Math.sqrt(((enemy.x - p.x) * (enemy.x - p.x)) + ((enemy.y - p.y) * (enemy.y - p.y)));
    		if(dist <= p.range)
    			actions.add(Action.createPrimitiveAttack(p.id, enemy.id));
    	}
    	
    	return actions;
    }
    
    public ArrayList<Action> createAllActions(Player p){
    	return max ? createFootmanActions(p) : createArcherActions(p);
    }
    
    public GameState simulate(HashMap<Integer,Action> map) {
    	GameState state = this;
    	for(Integer key : map.keySet()) {
    		state = update(map.get(key),state);
    	}
    	return new GameState(state.foes,state.friends,state.resources,state.xDim,state.yDim,!state.max);
    }
    
    public ArrayList<HashMap<Integer,Action>> createPlayerActionTable(ArrayList<ArrayList<Action>> actions){
        ArrayList<HashMap<Integer,Action>> table = new ArrayList<>();
        if(friends.size() == 1) {
         for(Action a : actions.get(0)) {
          HashMap<Integer,Action> map = new HashMap<Integer,Action>();
          map.put(a.getUnitId(),a);
          table.add(map);
         }
        }
        else {
         for(int i = 0; i < actions.size() - 1; i ++) {
            for(int j = 0; j < actions.get(i).size(); j++) {
           for(int k = i + 1; k < actions.size(); k++) {
            for(int l = 0; l < actions.get(k).size(); l++) {
             HashMap<Integer,Action> map = new HashMap<>();
             map.put(actions.get(i).get(j).getUnitId(),actions.get(i).get(j));
             map.put(actions.get(k).get(l).getUnitId(),
               actions.get(k).get(l));
             table.add(map);
            }
           }
          }
           }
        }
        return table;
       }
    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
    	List<GameStateChild> children = new ArrayList<>();
    	ArrayList<ArrayList<Action>> actions = new ArrayList<>();
    	
    	for(Player friend : friends) 
    		actions.add(createAllActions(friend));
        
        for(HashMap<Integer,Action> map : createPlayerActionTable(actions))
        	children.add(new GameStateChild(map,simulate(map)));
      
      return children;
    }
}
