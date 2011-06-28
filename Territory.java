package risk;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.Border;

public class Territory 
{
	private static final int DEFAULT_UNITS = 3;
	private static final int X_OFFSET = -41;
	private static final int Y_OFFSET = -51;
	private static final int BTN_WIDTH = 22;
	private static final int BTN_HEIGHT = 16;
	
	private static final int REDUCE_FONT_LIMIT = 100;
	
	public static final Border BORDER_HIGHLIGHT = BorderFactory.createLineBorder(Color.WHITE, 2);
	
	public String name;
	public int x;
	public int y;
	public int units = DEFAULT_UNITS;
	public Player owner;
	
	private Territory[] connectors;
	private final JButton btn = new JButton(Integer.toString(DEFAULT_UNITS));
	
	private static final Font SMALL_FONT = new Font("Monospaced", Font.BOLD, 10);
	private static final Font LARGE_FONT = new Font("Monospaced", Font.BOLD, 11);
	
	public Territory(String name, int x, int y)
	{
		this.name = name;
		this.x = x;
		this.y = y;
		
		btn.setName(name);
		btn.setFont(LARGE_FONT);
		btn.setFocusable(false);
		btn.setBorder(null);
		btn.setFocusPainted(false);
		btn.setForeground(Color.BLACK);
		btn.setBounds(x + X_OFFSET, y + Y_OFFSET, BTN_WIDTH, BTN_HEIGHT);
	}
	
	public void addMouseListener(MouseListener listener)
	{
		btn.addMouseListener(listener);
	}
	
	public void connect(Territory[] conns)
	{
		connectors = conns;
	}
	
	public void addUnits(int num)
	{
		if (num != 0)
		{
			units += num;
			btn.setText(Integer.toString(units));
			if (units >= REDUCE_FONT_LIMIT)
				btn.setFont(SMALL_FONT);
			else
				btn.setFont(LARGE_FONT);
		}
	}
	
	public void setUnits(int num)
	{
		if (num != units)
		{
			units = num;
			btn.setText(Integer.toString(units));
			if (units >= REDUCE_FONT_LIMIT)
				btn.setFont(SMALL_FONT);
			else
				btn.setFont(LARGE_FONT);
		}
	}
	
	public void setOwner(Player newOwner)
	{
		owner = newOwner;
		btn.setBackground(newOwner.color);
	}
	
	public JButton getButton()
	{
		return btn;
	}
	
	public void hilite()
	{
		btn.setBorder(BORDER_HIGHLIGHT);
		btn.setForeground(Color.WHITE);
	}
	
	public void unhilite()
	{
		btn.setBorder(null);
		btn.setForeground(Color.BLACK);
	}
	
	public Territory[] getConnectors()
	{
		return connectors;
	}
	
	public Territory[] getFriendlyConnectors(Player player)
	{
		ArrayList<Territory> friends = new ArrayList<Territory>();
		for (int i = 0; i < connectors.length; i++)
		{
			if (connectors[i].owner == player)
				friends.add(connectors[i]);
		}
		Territory[] friendTerrits = new Territory[friends.size()];
		friends.toArray(friendTerrits);
		return friendTerrits;
	}
	
	public Territory[] getEnemyConnectors(Player player)
	{
		ArrayList<Territory> enemies = new ArrayList<Territory>();
		for (int i = 0; i < connectors.length; i++)
		{
			if (connectors[i].owner != player)
				enemies.add(connectors[i]);
		}
		Territory[] enemyTerrits = new Territory[enemies.size()];
		enemies.toArray(enemyTerrits);
		return enemyTerrits;
	}
	
	public boolean isConnecting(Territory territ)
	{
		for (Territory conn : connectors)
		{
			if (conn == territ)
				return true;
		}
		return false;
	}
	
	public boolean isFortifyConnecting(Territory target)
	{
		if (isConnecting(target))
			return true;
		
		HashMap<Territory, Boolean> checked  = new HashMap<Territory, Boolean>();
		ArrayList<Territory> territs = new ArrayList<Territory>();
		territs.add(this);
		while (territs.size() > 0)
		{
			Territory territ = territs.remove(0);
			if (target == territ)
				return true;
			checked.put(territ, true);
			
			Territory[] conns = territ.getFriendlyConnectors(owner);
			for (Territory conn : conns)
			{
				if (!checked.containsKey(conn))
					territs.add(conn);
			}
		}
		return false;
	}
}
