package risk;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class RiskGame extends JFrame implements MouseListener
{	
	private static final String VERSION = "5.0.0";
	
	private static final int MAX_NUM_PLAYERS = 21;
	private static final int MIN_NUM_PLAYERS = 2;
	
	public static final Color[] PLAYER_COLOURS = {new Color(238,44,44), new Color(28,134,238), new Color(50,205,50), new Color(238,238,0),
			new Color(255,0,255), Color.LIGHT_GRAY, new Color(255,140,0), new Color(160,32,240), new Color(72,209,204), new Color(219,219,112),
			new Color(185,211,238),new Color(255,218,185),new Color(154,192,205),new Color(120,134,107),new Color(238,180,180),new Color(124,252,0),
			new Color(154,205,50),new Color(209,146,117),new Color(205,96,144),new Color(255,99,71),new Color(191,62,255),new Color(219,219,112)};
	
	private static final int[] CASH_IN = {4, 6, 8, 10, 12, 15};
	private static final int CASH_IN_INCREMENT = 5;
	private static final int INITIAL_PLACE_COUNT = 1;
	
	public static enum State {PLACE, DEPLOY, ATTACK, ADVANCE, FORTIFY};
	
	private static final String ACTION_END_ATTACKS = "End Attacks";
	
	public final static int REGULAR = 0;
	public final static int NONE = 1;
	public final static int MODIFIED = 2;
	
	private int numPlayers;
	private JTextField[] playerNames;
	private JCheckBox[] playerType;
	private JPanel namePnl;
	private ActionListener checkBoxListener;
	private HashMap<JCheckBox, JTextField> checkFieldHash = new HashMap<JCheckBox, JTextField>();
	
	private JPanel gamePnl;
	private JLabel actionLbl;
	public JButton actionBtn;
	private JComboBox troopAmountCombo;
	private JTextArea gameLog;
	private JButton autoPlayBtn;
	private JPanel sidePnl1;
	private JPanel sidePnl2;
	
	private File saveFile;
	private JFileChooser fileChooser = new JFileChooser();
	
	private Map map;
	
	public Player[] players;
	private HashMap<Player, PlayerPanel> playerPnlHash = new HashMap<Player, PlayerPanel>();
	private Player activePlayer;
	private Player eliminatedPlayer;
	public boolean conqueredTerritory;
	
	private int index;
	private int round;
	public State state = State.PLACE;
	private int placeNum = INITIAL_PLACE_COUNT;
	public int deployNum;
	private int firstPlayerIndex;
	
	public ArrayList<Card> deck = new ArrayList<Card>();
	private int cashes = 0;
	private JLabel cashInLbl;
	
	private Territory fromTerrit;
	private Territory toTerrit;
	
	private boolean useEpicMap;
	public int cardType = REGULAR;
	
	public RiskCalculator riskCalc = new RiskCalculator(false);
	
	private boolean shiftKeyDown;
	private boolean ctrlKeyDown;
	
	public boolean gameOver;
	
	public boolean autoGame;
	public boolean simulate;
	public int simulateCount;
	public boolean paused;
	
	private JPanel homeScreen;
	private JPanel gameScreen;
	private StatsScreen statsScreen;
	
	public RiskGame()
	{
		super("Risk " + VERSION + " - By Jeff Sullivan");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		populateHomeScreen();
		pushScreen(homeScreen);
		selectPlayerCount(2);
		
		setVisible(true);
	}
	
	private void pushScreen(JPanel screen)
	{
		if (screen.getParent() != this)
		{
			getContentPane().removeAll();
			add(screen);
			pack();
		}
	}
	
	private void populateHomeScreen()
	{	
		homeScreen = new JPanel();
		homeScreen.setLayout(new BoxLayout(homeScreen, BoxLayout.Y_AXIS));
		
		JLabel titleLbl = new JLabel("Risk - Conquer the World!");
		titleLbl.setAlignmentX(CENTER_ALIGNMENT);
		titleLbl.setFont(new Font("Helvetica", Font.BOLD, 16));
		homeScreen.add(titleLbl);
				
		String[] numPlayersOption = new String[MAX_NUM_PLAYERS - MIN_NUM_PLAYERS + 1];
		for (int i = MIN_NUM_PLAYERS; i <= MAX_NUM_PLAYERS; i++)
			numPlayersOption[i - MIN_NUM_PLAYERS] = Integer.toString(i);
		final JComboBox numPlayersCombo = new JComboBox(numPlayersOption);
		numPlayersCombo.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent event)
			{
				selectPlayerCount(numPlayersCombo.getSelectedIndex() + MIN_NUM_PLAYERS);
			}
		});
		homeScreen.add(new JLabel("Number of Players:"));
		homeScreen.add(numPlayersCombo);
		
		namePnl = new JPanel();
		namePnl.setLayout(new BoxLayout(namePnl, BoxLayout.Y_AXIS));
		JScrollPane scrollPane = new JScrollPane(namePnl);
		homeScreen.add(scrollPane);
		
		final JComboBox mapCombo = new JComboBox(new String[] {"Epic Map", "Classic Map"});
		homeScreen.add(mapCombo);
		final JComboBox cardCombo = new JComboBox(new String[] {"Regular Cards", "No Cards", "Modified Cards"});
		homeScreen.add(cardCombo);
		
		JPanel btnPnl = new JPanel();
		
		JButton startBtn = new JButton("Start Game");
		startBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				numPlayers = playerNames.length;
				useEpicMap = mapCombo.getSelectedIndex() == 0;
				cardType = cardCombo.getSelectedIndex();
				initializeGame();
			}
		});
		btnPnl.add(startBtn);
		
		JButton loadBtn = new JButton("Load Game");
		loadBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					if (fileChooser.showOpenDialog(RiskGame.this) == JFileChooser.APPROVE_OPTION)
						loadGame(fileChooser.getSelectedFile());
				}
				catch (Exception ex) { error("Failed to load saved game."); }
			}
		});
		btnPnl.add(loadBtn);
		
		homeScreen.add(btnPnl);
	}
	
	private void initializeGame()
	{
		players = new Player[numPlayers];
		firstPlayerIndex = (int) (numPlayers * Math.random());
		index = firstPlayerIndex;
		playerPnlHash.clear();
		
		initGUI();
		pushScreen(gameScreen);
		
		map = new Map(this, useEpicMap);
		
		int half = (int) Math.ceil(numPlayers / 2.0);
		for (int i = 0; i < numPlayers; i++)
		{
			if (!playerType[i].isSelected())
				players[i] = new Player(i + 1, playerNames[i].getText(), PLAYER_COLOURS[i]);
			else //if (i == 0)
				players[i] = new Einstein(i + 1, PLAYER_COLOURS[i], this);
			//else
			//	players[i] = new AIPlayer(i + 1, PLAYER_COLOURS[i], this);
			
			PlayerPanel playerPnl = new PlayerPanel(players[i]);
			if (i < half)
				sidePnl1.add(playerPnl);
			else
				sidePnl2.add(playerPnl);
		}
		
		activePlayer = players[index];
		playerPnlHash.get(activePlayer).setActive(true);
		message(activePlayer.name + ", you have " + placeNum + " armies left to place.");
		
		ArrayList<Territory> territories = map.getTerritories();
		int terrCount = territories.size();
		deck.removeAll(deck);
		for (int i = 0; i < terrCount; i++)
		{
			Territory territ = territories.remove((int) (Math.random() * territories.size()));
			territ.setOwner(players[i % numPlayers]);
			deck.add(new Card(territ, i % 3));
			gamePnl.add(territ.getButton());
		}
		
		for (Player player : players)
			playerPnlHash.get(player).update();
		log("Game Initialized.");
		
		ImageIcon img = (useEpicMap) ? new ImageIcon(getClass().getResource("Epic.jpg")) : new ImageIcon(getClass().getResource("Regular.jpg"));
		JLabel imgLbl = new JLabel(img);
		gamePnl.add(imgLbl);
		imgLbl.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
		
		saveGame();
		beginPlacement();
	}
	
	private void loadGame(File file) throws IOException
	{	
		state = State.DEPLOY;
		
		saveFile = file;
		BufferedReader reader = new BufferedReader(new FileReader(saveFile));
		useEpicMap = reader.readLine().equals("true");
		cardType = Integer.parseInt(reader.readLine());
		
		numPlayers = Integer.parseInt(reader.readLine());
		players = new Player[numPlayers];
		playerPnlHash.clear();
		
		initGUI();
		pushScreen(gameScreen);
		
		map = new Map(this, useEpicMap);
		
		int half = (int) Math.ceil(numPlayers / 2.0);
		for (int i = 0; i < numPlayers; i++)
		{
			String name = reader.readLine();
			boolean living = true;
			if (reader.readLine().equals("eliminated"))
				living = false;
			
			if (reader.readLine().equals("AI"))
				players[i] = new AIPlayer(i + 1, PLAYER_COLOURS[i], this);
			else
				players[i] = new Player(i + 1, name, PLAYER_COLOURS[i]);
			
			PlayerPanel playerPnl = new PlayerPanel(players[i]);
			players[i].setLiving(living);
			if (i < half)
				sidePnl1.add(playerPnl);
			else
				sidePnl2.add(playerPnl);
			
			int cardCount = Integer.parseInt(reader.readLine());
			for (int j = 0; j < cardCount; j++)
			{
				String[] vals = reader.readLine().split("/");
				players[i].giveCard(new Card(map.getTerritory(vals[0]), Integer.parseInt(vals[1])));
			}
		}
		
		index = Integer.parseInt(reader.readLine()) - 1;
		firstPlayerIndex = index + 1;
		activePlayer = players[index + 1];
		playerPnlHash.get(activePlayer).setActive(true);
		message("Loading game...");
		
		if (cardType == REGULAR)
		{
			cashes = Integer.parseInt(reader.readLine());
			int nextCashIn = (cashes >= CASH_IN.length - 1) ? CASH_IN[CASH_IN.length - 1] + CASH_IN_INCREMENT * (cashes - CASH_IN.length + 1)
					: CASH_IN[cashes];
			cashInLbl.setText(Integer.toString(nextCashIn));
		}
		else if (cardType == MODIFIED)
		{
			for (int i = 0; i < numPlayers; i++)
				players[i].cashes = Integer.parseInt(reader.readLine());
		}
				
		String line;
		int i = 0;
		deck.removeAll(deck);
		while ((line = reader.readLine()) != null)
		{
			String[] vals = line.split("/");
			Territory t = map.getTerritory(vals[0]);
			t.setOwner(players[Integer.parseInt(vals[1]) - 1]);
			t.setUnits(Integer.parseInt(vals[2]));
			deck.add(new Card(t, i % 3));
			gamePnl.add(t.getButton());
			i++;
		}
		
		for (Player player : players)
			playerPnlHash.get(player).update();
		log("Game Loaded.");
		
		ImageIcon img = (useEpicMap) ? new ImageIcon(getClass().getResource("Epic.jpg")) : new ImageIcon(getClass().getResource("Regular.jpg"));
		JLabel imgLbl = new JLabel(img);
		gamePnl.add(imgLbl);
		imgLbl.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
				
		beginPlacement();
	}
	
	private void initGUI()
	{
		gameScreen = new JPanel(new BorderLayout());
		
		final JTabbedPane tabs = new JTabbedPane();
		addFocusListener(new FocusAdapter()
		{
			public void focusGained(FocusEvent event)
			{
				if (tabs.getSelectedIndex() != 1)
					tabs.requestFocus();
			}
		});
		
		tabs.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent event) 
			{
				if (tabs.getSelectedIndex() == 1)
				{
					updateStats();
					statsScreen.requestFocus();
				}
				else
					tabs.requestFocus();
			}
		});
		
		tabs.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent event)
			{
				if (event.getKeyCode() == KeyEvent.VK_SHIFT)
					shiftKeyDown = true;
				else if (event.getKeyCode() == KeyEvent.VK_CONTROL)
					ctrlKeyDown = true;
			}
			
			public void keyReleased(KeyEvent event)
			{
				if (event.getKeyCode() == KeyEvent.VK_SHIFT)
					shiftKeyDown = false;
				else if (event.getKeyCode() == KeyEvent.VK_CONTROL)
					ctrlKeyDown = false;
			}
		});
		
		JPanel playPnl = new JPanel();
		playPnl.setLayout(new BorderLayout());
		tabs.addTab("Game", playPnl);
		
		gamePnl = new JPanel();
		playPnl.add(gamePnl, BorderLayout.CENTER);
		if (useEpicMap)
			gamePnl.setPreferredSize(new Dimension(900, 618));
		else
			tabs.setPreferredSize(new Dimension(794, 618));
		gamePnl.setLayout(null);
		gameScreen.add(tabs, BorderLayout.CENTER);
		
		actionLbl = new JLabel();
		actionLbl.setAlignmentX(CENTER_ALIGNMENT);
		actionLbl.setFont(new Font("Helvetica", Font.BOLD, 16));
		
		JPanel optionPnl = new JPanel();
		optionPnl.setLayout(new BoxLayout(optionPnl, BoxLayout.Y_AXIS));
		if (actionBtn == null)
		{
			actionBtn = new JButton(ACTION_END_ATTACKS);
			actionBtn.setAlignmentX(CENTER_ALIGNMENT);
			actionBtn.setFocusable(false);
			actionBtn.setEnabled(false);
			actionBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event)
				{
					switch (state)
					{
						case ATTACK:
							endAttacks();
							break;
						case ADVANCE:
							endAdvance();
							break;
						case FORTIFY:
							endFortifications();
							break;
					}
				}
			});
		}
		
		JPanel actionPnl = new JPanel();
		if (troopAmountCombo == null)
		{
			troopAmountCombo = new JComboBox(new String[] {"1", "2", "3", "5", "10", "All"});
			troopAmountCombo.setFocusable(false);
		}
		actionPnl.add(actionBtn);
		actionPnl.add(new JLabel("Troops/click:"));
		actionPnl.add(troopAmountCombo);
		
		gameLog = new JTextArea(6, 5);
		gameLog.setEditable(false);
		JScrollPane sp = new JScrollPane(gameLog);
		sp.setPreferredSize(new Dimension(400, 300));
		JButton logBtn = new JButton("Log");
		logBtn.setFocusable(false);
		final JFrame logWin = new JFrame("Game Log");
		logWin.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 300, Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 125);
		logWin.add(sp);
		logWin.pack();
		logBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				logWin.setVisible(true);
			}
		});
		actionPnl.add(logBtn);
		
		autoPlayBtn = new JButton("Autoplay");
		autoPlayBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				
			}
		});
		actionPnl.add(autoPlayBtn);
		
		if (cardType == REGULAR)
		{
			cashInLbl = new JLabel(Integer.toString(CASH_IN[0]));
			actionPnl.add(new JLabel("Next Cash-In:"));
			actionPnl.add(cashInLbl);
			actionPnl.add(new JLabel("troops"));
		}
		optionPnl.add(actionPnl);
		
		statsScreen = new StatsScreen(players);
		tabs.addTab("Statistics", statsScreen);
		
		optionPnl.add(actionLbl);
		playPnl.add(optionPnl, BorderLayout.PAGE_END);
		
		sidePnl1 = new JPanel();
		sidePnl2 = new JPanel();
		JScrollPane sp1 = new JScrollPane(sidePnl1);
		JScrollPane sp2 = new JScrollPane(sidePnl2);
		int rows = (int) Math.ceil(numPlayers / 2.0);
		sidePnl1.setLayout(new GridLayout(rows, 1));
		sidePnl2.setLayout(new GridLayout(rows, 1));
		Dimension dim = (useEpicMap) ? new Dimension(120, 640) : new Dimension(120, 500);
		sp1.setPreferredSize(dim);
		sp2.setPreferredSize(dim);
		gameScreen.add(sp1, BorderLayout.LINE_START);
		gameScreen.add(sp2, BorderLayout.LINE_END);
	}
	
	private void handleTurn()
	{
		if (activePlayer.isAI())
			handleAITurn();
		else
			beginTurn();
	}
	
	private void handleAITurn()
	{
		while (activePlayer.isAI())
		{
			AIPlayer aiPlayer = (AIPlayer) activePlayer;
			beginTurn();
			aiPlayer.deploy();
			aiPlayer.attack();
			aiPlayer.fortify();
			
			playerPnlHash.get(activePlayer).update();
			gamePnl.paintImmediately(0, 0, gamePnl.getWidth(), gamePnl.getHeight());
			sidePnl1.paintImmediately(0, 0, sidePnl1.getWidth(), sidePnl1.getHeight());
			sidePnl2.paintImmediately(0, 0, sidePnl2.getWidth(), sidePnl2.getHeight());
		}
	}
	
	private void incrementTurn()
	{
		index++;
		for (int i = index;; i++)
		{
			index = i % numPlayers;
			if (players[index].isLiving())
				break;
		}
			
		if (index == firstPlayerIndex)
		{
			updateRound();
			round++;
		}
			
		playerPnlHash.get(activePlayer).setActive(false);
		activePlayer = players[index];
		
		if (saveFile != null)
			saveGame();
		
		if (!activePlayer.isAI())
			handleTurn();
	}
	
	public void beginTurn()
	{
		if (!autoGame)
		{
			autoGame = true;
			for (int i = 0; i < numPlayers; i++)
			{
				if (!players[i].isAI() && players[i].isLiving())
					autoGame = false;
			}
		}
		
		playerPnlHash.get(activePlayer).setActive(true);
		
		int troopCount = Math.max(map.getTerritoryCount(activePlayer) / 3, 3);
		log(activePlayer.name + " gets " + troopCount + " troops for " + map.getTerritoryCount(activePlayer) + " territories.");
		
		int extra = 0;
		if (activePlayer.hasSet())
			extra += playSet();
		troopCount += extra;
		
		state = State.DEPLOY;
		
		conqueredTerritory = false;
		fromTerrit = null;
		toTerrit = null;
		
		actionBtn.setText("End Attacks");
		actionBtn.setEnabled(false);
		autoPlayBtn.setEnabled(true);
		
		Continent[] conts = map.getContinents();
		for (Continent cont : conts)
		{
			if (cont.hasContinent(activePlayer))
			{
				troopCount += cont.getBonus();
				log(activePlayer.name + " gets " + cont.getBonus() + " troops for holding " + cont.name + ".");
			}
		}
		activePlayer.updateStats(Player.BONUS, troopCount - extra);
		message(activePlayer.name + ", you have " + troopCount + " troops to place.");
		deployNum = troopCount;
	}
	
	public int playSet()
	{
		Card[][] sets = activePlayer.getSets();
		if (sets.length == 0)
			return 0;
		
		String[] setTexts = new String[sets.length];
		Arrays.fill(setTexts, "");
		for (int i = 0; i < sets.length; i++)
		{
			for (int j = 0; j < 3; j++)
			{
				if (j > 0)
					setTexts[i] += ", ";
				if (sets[i][j].territory.owner == activePlayer)
					setTexts[i] += "[ " + sets[i][j].territory.name + " ]";
				else
					setTexts[i] += sets[i][j].territory.name;
			}
		}
		
		int extra = 0;
		if (cardType == REGULAR)
			extra = (cashes >= CASH_IN.length) ? 5 * cashes - 10 : CASH_IN[cashes];
		else if (cardType == MODIFIED)
			extra = (activePlayer.cashes >= CASH_IN.length) ? 5 * activePlayer.cashes - 10 : CASH_IN[activePlayer.cashes];
		
		String setChosen = null;
		if (!activePlayer.isAI())
		{
			if (activePlayer.getCardCount() >= 5)
			{
				while (setChosen == null)
					setChosen = (String) JOptionPane.showInputDialog(this, "You have a set to play for " + extra + " extra troops. Choose a set:", "Play a Set", JOptionPane.QUESTION_MESSAGE, null, setTexts, "");
			}
			else
				setChosen = (String) JOptionPane.showInputDialog(this, "You have a set to play for " + extra + " extra troops. Choose a set or cancel:", "Play a Set", JOptionPane.QUESTION_MESSAGE, null, setTexts, "");
		}
		else
			setChosen = setTexts[0];
		
		if (setChosen != null)
		{
			if (cardType == REGULAR)
				cashes++;
			else
				activePlayer.cashes++;
			
			Card[] setPlayed = null;
			for (int i = 0; i < setTexts.length; i++)
			{
				if (setTexts[i].equals(setChosen))
				{
					setPlayed = sets[i];
					break;
				}
			}
			if (setPlayed == null)
				return 0;
			
			activePlayer.playSet(setPlayed);
			
			for (int i = 0; i < 3; i++)
			{
				if (setPlayed[i].territory.owner == activePlayer)
				{
					setPlayed[i].territory.addUnits(2);
					activePlayer.updateStats(Player.TROOPS_DEPLOYED, 2);
				}
			}
			
			if (activePlayer.isAI())
				((AIPlayer) activePlayer).message(activePlayer.name + " played a set worth " + extra + " troops.");
			log(activePlayer.name + " played a set worth " + extra + " troops.");
			
			if (cardType == REGULAR)
			{
				int nextCashIn = (cashes >= CASH_IN.length) ? extra + 5 : CASH_IN[cashes];
				cashInLbl.setText(Integer.toString(nextCashIn));
			}
			
			playerPnlHash.get(activePlayer).update();
			if (activePlayer.hasSet())
				extra += playSet();
		}
		else
			return 0;
		
		return extra;
	}
	
	private void beginPlacement()
	{
		state = State.PLACE;
		if (activePlayer.isAI())
			handleAIPlacement();
	}
	
	private void handleAIPlacement()
	{
		while (activePlayer.isAI())
		{
			((AIPlayer) activePlayer).place();
		}
	}
	
	private void incrementPlacementTurn()
	{
		playerPnlHash.get(activePlayer).setActive(false);
		index++;
		activePlayer = players[index % numPlayers];
		playerPnlHash.get(activePlayer).setActive(true);
		if (index % numPlayers == firstPlayerIndex)
			placeNum--;
		if (placeNum > 0)
			message(activePlayer.name + ", you have " + placeNum + " armies left to deploy.");
		else
		{
			updateRound();
			handleTurn();
		}
	}
	
	public void place(Territory territ)
	{
		if (territ.owner != activePlayer)
			return;
		territ.addUnits(1);
		playerPnlHash.get(activePlayer).update();
		log(activePlayer.name + " placed 1 troop on " + territ.name + ".");
		incrementPlacementTurn();
	}
	
	public void deploy(Territory territ, boolean all)
	{
		if (territ.owner != activePlayer)
			return;
		String placing = troopAmountCombo.getSelectedItem().toString();
		int toPlace = (all || placing.equals("All")) ? deployNum : Math.min(Integer.parseInt(placing), deployNum);
		territ.addUnits(toPlace);
		deployNum -= toPlace;
		activePlayer.updateStats(Player.TROOPS_DEPLOYED, toPlace);
		log(activePlayer.name + " placed " + toPlace + " troops on " + territ.name + ".");
		
		if (deployNum == 0)
			enterAttackState();
		else
			message(activePlayer.name + ", you have " + deployNum + " troops to place.");
		playerPnlHash.get(activePlayer).update();
	}
	
	private void enterAttackState()
	{
		state = State.ATTACK;
		actionBtn.setEnabled(true);
		autoPlayBtn.setEnabled(false);
		message("Attack - click from your territory to an adjacent one to attack.");
	}
	
	public void attack(Territory from, Territory to, boolean all)
	{
		fromTerrit = from;
		attack(to, all);
	}
	
	public void fortify(Territory from, Territory to, boolean all)
	{
		fromTerrit = from;
		fortify(to, all);
	}
	
	public void endAdvance()
	{
		if (state != State.ADVANCE)
			return;
		
		state = State.ATTACK;
		actionBtn.setText("End Attacks");
		if (toTerrit.units != 1)
		{
			fromTerrit.unhilite();
			fromTerrit = toTerrit;
		}
		else
			toTerrit.unhilite();
		toTerrit = null;
		
		message("Attack - click from your territory to an adjacent one to attack.");
		
		if (eliminatedPlayer != null)
		{
			log(activePlayer.name + " eliminated " + eliminatedPlayer.name + " from the game.");
			if (eliminatedPlayer.number - 1 == firstPlayerIndex)
			{
				int i = firstPlayerIndex;
				while (true)
				{
					i++;
					if (players[i % numPlayers].isLiving())
					{
						firstPlayerIndex = i % numPlayers;
						break;
					}
				}
			}
			
			int count = 0;
			for (int i = 0; i < numPlayers; i++)
			{
				if (players[i].isLiving())
					count++;
			}
			
			if (eliminatedPlayer.isAI())
				((AIPlayer) eliminatedPlayer).message("Argh!", RiskGame.this);
			
			if (count <= 1)
			{
				activePlayer.setStats(Player.TERRITORIES, map.getTerritoryCount(activePlayer));
				activePlayer.setStats(Player.TROOPS, map.getTroopCount(activePlayer));
				JOptionPane.showMessageDialog(null, activePlayer.name + " won the game!", "Congratulations!", JOptionPane.INFORMATION_MESSAGE);
				gameOver = true;
				return;
			}
			activePlayer.giveCards(eliminatedPlayer.takeAllCards());
			playerPnlHash.get(activePlayer).update();
			playerPnlHash.get(eliminatedPlayer).update();
			eliminatedPlayer = null;
			
			if (activePlayer.getCardCount() >= 5)
			{
				deployNum = playSet();
				state = State.DEPLOY;
				actionBtn.setEnabled(false);
				message(activePlayer.name + " you have " + deployNum + " troops to place from your cash-in.");
			}
		}
	}
	
	public void endAttacks()
	{
		if (state != State.ATTACK)
			return;
		
		state = State.FORTIFY;
		actionBtn.setText("End Fortification");
		if (fromTerrit != null)
			fromTerrit.unhilite();
		fromTerrit = null;
		message("Fortify - click from one territory to another to fortify troops.");
	}
	
	public void endFortifications()
	{
		if (fromTerrit != null)
			fromTerrit.unhilite();
		if (toTerrit != null)
			toTerrit.unhilite();
		endTurn();
	}
	
	public void attack(Territory territ, boolean all)
	{
		if (territ.owner == activePlayer)
		{
			if (fromTerrit != null)
				fromTerrit.unhilite();
			
			if (territ.units == 1)
			{
				error("You cannot attack with 1 troop!");
				return;
			}
			
			fromTerrit = territ;
			fromTerrit.hilite();
			message("Attack from " + fromTerrit.name + " to an adjacent territory.");
		}
		else if (fromTerrit != null)
		{
			if (fromTerrit.units == 1)
			{
				error("You cannot attack with 1 troop!");
				return;
			}
			
			if (!fromTerrit.isConnecting(territ))
			{
				error(fromTerrit.name + " does not connect with " + territ.name + "!");
				return;
			}
			
			toTerrit = territ;
			Player otherPlayer = toTerrit.owner;
			
			int[] outcome = getOutcome(fromTerrit, toTerrit);
			fromTerrit.addUnits(outcome[0]);
			toTerrit.addUnits(outcome[1]);
			activePlayer.updateStats(Player.TROOPS_KILLED, -outcome[1]);
			activePlayer.updateStats(Player.TROOPS_LOST, -outcome[0]);
			otherPlayer.updateStats(Player.TROOPS_KILLED, -outcome[0]);
			otherPlayer.updateStats(Player.TROOPS_LOST, -outcome[1]);
			
			if (toTerrit.units == 0)
			{
				if (map.getTerritoryCount(toTerrit.owner) == 1)
				{
					eliminatedPlayer = otherPlayer;
					eliminatedPlayer.setLiving(false);
				}
				
				toTerrit.setOwner(activePlayer);
				toTerrit.addUnits(1);
				fromTerrit.addUnits(-1);
				conqueredTerritory = true;
				
				if (fromTerrit.units > 1)
				{
					state = State.ADVANCE;
					message(actionLbl.getText() + " - Click to advance your armies.");
					actionBtn.setText("Advance Troops");
					fromTerrit.hilite();
					toTerrit.hilite();
				}
				else
				{
					state = State.ADVANCE;
					endAdvance();
					fromTerrit.unhilite();
				}
				
				log(activePlayer.name + " conquered " + territ.name + " from " + territ.owner.name + ".");
				activePlayer.updateStats(Player.TERRITORIES_CONQUERED, 1);
				otherPlayer.updateStats(Player.TERRITORIES_LOST, 1);
			}
			else if (all && fromTerrit.units > 3)
				attack(toTerrit, true);
			else
				toTerrit = null;
			
			playerPnlHash.get(activePlayer).update();
			playerPnlHash.get(otherPlayer).update();
		}
	}
	
	public int[] getOutcome(Territory from, Territory to)
	{
		int attackers = from.units;
		int defenders = to.units;
		
		int[] attackDice;
		int[] defendDice;
		
		if (attackers >= 4)
			attackDice = new int[] {rollDice(), rollDice(), rollDice()};
		else if (attackers == 3)
			attackDice = new int[] {rollDice(), rollDice()};
		else
			attackDice = new int[] {rollDice()};
		
		if (defenders >= 2)
			defendDice = new int[] {rollDice(), rollDice()};
		else
			defendDice = new int[] {rollDice()};
		
		int attackMax = 0;
		int attackMed = 0;
		boolean first = true;
		String dice = "";
		for (int i = 0; i < attackDice.length; i++)
		{
			if (attackDice[i] > attackMax)
			{
				attackMed = attackMax;
				attackMax = attackDice[i];
			}
			else if (attackDice[i] > attackMed)
				attackMed = attackDice[i];
			if (first)
				first = false;
			else
				dice += ",";
			dice += attackDice[i];
		}
		
		int defendMax = 0;
		int defendLow = 0;
		first = true;
		dice += " vs. ";
		for (int i = 0; i < defendDice.length; i++)
		{
			if (defendDice[i] > defendMax)
			{
				defendLow = defendMax;
				defendMax = defendDice[i];
			}
			else
				defendLow = defendDice[i];
			if (first)
				first = false;
			else
				dice += ",";
			dice += defendDice[i];
		}
		message(dice);
		
		int[] outcome = {0, 0};
		if (attackMax > defendMax)
			outcome[1]--;
		else
			outcome[0]--;
		
		if (attackMed > defendLow && attackMed != 0 && defendLow != 0)
			outcome[1]--;
		else if (defendLow >= attackMed && attackMed != 0 && defendLow != 0)
			outcome[0]--;
		
		if (attackDice.length == 3 && defendDice.length == 2)
		{
			if (outcome[0] == 0 && outcome[1] == -2)
				activePlayer.updateDiceStats(Player.W3V2);
			else if (outcome[0] == -1 && outcome[1] == -1)
				activePlayer.updateDiceStats(Player.T3V2);
			else
				activePlayer.updateDiceStats(Player.L3V2);
		}
		else if (attackDice.length == 2 && defendDice.length == 2)
		{
			if (outcome[0] == 0 && outcome[1] == -2)
				activePlayer.updateDiceStats(Player.W2V2);
			else if (outcome[0] == -1 && outcome[1] == -1)
				activePlayer.updateDiceStats(Player.T2V2);
			else
				activePlayer.updateDiceStats(Player.L2V2);
		}
		else if (attackDice.length == 3 && defendDice.length == 1)
		{
			if (outcome[0] == 0 && outcome[1] == -1)
				activePlayer.updateDiceStats(Player.W3V1);
			else
				activePlayer.updateDiceStats(Player.L3V1);
		}
		else if (attackDice.length == 2 && defendDice.length == 1)
		{
			if (outcome[0] == 0 && outcome[1] == -1)
				activePlayer.updateDiceStats(Player.W2V1);
			else
				activePlayer.updateDiceStats(Player.L2V1);
		}
		else if (attackDice.length == 1 && defendDice.length == 1)
		{
			if (outcome[0] == 0 && outcome[1] == -1)
				activePlayer.updateDiceStats(Player.W1V1);
			else
				activePlayer.updateDiceStats(Player.L1V1);
		}
		else if (attackDice.length == 1 && defendDice.length == 2)
		{
			if (outcome[0] == 0 && outcome[1] == -1)
				activePlayer.updateDiceStats(Player.W1V2);
			else
				activePlayer.updateDiceStats(Player.L1V2);
		}
		
		return outcome;
	}
	
	private int getTroopTransferCount(boolean all, int max)
	{
		String amountString = troopAmountCombo.getSelectedItem().toString();
		return (all || amountString.equals("All")) ? max : Math.min(max, Integer.parseInt(amountString));
	}
	
	public void advance(Territory territ, boolean all)
	{
		if (territ == fromTerrit)
		{
			if (toTerrit.units > 1)
			{
				int toPlace = getTroopTransferCount(all, toTerrit.units - 1);
				toTerrit.addUnits(-toPlace);
				fromTerrit.addUnits(toPlace);
			}
			
			if (all)
				endAdvance();
		}
		else if (territ == toTerrit)
		{
			if (fromTerrit.units > 1)
			{
				int toPlace = getTroopTransferCount(all, fromTerrit.units - 1);
				fromTerrit.addUnits(-toPlace);
				toTerrit.addUnits(toPlace);
			}
			
			if (all)
				endAdvance();
		}
	}
	
	public void fortify(Territory territ, boolean all)
	{
		if (territ.owner != activePlayer)
			return;
		
		if (fromTerrit == null)
		{
			if (territ.units == 1)
				return;
			fromTerrit = territ;
			fromTerrit.hilite();
			message("Click to fortify from " + fromTerrit.name + ".");
		}
		else if (toTerrit == null)
		{
			if (!fromTerrit.isFortifyConnecting(territ))
			{
				error(fromTerrit.name + " does not connect with " + territ.name + "!");
				return;
			}
			
			toTerrit = territ;
			toTerrit.hilite();
			int toPlace = getTroopTransferCount(all, fromTerrit.units - 1);
			toTerrit.addUnits(toPlace);
			fromTerrit.addUnits(-toPlace);
			
			log(activePlayer.name + " fortified " + toTerrit.name + " with " + toPlace + " troops from " + fromTerrit.name + ".");
			
			if (all)
				endFortifications();
		}
		else if (territ == toTerrit)
		{
			if (fromTerrit.units == 1)
				return;
			
			int toPlace = getTroopTransferCount(all, fromTerrit.units - 1);
			toTerrit.addUnits(toPlace);
			fromTerrit.addUnits(-toPlace);
			
			log(activePlayer.name + " fortified " + territ.name + " with " + toPlace + " troops from " + fromTerrit.name + ".");
			
			if (all)
				endFortifications();
		}
		else if (territ == fromTerrit)
		{
			if (toTerrit.units == 1)
				return;
			
			int toPlace = getTroopTransferCount(all, toTerrit.units - 1);
			toTerrit.addUnits(-toPlace);
			fromTerrit.addUnits(toPlace);
			
			log(activePlayer.name + " fortified " + territ.name + " with " + toPlace + " troops from " + toTerrit.name + ".");
			
			if (all)
				endFortifications();
		}
	}
	
	private void calculate(Territory t)
	{
		if (fromTerrit == null && t.owner != activePlayer)
			return;
		if (fromTerrit == null || t.owner == activePlayer)
		{
			if (t.units == 1)
			{
				error("Cannot attack with only 1 troop!");
				return;
			}
			if (fromTerrit != null)
				fromTerrit.unhilite();
			fromTerrit = t;
			fromTerrit.hilite();
		}
		else
		{
			if (!fromTerrit.isConnecting(t))
			{
				error(fromTerrit.name + " does not connect with " + t.name + "!");
				return;
			}
			message(riskCalc.getResults(fromTerrit.units, t.units));
		}
	}
	
	public void endTurn()
	{
		if (cardType != NONE && conqueredTerritory)
		{
			activePlayer.giveCard(deck.get((int) (Math.random() * deck.size())));
			log(activePlayer.name + " gets a card.");
		}
		playerPnlHash.get(activePlayer).update();
		incrementTurn();
	}
	
	private void saveGame()
	{
		if (saveFile == null)
		{
			while (true)
			{
				if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
				{
					saveFile = fileChooser.getSelectedFile();
					break;
				}
			}
		}
		
		try
		{
			PrintWriter writer = new PrintWriter(new FileWriter(saveFile));
			writer.println(useEpicMap);
			writer.println(cardType);
			writer.println(numPlayers);
			for (int i = 0; i  < numPlayers; i++)
			{
				writer.println(players[i].name);
				if (players[i].isLiving())
					writer.println("alive");
				else
					writer.println("eliminated");
				if (players[i].isAI())
				{
					writer.println("AI");
					//writer.println(((AIPlayer)players[i]).AINum);
				}
				else
					writer.println("human");
				ArrayList<Card> cards = players[i].getCards();
				writer.println(players[i].getCardCount());
				for (int j = 0; j < cards.size(); j++)
					writer.println(cards.get(j).territory.name + "/" + cards.get(j).type);
			}
			writer.println(index);
			if (cardType != MODIFIED)
				writer.println(cashes);
			else
			{
				for (int i = 0; i < numPlayers; i++)
					writer.println(players[i].cashes);
			}
			ArrayList<Territory> territs = map.getTerritories();
			for (int i = 0; i < territs.size(); i++)
				writer.println(territs.get(i).name + "/" + territs.get(i).owner.number + "/" + territs.get(i).units);
			writer.close();
		}
		catch (Exception ex) { error("Error saving the game."); }
	}

	public void mousePressed(MouseEvent event) 
	{
		if (event.getSource() instanceof JButton)
		{
			JButton btn = (JButton) event.getSource();
			Territory territ = map.getTerritory(btn.getName());
			boolean all = (event.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK;
			switch (state)
			{
				case PLACE:
					place(territ);
					break;
				case DEPLOY:
					deploy(territ, all);
					break;
				case ATTACK:
					if (shiftKeyDown)
						calculate(territ);
					else
						attack(territ, all);
					break;
				case ADVANCE:
					advance(territ, all);
					break;
				case FORTIFY:
					fortify(territ, all);
					break;
			}
		}
	}
	
	private void updateStats()
	{
		for (Player player : players)
		{
			if (player.isLiving())
			{
				player.setStats(Player.TROOPS, map.getTroopCount(player));
				player.setStats(Player.TERRITORIES, map.getTerritoryCount(player));
				player.updateLuckStats();
			}
		}
	}
	
	private void updateRound()
	{
		updateStats();
		for (Player player : players)
		{
			if (player.isLiving())
				player.updateRound();
		}
	}
	
	public int getBonus(Player player)
	{
		Continent[] continents = map.getContinents();
		int total = 0;
		for (Continent cont : continents)
		{
			if (cont.hasContinent(player))
				total += cont.getBonus();
		}
		return Math.max((int) (map.getTerritoryCount(player) / 3), 3) + total;
	}
	
	private void selectPlayerCount(int numPlayers)
	{
		playerNames = new JTextField[numPlayers];
		playerType = new JCheckBox[numPlayers];
		namePnl.removeAll();
		if (checkBoxListener == null)
		{
			checkBoxListener = new ActionListener() 
			{
				public void actionPerformed(ActionEvent event)
				{
					if (event.getSource() instanceof JCheckBox)
					{
						JCheckBox checkBox = (JCheckBox) event.getSource();
						checkFieldHash.get(checkBox).setEnabled(!checkBox.isSelected());
					}
				}
			};
		}
		checkFieldHash.clear();
		for (int i = 0; i < numPlayers; i++)
		{
			JPanel pnl = new JPanel();
			playerNames[i] = new JTextField("Player " + (i + 1), 15);
			playerType[i] = new JCheckBox();
			playerType[i].addActionListener(checkBoxListener);
			checkFieldHash.put(playerType[i], playerNames[i]);
			pnl.add(playerNames[i]);
			pnl.add(new JLabel("AI:"));
			pnl.add(playerType[i]);
			namePnl.add(pnl);
		}
		pack();
		if (namePnl.getParent().getHeight() > 400)
			namePnl.getParent().setPreferredSize(new Dimension(225, 400));
		namePnl.revalidate();
	}
	
	public void resumeGame()
	{
		paused = false;
		actionBtn.setText("");
	}
	
	public void pauseGame()
	{
		paused = true;
	}
	
	public void simulate(int n)
	{
		simulate = true;
		simulateCount = n;
	}
	
	private int rollDice()
	{
		return (int) (Math.random() * 6) + 1;
	}
	
	public void message(String msg)
	{
		actionLbl.setText(msg);
	}
	
	public void log(String msg)
	{
		gameLog.append(msg + "\n");
	}
	
	public void error(String msg)
	{
		JOptionPane.showMessageDialog(this, msg, "Oops!", JOptionPane.ERROR_MESSAGE);
	}
	
	public Map getMap()
	{
		return map;
	}
	
	public void hiliteTerritories(Territory[] territories)
	{
		for (Territory territ : territories)
		{
			JButton btn = territ.getButton();
			if (btn.getBorder() == null)
				btn.setBorder(Territory.BORDER_HIGHLIGHT);
		}
	}
	
	public void unhiliteTerritories(Territory[] territories)
	{
		for (Territory territ : territories)
		{
			if (territ != fromTerrit && territ != toTerrit)
				territ.getButton().setBorder(null);
		}
	}

	public void mouseEntered(MouseEvent event)
	{
		if (ctrlKeyDown && event.getSource() instanceof JButton)
		{
			Territory[] connectors = map.getTerritory(((JButton) event.getSource()).getName()).getConnectors();
			hiliteTerritories(connectors);
		}
	}
	
	public void mouseExited(MouseEvent event)
	{
		if (event.getSource() instanceof JButton)
		{
			Territory[] connectors = map.getTerritory(((JButton) event.getSource()).getName()).getConnectors();
			unhiliteTerritories(connectors);
		}
	}
	
	public void mouseClicked(MouseEvent event) { }
	public void mouseReleased(MouseEvent event) { }
	
	private static final Font LABEL_FONT = new Font("Helvetica", Font.BOLD, 14);
	private static final Font NAME_FONT = new Font("Helvetica", Font.BOLD, 16);
	private static final Font SMALL_LABEL_FONT = new Font("Helvetica", Font.BOLD, 14);
	private static final Font SMALL_NAME_FONT = new Font("Helvetica", Font.BOLD, 16);
	
	private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(0, 5, 0, 0);
	private static final Border ACTIVE_BORDER = BorderFactory.createLineBorder(Color.BLACK, 3);
	
	private class PlayerPanel extends JPanel
	{
		private Player player;
				
		private JLabel nameLbl;
		private JLabel troopCount = new JLabel("0 troops");
		private JLabel terrCount = new JLabel("0 territories");
		private JLabel cardCount;
		private JLabel bonusCount = new JLabel("0 bonus");
		
		public PlayerPanel(Player p)
		{
			super();
			player = p;
			playerPnlHash.put(player, this);
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			addMouseListener(new MouseAdapter()
			{
				public void mouseEntered(MouseEvent event)
				{
					hiliteTerritories(map.getTerritories(player));
				}
				
				public void mouseExited(MouseEvent event)
				{
					unhiliteTerritories(map.getTerritories(player));
				}
			});
			
			setBackground(player.color);
			
			nameLbl = new JLabel(player.name);
			nameLbl.setBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0));
			nameLbl.setForeground(Color.BLACK);
			
			troopCount.setForeground(Color.BLACK);
			troopCount.setBorder(EMPTY_BORDER);
			terrCount.setForeground(Color.BLACK);
			terrCount.setBorder(EMPTY_BORDER);
			bonusCount.setForeground(Color.BLACK);
			bonusCount.setBorder(EMPTY_BORDER);
			
			if (cardType != RiskGame.NONE)
			{
				cardCount = new JLabel("0 cards");
				cardCount.setForeground(Color.BLACK);
				cardCount.setBorder(EMPTY_BORDER);
				if (numPlayers <= 10)
					cardCount.setFont(LABEL_FONT);
				else
					cardCount.setFont(SMALL_LABEL_FONT);
			}
			
			if (numPlayers <= 10)
			{
				nameLbl.setFont(NAME_FONT);
				troopCount.setFont(LABEL_FONT);
				terrCount.setFont(LABEL_FONT);
				bonusCount.setFont(LABEL_FONT);
			}
			else
			{
				nameLbl.setFont(SMALL_NAME_FONT);
				troopCount.setFont(SMALL_LABEL_FONT);
				terrCount.setFont(SMALL_LABEL_FONT);
				bonusCount.setFont(SMALL_LABEL_FONT);
			}
			
			setPreferredSize(new Dimension(100, Math.max((int) (500.0 / Math.ceil(numPlayers / 2.0)), 85)));
			
			add(nameLbl);
			add(troopCount);
			add(terrCount);
			add(bonusCount);
			if (cardType != RiskGame.NONE)
				add(cardCount);
		}
		
		public void update()
		{
			if (player.isLiving())
			{
				troopCount.setText(map.getTroopCount(player) + " troops");
				int territs = map.getTerritoryCount(player);
				terrCount.setText(territs + " territories");
				
				if (cardType != RiskGame.NONE)
					cardCount.setText(player.getCardCount() + " cards");
				
				int bonus = getBonus(player);
				bonusCount.setText(bonus + " bonus");
			}
			else
			{
				bonusCount.setText("0 bonus");
				setBackground(Color.BLACK);
				nameLbl.setForeground(Color.LIGHT_GRAY);
			}
		}
		
		public void setActive(boolean active)
		{
			if (active)
				setBorder(ACTIVE_BORDER);
			else
				setBorder(null);
		}
	}
	
	public static void main(String[] args)
	{
		new RiskGame();
	}
}