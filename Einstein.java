package risk;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;

public class Einstein extends AIPlayer
{
	private Continent[] continents;
	
	private Map map;
	
	private ArrayList<Plan> conquestPlans;
	//private ArrayList<Plan> threatPlans;
	//private ArrayList<Plan> eliminatePlans;
	//private ArrayList<Plan> defendPlans;
	//private ArrayList<Plan> expandPlans;
	
	public Einstein(int num, Color color, RiskGame rg)
	{
		super(num, color, rg);
		img = new ImageIcon(getClass().getResource("ai/Albert Einstein.jpg"));
		map = game.getMap();
		continents = map.getContinents();
	}
	
	private void buildConquestPlans()
	{
		conquestPlans = new ArrayList<Plan>();
		int troops = game.getBonus(this);
		ArrayList<Continent> conts = new ArrayList<Continent>();
		for (Continent cont : continents)
			conts.add(cont);
		
		for (int i = 0; i < continents.length && troops > 0; i++)
		{
			Continent[] currentConts = new Continent[conts.size()];
			conts.toArray(currentConts);
			Continent optimalCont = getOptimalContinent(currentConts);
			conts.remove(optimalCont);
			double score = getContinentScore(optimalCont);
			if (score > 2)
				troops = planConquest(optimalCont, troops);
			else if (score > 1)
				troops = distribute(optimalCont, troops);
		}
	}
	
	private int planConquest(Continent cont, int troops)
	{
		Plan conquestPlan = new Plan();
		Territory[] enemies = cont.getEnemyTerritories(this);
		for (int i = 0; i < enemies.length && troops > 0; i++)
		{
			Territory nextEnemy = null;
			Territory nextAlly = null;
			Territory attackChainStart = null;
			int maxDiff = Integer.MIN_VALUE;
			for (Territory enemy : enemies)
			{
				Territory[] allyConns = enemy.getFriendlyConnectors(this);
				for (Territory allyConn : allyConns)
				{
					int diff = allyConn.units - enemy.units;
					if (diff > maxDiff || (diff == maxDiff && allyConn.getEnemyConnectors(this).length > nextAlly.getEnemyConnectors(this).length))
					{
						maxDiff = diff;
						nextAlly = allyConn;
						nextEnemy = enemy;
					}
				}
				
				Territory[] enemyConns = enemy.getEnemyConnectors(this);
				for (Territory enemyConn : enemyConns)
				{
					if (conquestPlan.containsAttackTo(enemyConn))
					{
						Attack attack = conquestPlan.getAttackTo(enemyConn);
						int troopsLost = attack.to.units;
						while (attack.from.owner != this)
						{
							troopsLost += attack.from.units;
							attack = conquestPlan.getAttackTo(attack.from);
						}
						int diff = attack.from.units - troopsLost - enemy.units;
						if (diff > maxDiff || (diff == maxDiff && attack.from.getEnemyConnectors(this).length > nextAlly.getEnemyConnectors(this).length))
						{
							maxDiff = diff;
							nextAlly = enemyConn;
							nextEnemy = enemy;
							attackChainStart = attack.from;
						}
					}
				}
			}
			
			if (nextAlly.owner == this)
			{
				for (int j = maxDiff - 4; j <= 0 && troops > 0; j++)
				{
					conquestPlan.deploy(nextAlly);
					troops--;
				}
			}
			else
			{
				for (int j = maxDiff - 4; j <= 0 && troops > 0; j++)
				{
					conquestPlan.deploy(attackChainStart);
					troops--;
				}
			}
			conquestPlan.attack(nextAlly, nextEnemy);
		}
		conquestPlans.add(conquestPlan);
		return troops;
	}
	
	private int distribute(Continent cont, int troops)
	{
		Plan conquestPlan = new Plan();
		Territory[] allies = cont.getFriendlyTerritories(this);
		for (int i = 0; i < allies.length && troops > 0; i++)
		{
			int maxCount = -1;
			Territory optimalTerrit = null;
			for (Territory ally : allies)
			{
				if (!conquestPlan.containsDeployment(ally))
				{
					Territory[] enemyConns = ally.getEnemyConnectors(this);
					int count = 0;
					for (Territory enemyConn : enemyConns)
					{
						if (cont.hasTerritory(enemyConn))
							count += enemyConn.units;
					}
					
					if (count > maxCount || (count == maxCount && ally.units > optimalTerrit.units))
					{
						maxCount = count;
						optimalTerrit = ally;
					}
				}
			}
			
			while (optimalTerrit.units - maxCount < 2 && troops > 0)
			{
				conquestPlan.deploy(optimalTerrit);
				troops--;
			}
		}
		conquestPlans.add(conquestPlan);
		return troops;
	}
	
	public void place()
	{
		Continent optimalCont = getOptimalContinent(map.getContinents());
		Territory territ = getOptimalPlacementTerritory(optimalCont);
		message("Placing on " + territ.name + " in " + optimalCont.name);
		game.place(territ);
	}
	
	public void deploy()
	{
		buildConquestPlans();
		for (int i = 0; i < conquestPlans.size(); i++)
		{
			conquestPlans.get(i).runDeployment();
		}
	}
	
	public void attack()
	{
		for (int i = 0; i < conquestPlans.size(); i++)
			conquestPlans.get(i).runAttack();
		game.endAttacks();
	}
	
	public void fortify()
	{
		game.endFortifications();
	}
	
