package risk;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

public class RiskCalculator
{	
	public static final double W1V1 = 15.0 / 36.0;
	public static final double L1V1 = 21.0 / 36.0;
	public static final double W2V1 = 125.0 / 216.0;
	public static final double L2V1 = 91.0 / 216.0;
	public static final double W3V1 = 855.0 / 1296.0;
	public static final double L3V1 = 441.0 / 1296.0;
	public static final double W1V2 = 55.0 / 216.0;
	public static final double L1V2 = 161.0 / 216.0;
	public static final double W2V2 = 295.0 / 1296.0;
	public static final double L2V2 = 581.0 / 1296.0;
	public static final double T2V2 = 420.0 / 1296.0;
	public static final double W3V2 = 2890.0 / 7776.0;
	public static final double L3V2 = 2275.0 / 7776.0;
	public static final double T3V2 = 2611.0 / 7776.0;
	
	private int attackers;
	private int defenders;
	
	private double[][] outcome;
	
	private static JPanel outcomePnl;
	
	private static NumberFormat format = NumberFormat.getPercentInstance();
	
	public RiskCalculator(boolean show)
	{	
		format.setMaximumFractionDigits(6);
		format.setMinimumFractionDigits(6);
	}
	
	public String getResults(int a, int d)
	{
		attackers = a;
		defenders = d;
		outcome = new double[attackers + 1][defenders + 1];
		for (int i = 0; i < attackers + 1; i++)
			for (int j = 0; j < defenders + 1; j++)
				outcome[i][j] = -1.0;
		outcome[attackers][defenders] = 1.0;
		
		double winOdds = 0.0;
		double loseOdds = 0.0;
		String likelyOutcomeState = null;
		double likelyOutcome = -1.0;
		
		for (int i = attackers; i >= 2; i--)
		{
			winOdds += getOdds(i, 0);
			if (outcome[i][0] > likelyOutcome)
			{
				likelyOutcome = outcome[i][0];
				likelyOutcomeState = i + " vs " + 0;
			}
		}
		for (int i = 1; i <= defenders; i++)
		{
			loseOdds += getOdds(1, i);
			if (outcome[1][i] > likelyOutcome)
			{
				likelyOutcome = outcome[1][i];
				likelyOutcomeState = 1 + " vs " + i;
			}
		}
		
		return "Victory odds: " + format.format(winOdds) + ", Likeliest outcome: " + likelyOutcomeState;
	}
	
	public double getWinningOdds(int a, int d)
	{
		attackers = a;
		defenders = d;
		outcome = new double[attackers + 1][defenders + 1];
		for (int i = 0; i < attackers + 1; i++)
			for (int j = 0; j < defenders + 1; j++)
				outcome[i][j] = -1.0;
		outcome[attackers][defenders] = 1.0;
		
		double winOdds = 0.0, loseOdds = 0.0;
		double likelyOutcome = -1.0;
		
		for (int i = attackers; i >= 2; i--)
		{
			winOdds += getOdds(i, 0);
			if (outcome[i][0] > likelyOutcome)
				likelyOutcome = outcome[i][0];
		}
		for (int i = 1; i <= defenders; i++)
		{
			loseOdds += getOdds(1, i);
			if (outcome[1][i] > likelyOutcome)
				likelyOutcome = outcome[1][i];
		}
		return winOdds;
	}
	
	private void getOutcome(int a, int d)
	{
		attackers = a;
		defenders = d;
		outcome = new double[attackers + 1][defenders + 1];
		for (int i = 0; i < attackers + 1; i++)
			for (int j = 0; j < defenders + 1; j++)
				outcome[i][j] = -1.0;
		outcome[attackers][defenders] = 1.0;
		
		double winOdds = 0.0, loseOdds = 0.0;
		String likelyOutcomeState = "";
		double likelyOutcome = -1.0;
		
		String[][] data1 = new String[attackers - 1][3];
		String[][] data2 = new String[defenders][3];
		int k = 0;
		
		for (int i = attackers; i >= 2; i--)
		{
			winOdds += getOdds(i, 0);
			data1[k] = new String[] {"" + i, "0", format.format(outcome[i][0])};
			k++;
			if (outcome[i][0] > likelyOutcome)
			{
				likelyOutcome = outcome[i][0];
				likelyOutcomeState = i + " vs " + 0;
			}
		}
		k = 0;
		for (int i = 1; i <= defenders; i++)
		{
			loseOdds += getOdds(1, i);
			data2[k] = new String[] {"1", "" + i, format.format(outcome[1][i])};
			k++;
			if (outcome[1][i] > likelyOutcome)
			{
				likelyOutcome = outcome[1][i];
				likelyOutcomeState = 1 + " vs " + i;
			}
		}
		
		outcomePnl.removeAll();
		outcomePnl.setPreferredSize(new Dimension(550, 500));
		outcomePnl.setLayout(new BoxLayout(outcomePnl, BoxLayout.Y_AXIS));
		
		Font font = new Font("Arial", Font.BOLD, 14);
		
		JPanel pnl1 = new JPanel();
		pnl1.setLayout(new BoxLayout(pnl1, BoxLayout.Y_AXIS));
		JLabel victoryOddsLbl = new JLabel("Victory odds: " + format.format(winOdds));
		victoryOddsLbl.setFont(font);
		JLabel likelyOutcomeLbl = new JLabel("Most likely outcome: " + likelyOutcomeState + " (" + format.format(likelyOutcome) + ")");
		likelyOutcomeLbl.setFont(font);
		pnl1.add(Box.createVerticalStrut(10));
		pnl1.add(victoryOddsLbl);
		pnl1.add(Box.createVerticalStrut(10));
		pnl1.add(likelyOutcomeLbl);
		pnl1.add(Box.createVerticalStrut(10));
		
		JPanel pnl2 = new JPanel();
		pnl2.setLayout(new BoxLayout(pnl2, BoxLayout.Y_AXIS));
		String[] columns = {"Attackers Remaining", "Defenders Remaining", "Probability"};
		JTable outcomeTbl1 = new JTable(data1, columns);
		outcomeTbl1.setEnabled(false);
		JTable outcomeTbl2 = new JTable(data2, columns);
		outcomeTbl2.setEnabled(false);
		pnl2.add(outcomeTbl1.getTableHeader());
		pnl2.add(outcomeTbl1);
		pnl2.add(outcomeTbl2.getTableHeader());
		pnl2.add(outcomeTbl2);
		JScrollPane scrollPane = new JScrollPane(pnl2);
		
		outcomePnl.add(pnl1);
		outcomePnl.add(scrollPane);
		
		outcomePnl.revalidate();
	}
	
