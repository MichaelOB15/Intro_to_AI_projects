package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.*;
import java.util.*;

public class RLAgent extends Agent {

    /**
     * Set in the constructor. Defines how many learning episodes your agent should run for.
     * When starting an episode. If the count is greater than this value print a message
     * and call sys.exit(0)
     */
    public final int numEpisodes;
    public static boolean evalMode;
    public static double evalCumSum = 0.0;
    public static int evalCounter = 0;
    public static List<Double> cumSums = new ArrayList<>();


    /**
     * List of your footmen and your enemies footmen
     */
    private List<Integer> friends;
    private List<Integer> foes;

    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;

    /**
     * Set this to whatever size your feature vector is.
     */
    public static final int NUM_FEATURES = 10;

    /** Use this random number generator for your epsilon exploration. When you submit we will
     * change this seed so make sure that your agent works for more than the default seed.
     */
    public final Random random = new Random(12345);

    /**
     * Your Q-function weights.
     */
    public Double[] weights;
    public double cumsum;
    public int currentEpisode;
    public double[] oldFeatures;
    public HashMap<Integer,Action> map;

    
    //public State.StateView previousState;
    //public Action previousAction;
    //if decide to remove, change code in middle and terminal step
    
    /**
     * These variables are set for you according to the assignment definition. You can change them,
     * but it is not recommended. If you do change them please let us know and explain your reasoning for
     * changing them.
     */
    public final double gamma = 0.9;
    public final double learningRate = .0001;
    public final double epsilon = .02;

