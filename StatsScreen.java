package risk;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class StatsScreen extends JPanel
{
	private static final Font STATS_BUTTON_FONT = new Font("Arial", Font.PLAIN, 11);
	private static final Font GRAPH_FONT = new Font("Arial", Font.BOLD, 16);
	private static final BasicStroke LINE_GRAPH_STROKE = new BasicStroke(5);
	private static final int LINE_PT_SIZE = 4;
	
	private static final int OFFSET_LEFT = 40;
	private static final int OFFSET_RIGHT = 40;
	private static final int OFFSET_TOP = 20;
	private static final int OFFSET_BOTTOM = 40;
	
	private static final int BAR_HEIGHT = 25;
	private static final int BAR_SPACE = 40;
	private static final int TEXT_INDENT = 5;
	private static final int VALUE_INDENT = 8;
	private static final int TEXT_TOP_OFFSET = 36;
	
	private Graphics2D g2d;
	private JPanel canvas = new JPanel();
	private boolean lineGraph = false;
	private JButton currentStatBtn = null;
	private int currentStats = Player.TROOPS;
	
	private Player[] players;
	
	public StatsScreen(Player[] players)
	{
		super();
		this.players = players;
		populateScreen();
		
		addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent event)
			{
				canvas.requestFocus();
			}
		});
	}
	
	private void populateScreen()
	{
		setLayout(new BorderLayout());
		canvas.setFocusable(true);
		canvas.addFocusListener(new FocusAdapter()
		{
			public void focusGained(FocusEvent event)
			{
				drawGraph();
			}
		});
		
		JPanel optionsPnl = new JPanel();
		optionsPnl.add(createStatButton("Troops", Player.TROOPS));
		optionsPnl.add(createStatButton("Territories", Player.TERRITORIES));
		optionsPnl.add(createStatButton("Luck", Player.LUCK_FACTOR));
		optionsPnl.add(createStatButton("Bonus", Player.BONUS));
		optionsPnl.add(createStatButton("Deployed", Player.TROOPS_DEPLOYED));
		optionsPnl.add(createStatButton("Killed", Player.TROOPS_KILLED));
		optionsPnl.add(createStatButton("Perished", Player.TROOPS_LOST));
		optionsPnl.add(createStatButton("Conquered", Player.TERRITORIES_CONQUERED));
		optionsPnl.add(createStatButton("Lost", Player.TERRITORIES_LOST));
		
		final JComboBox displayOptionsCombo = new JComboBox(new String[] {"Bar Graph", "Line Graph"});
		displayOptionsCombo.setFont(new Font("Arial", Font.PLAIN, 11));
		displayOptionsCombo.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (displayOptionsCombo.getSelectedIndex() == 0)
					lineGraph = false;
				else
					lineGraph = true;
				canvas.requestFocus();
			}
		});
		optionsPnl.add(displayOptionsCombo);
		add(optionsPnl, BorderLayout.PAGE_START);
		add(canvas, BorderLayout.CENTER);
	}
	
	private void drawLineGraph()
	{
		int[][] data = new int[players.length][];
		int maxValue = 0;
		int latestRound = 0;
		for (int i = 0; i < players.length; i++)
		{
			data[i] = players[i].getStats(currentStats);
			if (data[i].length > latestRound)
				latestRound = data[i].length;
			for (int j = 0; j < data[i].length; j++)
			{
				if (Math.abs(data[i][j]) > maxValue)
					maxValue = Math.abs(data[i][j]);
			}
		}
		
		if (currentStats == Player.LUCK_FACTOR)
		{
			drawLuckLineGraph(data, maxValue, latestRound);
			return;
		}
		
		int height = canvas.getHeight();
		int width = canvas.getWidth();
		
		double factorX = ((double) (width - OFFSET_LEFT - OFFSET_RIGHT)) / (latestRound - 1);
		double factorY = ((double) (height - OFFSET_TOP - OFFSET_BOTTOM)) / maxValue;
		
		g2d.setColor(Color.BLACK);
		g2d.setStroke(LINE_GRAPH_STROKE);
		g2d.drawLine(OFFSET_LEFT, height - OFFSET_BOTTOM, width - OFFSET_RIGHT, height - OFFSET_BOTTOM);
		g2d.drawLine(OFFSET_LEFT, OFFSET_TOP, OFFSET_LEFT, height - OFFSET_BOTTOM);
		
		for (int i = 0; i < players.length; i++)
		{
			int[] playerData = data[i];
			g2d.setColor(players[i].color);
			for (int j = 1; j < playerData.length; j++)
			{
				if (playerData[j] >= 0)
				{
					int x1 = (int) ((j - 1) * factorX) + OFFSET_LEFT;
					int y1 = height - OFFSET_BOTTOM - (int) (playerData[j - 1] * factorY);
					int x2 = (int) (j * factorX) + OFFSET_LEFT;
					int y2 = height - OFFSET_BOTTOM - (int) (playerData[j] * factorY);
					g2d.drawOval(x2 - LINE_PT_SIZE / 2, y2 - LINE_PT_SIZE / 2, LINE_PT_SIZE, LINE_PT_SIZE);
					g2d.drawLine(x1, y1, x2, y2);
				}
			}
		}
	}
	
	private void drawLuckLineGraph(int[][] data, int maxValue, int latestRound)
	{
		int height = canvas.getHeight();
		int width = canvas.getWidth();
		
		double factorX = ((double) (width - OFFSET_LEFT - OFFSET_RIGHT)) / (latestRound - 1);
		double factorY = ((double) (height - OFFSET_TOP - OFFSET_BOTTOM)) / (2 * maxValue);
		
		int luckOffset = (height - OFFSET_BOTTOM - OFFSET_TOP) / 2 + OFFSET_TOP;
		
		g2d.setColor(Color.BLACK);
		g2d.setStroke(LINE_GRAPH_STROKE);
		g2d.drawLine(OFFSET_LEFT, luckOffset, width - OFFSET_RIGHT, luckOffset);
		g2d.drawLine(OFFSET_LEFT, OFFSET_TOP, OFFSET_LEFT, height - OFFSET_BOTTOM);
		
		for (int i = 0; i < players.length; i++)
		{
			int[] playerData = data[i];
			g2d.setColor(players[i].color);
			for (int j = 1; j < playerData.length; j++)
			{
				int x1 = (int) ((j - 1) * factorX) + OFFSET_LEFT;
				int y1 = luckOffset - (int) ((playerData[j-1] * factorY));
				int x2 = (int) (j * factorX) + OFFSET_LEFT;
				int y2 = luckOffset - (int) ((playerData[j]*factorY));
				g2d.drawOval(x2 - LINE_PT_SIZE / 2, y2 - LINE_PT_SIZE / 2, LINE_PT_SIZE, LINE_PT_SIZE);
				g2d.drawLine(x1, y1, x2, y2);
			}
		}
	}
	
	private void drawBarGraph()
	{
		Bar[] bars = new Bar[players.length];
		for (int i = 0; i < players.length; i++)
			bars[i] = new Bar(players[i], players[i].getCurrentStats(currentStats));
		sortBars(bars);
		
		if (currentStats == Player.LUCK_FACTOR)
		{
			drawLuckBarGraph(bars);
			return;
		}
		
		int width = canvas.getWidth();
		int max = bars[0].value;
		for (int i = 0; i < bars.length; i++)
		{
			Bar bar = bars[i];
			g2d.setColor(bar.color);
			
			int barLength = (int) ((width - 60) * ((double) bar.value) / max);
			g2d.fillRect(OFFSET_LEFT, OFFSET_TOP + BAR_SPACE * i, barLength, BAR_HEIGHT);
			
			g2d.setColor(Color.BLACK);
			g2d.drawString(bar.name, OFFSET_LEFT + TEXT_INDENT, TEXT_TOP_OFFSET + BAR_SPACE * i);
			g2d.drawString(Integer.toString(bar.value), VALUE_INDENT, TEXT_TOP_OFFSET + BAR_SPACE * i);
		}
	}
	
	private void drawLuckBarGraph(Bar[] bars)
	{
		int width = canvas.getWidth();
		int max = Math.max(bars[0].value, Math.abs(bars[bars.length - 1].value));
		for (int i = 0; i < bars.length; i++)
		{
			Bar bar = bars[i];
			g2d.setColor(bar.color);
			
			int barLength = (int) (((width - OFFSET_RIGHT - OFFSET_LEFT) / 2) * ((double) bar.value) / max);
			if (barLength >= 0)
				g2d.fillRect(width / 2, OFFSET_TOP + BAR_SPACE * i, barLength, BAR_HEIGHT);
			else
				g2d.fillRect(width / 2 + barLength, OFFSET_TOP + BAR_SPACE * i, -barLength, BAR_HEIGHT);
			
			g2d.setColor(Color.BLACK);
			g2d.drawString(bar.name + " (" + bar.value + ")", width / 2 + TEXT_INDENT, TEXT_TOP_OFFSET + BAR_SPACE * i);
		}
	}
	
	private void drawGraph()
	{
		if (g2d == null)
		{
			g2d = (Graphics2D) canvas.getGraphics();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setFont(GRAPH_FONT);
		}
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		
		if (lineGraph)
			drawLineGraph();
		else
			drawBarGraph();
	}
	
	private JButton createStatButton(String s, final int n)
	{
		final JButton btn = new JButton(s);
		btn.setFont(STATS_BUTTON_FONT);
		if (n == currentStats)
		{
			btn.setBackground(Color.YELLOW);
			currentStatBtn = btn;
		}
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				currentStatBtn.setBackground(null);
				currentStatBtn = btn;
				currentStats = n;
				btn.setBackground(Color.YELLOW);
				canvas.requestFocus();
			}
		});
		return btn;
	}
	
	private void sortBars(Bar[] bars)
	{
		for (int i = 1; i < bars.length; i++)
		{
			for (int j = i - 1; j >= 0 && bars[j + 1].value > bars[j].value; j--)
			{
				Bar temp = bars[j];
				bars[j] = bars[i];
				bars[i] = temp;
			}
		}
	}
	
	private class Bar
	{
		public Color color;
		public int value;
		public String name;
		
		public Bar(Player player, int val)
		{
			name = player.name;
			color = player.color;
			value = val;
		}
	}
}