	private double getOdds(int a, int d)
	{
		if (outcome[a][d] != -1.0)
			return outcome[a][d];
		
		double odds = 0.0;
		if (a + 2 <= attackers && d >= 2)
		{
			if (a + 2 >= 4)
				odds += L3V2 * getOdds(a + 2, d);
			else if (a + 2 == 3)
				odds += L2V2 * getOdds(a + 2, d);
		}
		if (d + 2 <= defenders && a >= 3)
		{
			if (a >= 4)
				odds += W3V2 * getOdds(a, d + 2);
			else if (a == 3)
				odds += W2V2 * getOdds(a, d + 2);
		}
		if (a + 1 <= attackers && d + 1 <= defenders && d + 1 >= 2 && a + 1 >= 3)
		{
			if (a + 1 >= 4)
				odds += T3V2 * getOdds(a + 1, d + 1);
			else if (a + 1 == 3)
				odds += T2V2 * getOdds(a + 1, d + 1);
		}
		if (a + 1 <= attackers)
		{
			if (a + 1 == 2 && d >= 2)
				odds += L1V2 * getOdds(a + 1, d);
			else if (a + 1 >= 4 && d == 1)
				odds += L3V1 * getOdds(a + 1, d);
			else if (a + 1 == 3 && d == 1)
				odds += L2V1 * getOdds(a + 1, d);
			else if (a + 1 == 2 && d == 1)
				odds += L1V1 * getOdds(a + 1, d);
		}
		if (d + 1 <= defenders)
		{
			if (a == 2 && d + 1 >= 2)
				odds += W1V2 * getOdds(a, d + 1);
			else if (a >= 4 && d + 1 == 1)
				odds += W3V1 * getOdds(a, d + 1);
			else if (a == 3 && d + 1 == 1)
				odds += W2V1 * getOdds(a, d + 1);
			else if (a == 2 && d + 1 == 1)
				odds += W1V1 * getOdds(a, d + 1);
		}
		outcome[a][d] = odds;
		return odds;
	}
	
	public static void main(String[] args)
	{
		final JFrame riskCalcWin = new JFrame("Jeff Sullivan's Risk Calculator");
		riskCalcWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		riskCalcWin.setResizable(false);
		riskCalcWin.setLayout(new BorderLayout());
		
		final RiskCalculator riskCalc = new RiskCalculator(true);
		
		JLabel titleLbl = new JLabel("Risk Calculator by Jeffrey \"The Great\" Sullivan");
		titleLbl.setFont(new Font("Arial", Font.BOLD, 24));
		JPanel titlePnl = new JPanel();
		titlePnl.add(titleLbl);
		riskCalcWin.add(titlePnl, BorderLayout.PAGE_START);
		
		JPanel optionPnl = new JPanel();
		final JTextField attackTxt = new JTextField(4);
		final JTextField defendTxt = new JTextField(4);
		JButton calcBtn = new JButton("Calculate Odds");
		calcBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					riskCalc.getOutcome(Integer.parseInt(attackTxt.getText()), Integer.parseInt(defendTxt.getText()));
					riskCalcWin.repaint();
					riskCalcWin.pack();
				}
				catch (Exception ex)
				{
					System.err.println("Invalid number of troops entered");
				}
			}
		});
		optionPnl.add(new JLabel("Attackers:"));
		optionPnl.add(attackTxt);
		optionPnl.add(new JLabel("Defenders:"));
		optionPnl.add(defendTxt);
		optionPnl.add(calcBtn);
		riskCalcWin.add(optionPnl, BorderLayout.PAGE_END);
		
		outcomePnl = new JPanel();
		riskCalcWin.add(outcomePnl, BorderLayout.CENTER);
		
		riskCalcWin.pack();
		riskCalcWin.setVisible(true);
	}
}