    public RLAgent(int playernum, String[] args) {
        super(playernum);

        if (args.length >= 1) {
            numEpisodes = Integer.parseInt(args[0]);
            System.out.println("Running " + numEpisodes + " episodes.");
        } else {
            numEpisodes = 10;
            System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
        }

        boolean loadWeights = false;
        if (args.length >= 2) {
            loadWeights = Boolean.parseBoolean(args[1]);
        } else {
            System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        }

        if (loadWeights) {
            weights = loadWeights();
        } else {
            // initialize weights to random values between -1 and 1
            weights = new Double[NUM_FEATURES];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = random.nextDouble() * 2 - 1;
            }
        }
    }

    /**
     * We've implemented some setup code for your convenience. Change what you need to.
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {

        //this is a check to see if we are in evaluation mode or not
        //it also keeps track of how many more evaluation episodes are left
    	evalMode = evalCounter-- > 0;
    	
    	
        // Find all of your units
        friends = new ArrayList<>();
        for (Integer unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                friends.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        // Find all of the enemy units
        foes = new ArrayList<>();
        for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                foes.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }
        
        
        cumsum = 0;
        
        

        return middleStep(stateView, historyView);
    }

    /**
     * You will need to calculate the reward at each step and update your totals. You will also need to
     * check if an event has occurred. If it has then you will need to update your weights and select a new action.
     *
     * Some useful API calls here are:
	 *
     * If you are using the footmen vectors you will also need to remove killed enemies and your units which being killed. To do so use the historyView
     * to get a DeathLog. Each DeathLog tells you which player's unit died and the unit ID of the dead unit. To get
     * the deaths from the last turn do something similar to the following snippet. Please be aware that on the first
     * turn you should not call this as you will get nothing back.
     *
     ** 
     *for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
     *     System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
     * }
     **
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an event whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     **
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     **
     *
     * Remember that you can use result.getFeedback() on an ActionResult, and compare the result to an ActionFeedback enum.
     * Useful ActionFeedback values include COMPLETED, FAILED, and INCOMPLETE.
     * 
     * You can also get the ID of the unit executing an action from an ActionResult. For example,
     * result.getAction().getUnitID()
     * 
     * For this assignment it will be most useful to create compound attack actions. These will move your unit
     * within range of the enemy and then attack them once. You can create one using the static method in Action:
     * Action.createCompoundAttack(attackerID, targetID)
     * 
     * You will then need to add the actions you create to a Map that will be returned. This creates a mapping
     * between the ID of the unit performing the action and the Action object.
     * 
     * @return New actions to execute or nothing if an event has not occurred.
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
    	
    	map = new HashMap<>();
    	
    	  friends = new ArrayList<>();
          for (Integer unitId : stateView.getUnitIds(playernum)) {
              Unit.UnitView unit = stateView.getUnit(unitId);

              String unitName = unit.getTemplateView().getName().toLowerCase();
              if (unitName.equals("footman")) {
                  friends.add(unitId);
              } else {
                  System.err.println("Unknown unit type: " + unitName);
              }
          }

          // Find all of the enemy units
          foes = new ArrayList<>();
          for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
              Unit.UnitView unit = stateView.getUnit(unitId);

              String unitName = unit.getTemplateView().getName().toLowerCase();
              if (unitName.equals("footman")) {
                  foes.add(unitId);
              } else {
                  System.err.println("Unknown unit type: " + unitName);
              }
          }
    	
    	
    	
    	if(stateView.getTurnNumber() > 1) {
    		boolean event = false;
    		List<ActionResult> failedActions = new ArrayList<>();
    		List<ActionResult> completedActions = new ArrayList<>();
    		List<ActionResult> incompletedActions = new ArrayList<>();
    		
    		
    		Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
    		for(ActionResult result : actionResults.values()) {
    			
    			ActionFeedback feedback = result.getFeedback();
    			
    			if(feedback == ActionFeedback.COMPLETED) {
            	  event = true;
            	  completedActions.add(result);
    			}
    			else if(feedback == ActionFeedback.FAILED) {
    				failedActions.add(result);
    			}
    			else incompletedActions.add(result); //possible error here, there are more cases
    		}	
    		
    		
    		double[] tempWeights = new double[NUM_FEATURES];
    		for(int i = 0; i < weights.length; i++) tempWeights[i] = weights[i];
    		
    		
    		
    		if(event) {//assign new actions to peasants and update information
    			for(Integer friend : friends) {
    				cumsum += calculateReward(stateView, historyView, friend);
    				if(evalMode) {
    					Integer foe = selectAction(stateView, historyView, friend, true);
    					map.put(friend, Action.createCompoundAttack(friend, foe));
    				}
    				else {
    					
    					tempWeights = updateWeights(tempWeights, oldFeatures, 0.0, stateView, historyView, friend);
    					for(int i = 0; i < weights.length; i++) weights[i] = tempWeights[i];
    					oldFeatures = calculateFeatureVector(stateView, historyView, friend,((TargetedAction) map.get(friend)).getTargetId());
    				}
    			}
    		}
    		else {//this is not an event check for possible failed actions
    			for(ActionResult action : failedActions) {
    				if(friends.contains(action.getAction().getUnitId())) {
    					map.put(action.getAction().getUnitId(), action.getAction());
    				}
    			}
    			for(ActionResult actionResult: incompletedActions) {
    				if(friends.contains(actionResult.getAction().getUnitId())) {
    					map.put(actionResult.getAction().getUnitId(),actionResult.getAction());
    				}
    			}
    		}
    	}
    	else {//if it is the first turn
    		for(Integer friend : friends) {
    			Integer foe = selectAction(stateView, historyView, friend, evalMode);
    			map.put(friend,Action.createCompoundAttack(friend,foe));
    			oldFeatures = calculateFeatureVector(stateView, historyView, friend, foe);
    		}	
    	}
    	return map;
    }

    /**
     *
     * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
     * finished a set of test episodes you will call out testEpisode.
     *
     * It is also a good idea to save your weights with the saveWeights function.
     */
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
    	if(evalMode) {
    		evalCumSum += cumsum;
    		if(evalCounter == 0) {
    			evalCumSum /= 5.0;
    			cumSums.add(new Double(evalCumSum));
    			System.out.println("Turn: "+currentEpisode+" Average reward: " + evalCumSum);
    		}

    	}
    	else {
    		if(evalCounter == -10) {
    			evalCounter = 5;
    			evalCumSum = 0.0;
    			cumSums.clear();
    		}
    		//Save your weights
            saveWeights(weights);
    	}
    	if(++currentEpisode > 30000) {
    		File path = new File("agent_cumsums/cumsums.txt");
            path.getAbsoluteFile().getParentFile().mkdirs();
            BufferedWriter writer;
			try {
				writer = new BufferedWriter(new FileWriter(path, false));
			
                for (double val : cumSums) {
                    writer.write(String.format("%f\n", val));
                }
                writer.flush();
                writer.close();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
    		printTestData(cumSums);
    	}

    }

    /**
     * Calculate the updated weights for this agent. 
     * @param oldWeights Weights prior to update
     * @param oldFeatures Features from (s,a)
     * @param totalReward Cumulative discounted reward for this footman.
     * @param stateView Current state of the game.
     * @param historyView History of the game up until this point
     * @param footmanId The footman we are updating the weights for
     * @return The updated weight vector.
     */
    public double[] updateWeights(double[] oldWeights, double[] oldFeatures, double totalReward, State.StateView stateView, History.HistoryView historyView, int footmanId) {

    	
    	Double[] pastWeights = new Double[oldWeights.length];
    	for(int i = 0; i < oldWeights.length; i++) {
    		pastWeights[i] = new Double(oldWeights[i]);
    	}
    	Integer foe = selectAction(stateView,historyView,footmanId,evalMode);
    	map.put(footmanId, Action.createCompoundAttack(footmanId, foe));
    	double qCurrent = calcQValue(stateView, historyView, footmanId, foe);
    	double qTarget = calculateReward(stateView, historyView, footmanId) + gamma * qCurrent;
    	double qPast = dot(pastWeights,oldFeatures);
    	double loss = qTarget - qPast;
    	for(int i = 0; i < NUM_FEATURES; i ++) {
    		oldWeights[i] += learningRate * loss * oldFeatures[i];
    	}
    	return oldWeights;
    }
    
    /**
     * take the dot product of two vectors
     * @param x first vector
     * @param y second vector
     * @return the dot product of x and y
     */
    public double dot(Double[] x, double[] y){
    	int dot = 0;
    	for(int i = 0; i < x.length; i++)
    		dot += x[i]*y[i];
    	return dot;
    }

    /**
     * Given a footman and the current state and history of the game select the enemy that this unit should
     * attack. This is where you would do the epsilon-greedy action selection.
     *
     * @param stateView Current state of the game
     * @param historyView The entire history of this episode
     * @param attackerId The footman that will be attacking
     * @return The enemy footman ID this unit should attack
     */
    public int selectAction(State.StateView stateView, History.HistoryView historyView, int attackerId, boolean eval) {
        
    	if(!eval) {	//if we are in training phase, we might explore new actions	
    		if(random.nextDouble() < epsilon) //Explore new actions
    			return foes.get(random.nextInt(foes.size()));
    	}
        //Exploit known actions
        double q_max = Double.NEGATIVE_INFINITY;
        int id = -1;
        for(int defenderId : foes) {//find best one
        	double q = calcQValue(stateView, historyView, attackerId, defenderId);
        	if(q_max < q) {
       			q_max = q;
       			id = defenderId;
       		}
       	}
        return id; 
    }


    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID() + "damage: " + damageLog.getDamage());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
    public double calculateReward(State.StateView stateView, History.HistoryView historyView, int footmanId) {
    	double reward = 0.0;
    	int enemyAttacked = -1;
    	for(DamageLog damageLog : historyView.getDamageLogs(stateView.getTurnNumber() - 1)) {
    		if(damageLog.getDefenderController() == playernum && damageLog.getDefenderID() == footmanId) reward -= damageLog.getDamage();
    		else {
    			if(damageLog.getAttackerID() == footmanId) {
    				enemyAttacked = damageLog.getDefenderID();
    				reward += damageLog.getDamage() - .1;
    			}
    		}
    		
    	}
    	
    	for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() - 1)) {
    		if(deathLog.getController() == playernum) {
    			if(deathLog.getDeadUnitID() == footmanId) reward -= 100.0;
    		}
    		else {
    			if(deathLog.getDeadUnitID() == enemyAttacked) reward += 100.0;
    		}
    	}
    	
    	
    	return reward;
    }

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
    public double calcQValue(State.StateView stateView,
                             History.HistoryView historyView,
                             int attackerId,
                             int defenderId) {
    	
    	double[] features = calculateFeatureVector(stateView,historyView,attackerId,defenderId);
    	return(dot(weights,features));
    }
    

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * for example: HP 
     * UnitView attacker = stateView.getUnit(attackerId);
     * attacker.getHP()
     * 
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
    public double[] calculateFeatureVector(State.StateView stateView,
            History.HistoryView historyView,
            int attackerId,
            int defenderId) {

		double[] features = new double[NUM_FEATURES];
		features[0] = 1;
		
		// (net) ratio of friendly health over enemy health 
		double totFoeHealth = 0.0;
		for (int i = 0; i < foes.size(); i++) {
			totFoeHealth += stateView.getUnit(foes.get(i)).getHP();
		}
		double totFriendHealth = 0.0;
		for (int i = 0; i < friends.size(); i++) {
			totFriendHealth += stateView.getUnit(friends.get(i)).getHP();
		}
		
		features[1] = totFriendHealth;
		features[2] = totFoeHealth;	
		
		// ratio of friendly to enemy attacking (we want more out units next to theirs rather than theirs next to ours)
		double friendliesNextToEnemies = 0.0;
		double enemiesNextToFriendlies = 0.0;
		for (int i = 0; i < friends.size(); i++) {
			for (int j = 0; j < foes.size(); j++) {
				if (isAdjecent(stateView.getUnit(friends.get(i)).getXPosition(),stateView.getUnit(friends.get(i)).getYPosition(),stateView.getUnit(foes.get(j)).getXPosition(),stateView.getUnit(foes.get(j)).getYPosition())) {
					friendliesNextToEnemies += 1;
					break;
				}
			}
		}
		for (int i = 0; i < foes.size(); i++) {
			for (int j = 0; j < friends.size(); j++) {
				if (isAdjecent(stateView.getUnit(friends.get(j)).getXPosition(),stateView.getUnit(friends.get(j)).getYPosition(),stateView.getUnit(foes.get(i)).getXPosition(),stateView.getUnit(foes.get(i)).getYPosition())) {
					enemiesNextToFriendlies += 1;
					break;
				}
			}
		}
		
		
		features[3] = friendliesNextToEnemies;
		features[4] = enemiesNextToFriendlies;
		
		/*
		// we want to be as little spread out comparitvly to the enemy (minimize our closeness to the enemy while maximizing their closeness to us)
		double totalClosestFoes = 0.0;
		double totalClosestFriends = 0.0;
		for (int i = 0; i < friends.size(); i++) {
			totalClosestFoes += closestOpposing(friends.get(i), true, stateView);
		}
		for (int i = 0; i < foes.size(); i++) {
			totalClosestFriends += closestOpposing(foes.get(i), false, stateView);
		}
		features[3] = totalClosestFoes - totalClosestFriends;
		
		// average enemy position our people are equally spaced around the avg enemy position
		double combinedX = 0.0, combinedY = 0.0;
		for (int i = 0; i < foes.size(); i++) {
			combinedX += stateView.getUnit(foes.get(i)).getXPosition();
			combinedY += stateView.getUnit(foes.get(i)).getYPosition();
		}
		double avgX = combinedX/foes.size();
		double avgY = combinedY/foes.size();
		double avgDist = 0.0;
		for (int i = 0; i < friends.size(); i++) {
			avgDist += chebyshevDistance(stateView.getUnit(friends.get(i)).getXPosition(),stateView.getUnit(friends.get(i)).getXPosition(),avgX,avgY);
		}
		avgDist = avgDist / friends.size();
		double variance = 0.0;
		for (int i = 0; i<friends.size(); i++) {
			variance += chebyshevDistance(stateView.getUnit(friends.get(i)).getXPosition(),stateView.getUnit(friends.get(i)).getXPosition(),avgX,avgY) - avgDist;
		}
		features[4] = variance;
		
		// attacker health minus defender health 
		double AttackerHP = stateView.getUnit(attackerId).getHP();
		double DefenderHP = stateView.getUnit(defenderId).getHP();
		features[5] = AttackerHP - DefenderHP;
		*/
		
		// grids to outnumber enemies (more outnumbers than undernumbers) 
		// if average health of enemy is higher than avg health of friends we want to run away to another grid 
		
		// we control more space than them 
		// we are farther from a corner than them
		
		
		
		// friendly damage dealt last turn
		
		// 
		
		
		
		
		//distance away from attacking (
		double dist = Math.abs(stateView.getUnit(attackerId).getXPosition()-stateView.getUnit(defenderId).getXPosition())+Math.abs(stateView.getUnit(attackerId).getYPosition()-stateView.getUnit(defenderId).getYPosition());
		features[5] = dist;
		
		//attacker health
		double AttackerHP = stateView.getUnit(attackerId).getHP();
		features[6] = AttackerHP;
		
		//defender health
		double DefenderHP = stateView.getUnit(defenderId).getHP();
		features[7] = DefenderHP;
		
		features[8] = friends.size();
		
		features[9] = foes.size();
		
		//is attacker being attacked
		
		//how many people are attacking attacker or defender
		
		//are our units more surrounding the enemies or more surrounded by the enemies
		
		
		return features;
	}
	
	public boolean isAdjecent(int playerX, int playerY, int enemyX, int enemyY) {
		return Math.abs(playerX - enemyX) <= 1 && Math.abs(playerY - enemyY) <= 1;
	}
	
	public double closestOpposing(int id, boolean friend, State.StateView stateView) {
		int xPosition = stateView.getUnit(id).getXPosition();
		int yPosition = stateView.getUnit(id).getYPosition();
		double closestDist = Integer.MAX_VALUE;
		if (friend) {
			for (int i = 0; i < foes.size(); i++) {
				double newDist = chebyshevDistance(xPosition,yPosition,stateView.getUnit(foes.get(i)).getXPosition(),stateView.getUnit(foes.get(i)).getYPosition());
				closestDist = Math.min(closestDist, newDist);
			}
		}
		else {
			for (int i = 0; i < friends.size(); i++) {
				double newDist = chebyshevDistance(xPosition,yPosition,stateView.getUnit(friends.get(i)).getXPosition(),stateView.getUnit(friends.get(i)).getYPosition());
				closestDist = Math.min(closestDist, newDist);
			}
		}
		return closestDist;
	}
	
	public double chebyshevDistance(double playerX, double playerY, double enemyX, double enemyY) {
        return Math.max(Math.abs(playerX - enemyX), Math.abs(playerY - enemyY));
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include th output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(Double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public Double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            List<Double> weights = new LinkedList<>();
            while((line = reader.readLine()) != null) {
                weights.add(Double.parseDouble(line));
            }
            reader.close();

            return weights.toArray(new Double[weights.size()]);
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