	private Territory getOptimalPlacementTerritory(Continent cont)
	{
		Territory[] territs = cont.getFriendlyTerritories(this);
		Territory optimalTerrit = null;
		int maxConnectors = -1;
		int maxTotalConnectors = -1;
		for (Territory territ : territs)
		{
			Territory[] conns = territ.getEnemyConnectors(this);
			int count = conns.length;
			for (Territory conn : conns)
			{
				if (!cont.hasTerritory(conn))
					count--;
			}
			
			if (count > maxConnectors || (count == maxConnectors && conns.length > maxTotalConnectors))
			{
				optimalTerrit = territ;
				maxConnectors = count;
				maxTotalConnectors = conns.length;
			}
		}
		return optimalTerrit;
	}
	
	private Continent getOptimalContinent(Continent[] conts)
	{
		Continent optimalCont = null;
		double maxScore = Double.NEGATIVE_INFINITY;
		for (Continent cont : conts)
		{
			if (cont.getBonus() > 0 && !cont.hasContinent(this))
			{
				double score = getContinentScore(cont);
				if (score > maxScore)
				{
					maxScore = score;
					optimalCont = cont;
				}
			}
		}
		return optimalCont;
	}
	
	private double getContinentScore(Continent cont)
	{
		int friendlyTroops = sumTroops(cont.getFriendlyTerritories(this));
		int enemyTroops = sumTroops(cont.getEnemyTerritories(this));
		
		Territory[] myTerrits = map.getTerritories(this);
		for (Territory myTerrit : myTerrits)
		{
			if (!cont.hasTerritory(myTerrit))
			{
				int chain = getShortestChainToContinent(myTerrit, cont, myTerrit.units);
				if (chain < myTerrit.units)
					friendlyTroops += myTerrit.units - chain;
			}
		}
		
		friendlyTroops += game.getBonus(this);
		return friendlyTroops * Math.sqrt(Math.sqrt(cont.getBonus())) / enemyTroops;
	}
	
	private int getShortestChainToContinent(Territory territ, Continent cont, int max)
	{
		HashMap<Territory, Integer> paths = new HashMap<Territory, Integer>();
		HashMap<Territory, Boolean> checked = new HashMap<Territory, Boolean>();
		checked.put(territ, true);
		paths.put(territ, 0);
		ArrayList<Territory> toCheck = new ArrayList<Territory>();
		toCheck.add(territ);
		int minPath = max;
		while (toCheck.size() > 0)
		{
			Territory currentTerrit = toCheck.remove(0);
			int currentPath = paths.get(currentTerrit);
			
			Territory[] connectors = currentTerrit.getConnectors();
			for (Territory conn : connectors)
			{
				if (!checked.containsKey(conn) && currentPath + conn.units < max && !cont.hasTerritory(conn))
				{
					toCheck.add(conn);
					checked.put(conn, true);
					int connPath = (conn.owner != this) ? currentPath + conn.units : currentPath;
					if (!paths.containsKey(conn))
						paths.put(conn, connPath);
					else
					{
						if (connPath < paths.get(conn))
						{
							paths.remove(conn);
							paths.put(conn, connPath);
						}
					}
				}
				else if (cont.hasTerritory(conn) && currentPath < minPath)
					minPath = currentPath;
			}
		}
		return minPath;
	}
	
	private static int sumTroops(Territory[] territs)
	{
		int sum = 0;
		for (Territory territ : territs)
			sum += territ.units;
		return sum;
	}
	
	private class Plan
	{
		private ArrayList<Territory> deployments = new ArrayList<Territory>();
		private ArrayList<Attack> attacks = new ArrayList<Attack>();
		
		public Plan()
		{
			
		}
		
		public void runDeployment()
		{
			for (Territory territ : deployments)
			{
				game.deploy(territ, false);
				message("Deploying on " + territ.name);
			}
		}
		
		public void runAttack()
		{
			for (int i = 0; i < attacks.size(); i++)
			{
				Attack attack = attacks.get(i);
				if (attack.from.units > 1 && attack.from.owner == Einstein.this)
				{
					Einstein.this.attack(attack.from, attack.to, true);
					if (game.state == RiskGame.State.ADVANCE)
					{
						Attack attackFrom = getAttackFrom(attack.from);
						Attack attackTo = getAttackFrom(attack.to);
						if (attackFrom == null)
						{
							game.advance(attack.to, true);
							continue;
						}
						if (attackTo == null)
						{
							game.advance(attack.from, true);
							continue;
						}
						double ratio = (double) attackTo.to.units / attackFrom.to.units;
						int toAdvance = (int) (ratio * attack.from.units);
						for (int j = 0; j < toAdvance; j++)
							game.advance(attack.to, false);
					}
				}
			}
		}
		
		public void deploy(Territory territ)
		{
			deployments.add(territ);
		}
		
		public void attack(Territory from, Territory to)
		{
			attacks.add(new Attack(from, to));
		}
		
		public Attack getAttackTo(Territory to)
		{
			for (Attack attack : attacks)
			{
				if (attack.to == to)
					return attack;
			}
			return null;
		}
		
		public Attack getAttackFrom(Territory from)
		{
			for (Attack attack : attacks)
			{
				if (attack.from == from)
					return attack;
			}
			return null;
		}
		
		public boolean containsDeployment(Territory territ)
		{
			return deployments.contains(territ);
		}
		
		public boolean containsAttackTo(Territory to)
		{
			for (Attack attack : attacks)
			{
				if (attack.to == to)
					return true;
			}
			return false;
		}
	}
	
	private class Attack
	{
		public Territory from;
		public Territory to;
		
		public Attack(Territory from, Territory to)
		{
			this.from = from;
			this.to = to;
		}
	}
}
