package ro.cst.tsearch.search.name;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * NameApplet
 * 
 * Name parsing and matching applet.
 *
 * @author catalinc
 */
public class NameApplet extends JApplet implements ActionListener {
	/**
	 * Input name 1
	 */
	JTextField textName1 = new JTextField(30);
	/**
	 * Input name 2
	 */
	JTextField textName2 = new JTextField(30);
	/**
	 * Run Forest !
	 */
	JButton goButton = new JButton("Parse and match"); 
	/**
	 * Reset all
	 */
	JButton resetButton = new JButton("Reset");
	/**
	 * Result area
	 */
	JTextArea textResults = new JTextArea(15,30);
	/**
	 * Select parsing schema
	 */
	JComboBox parsingSchema = new JComboBox();
	/**
	 * Matching weights
	 */
	JTextField[] textWeights = new JTextField[] {
		new JTextField(5),
		new JTextField(5),
		new JTextField(5),
		new JTextField(5),
		new JTextField(5),
		new JTextField(5)
	};
	/**
	 * Weights labels
	 */
	JLabel[] labelsWeights = new JLabel[] {
		new JLabel("Title/Prefix  weight: "),
		new JLabel("First Name weigth: "),
		new JLabel("Middle Name weigth: "),
		new JLabel("Last Name weigth: "),
		new JLabel("Suffix weigth: "),		
		new JLabel("Degree Name weigth: ")
	};
	/**
	 * Copy menu
	 */
	JMenuItem copyItem	= new JMenuItem("Copy");
	/**
	 * Paste menu
	 */
	JMenuItem pasteItem = new JMenuItem("Paste");
	/**
	 * Popup menu
	 */
	JPopupMenu popUpMenu = new JPopupMenu("Tools");
	
	/**
	 * Name parser.
	 */
	INameParser nameParser = new BasicNameParser("LFM");
	/**
	 * Name matcher.
	 */
	INameMatcher nameMatcher = new BasicNameMatcher();	
	/**
	 * Default constructor.
	 */
	public NameApplet() {
		
		JPanel inputPanel = new JPanel();
		JPanel resultsPanel = new JPanel();
		JPanel mainPanel = new JPanel();
		JScrollPane scroll = new JScrollPane(textResults);
		
		goButton.addActionListener(this);
		resetButton.addActionListener(this);
		
		parsingSchema.addItem(new String("LFM (Last First Middle)"));
		parsingSchema.addItem(new String("LMF (Last Middle First)"));
		parsingSchema.addItem(new String("FML (First Middle Last"));
		
		textName1.setFont(new Font("Helvetica",Font.BOLD,12));
		textName2.setFont(new Font("Helvetica",Font.BOLD,12));
		textResults.setFont(new Font("Helvetica",Font.BOLD,12));				
		textName1.setEditable(true);
		textName2.setEditable(true);

		popUpMenu.add(copyItem);
		popUpMenu.add(pasteItem);

		copyItem.addActionListener(this);
		pasteItem.addActionListener(this);

		textName1.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent evt) {
					if (evt.isPopupTrigger()) {
						popUpMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
				public void mouseReleased(MouseEvent evt) {
					if (evt.isPopupTrigger()) {
						popUpMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
			});

		textName2.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent evt) {
					if (evt.isPopupTrigger()) {
						popUpMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
				public void mouseReleased(MouseEvent evt) {
					if (evt.isPopupTrigger()) {
						popUpMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
			});

		textResults.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent evt) {
					if (evt.isPopupTrigger()) {
						popUpMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
				public void mouseReleased(MouseEvent evt) {
					if (evt.isPopupTrigger()) {
						popUpMenu.show(evt.getComponent(), evt.getX(), evt.getY());
					}
				}
			});

		
		GridBagLayout l = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		inputPanel.setLayout(l);
		JLabel l1 = new JLabel("Input string 1: ");
		c.insets = new Insets(10,0,0,0);
		l.setConstraints(l1,c);
		inputPanel.add(l1);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10,0,0,0);
		l.setConstraints(textName1,c);
		inputPanel.add(textName1);
		JLabel l2 = new JLabel("Input string 2: ");
		c.insets = new Insets(10,0,0,0);
		c.gridwidth = GridBagConstraints.RELATIVE;
		l.setConstraints(l2,c);
		inputPanel.add(l2);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		l.setConstraints(textName2,c);
		inputPanel.add(textName2);
		JPanel buttonsPanel = new JPanel(new GridLayout(1,3));
		buttonsPanel.add(new JLabel("Parsing schema: "));
		buttonsPanel.add(parsingSchema);
		buttonsPanel.add(goButton);
		buttonsPanel.add(resetButton);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,5,5,5);
		l.setConstraints(buttonsPanel,c);
		inputPanel.add(buttonsPanel);
		
		JPanel panelWeights = new JPanel(new GridLayout(2,6));
		for(int i = 0; i < 6; i++) {
			panelWeights.add(labelsWeights[i]);
		}
		for(int i = 0; i < 6; i++) {
			panelWeights.add(textWeights[i]);
		}
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,0,5,0);
		l.setConstraints(panelWeights,c);
		inputPanel.add(panelWeights);

		JPanel helpPanel = new JPanel(new BorderLayout());
		JLabel l3 = new JLabel("<html>" +
								"<table><tr><td bgcolor='#d5d5ff'>" 
								+ "1. Insert testing strings on specified inputs (1 and/or 2)<br>" 
								+ "2. Select desired parsing schema , LFM will do for most strings.<br>"
								+ "3. Select desired weight for each name token for matching process.<br>"
								+ "4. Click on <b>Parse and match</b> button."
								+"</td><td bgcolor='#e6e6e6'>"
								+ "(* About name tokens:<br>"
								+ "- Name prefix: MRS,MS,REV etc.<br>"
								+ "- Name suffix: JR,SR or III,I,II etc [Roman numbers converted in arabic form]<br>"
								+ "- Name degree: PHD,BS,MBA etc. )"
								+ "</td></tr><tr><td colspan=2 bgcolor='#caeeff'>"
								+ "Output will be displayed in textarea below as follow:<br>"
								+ "- Normalized input string<br>"
								+ "- Normalized form for name as follow: <font color='blue'><b>PREFIX : FIRST : MIDDLE : LAST : SUFIX : DEGREE</b></font><br>"
								+ "- Matching result for every name<br>"
								+ "</td></tr></table>"
								+ "</html>");
		l3.setFont(new Font("Helvetica",Font.PLAIN,12));
		helpPanel.add(l3,BorderLayout.CENTER);	
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,0,5,0);
		l.setConstraints(helpPanel,c);
		inputPanel.add(helpPanel);

		
		resultsPanel.setLayout(new BorderLayout());
		resultsPanel.add(scroll,BorderLayout.CENTER);
		
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(inputPanel,BorderLayout.NORTH);
		mainPanel.add(resultsPanel,BorderLayout.SOUTH);

		resetData();				
		this.getContentPane().add(mainPanel);
	}
	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(goButton)) {
			try {
				doParseAndMatch();
			} catch(Exception ex) {
				showError(ex.getMessage());
			}
		} else if(e.getSource().equals(resetButton)) {
			resetData();
		} else if(e.getSource().equals(copyItem)) {
			Component c = popUpMenu.getInvoker();
			if(c instanceof JTextComponent) {
				((JTextComponent)c).copy();			
			}
		} else if(e.getSource().equals(pasteItem)) {
			Component c = popUpMenu.getInvoker();
			if(c instanceof JTextComponent) {
				((JTextComponent)c).paste();			
			}			
		}
	}	
	/**
	 * Collect input weights.
	 */
	protected double[] getWeights() throws Exception {
		double[] weights = new double[6];
		for(int i=0; i < textWeights.length; i++) {
			try {
				weights[i] = Double.parseDouble(textWeights[i].getText());
			} catch(Exception ex) {
				throw new Exception("Incorrect data on field: [" + labelsWeights[i].getText() + "]\n" 
							+ "Input value must be number");
			}
			if(weights[i] > 1.0 || weights[i] < 0.0) {
				throw new Exception("Incorrect data on field: [" + labelsWeights[i].getText() + "]\n" +
							"Input value should be between 0.0 and 1.0");
			}
		}
		return weights;
	}
	/**
	 * Parse input.
	 */
	protected void doParseAndMatch() throws Exception {
		((BasicNameParser)nameParser).setParseSchema(parsingSchema.getSelectedItem().toString().substring(0,3));		
		ArrayList names1 = nameParser.parseNames(textName1.getText());
		ArrayList names2 = nameParser.parseNames(textName2.getText());

		textResults.append("\n\t** PARSING **\n\n");
		
		textResults.append("INPUT STRING 1 [" + textName1.getText() +"]\n");
		textResults.append("NORMALIZED FORM [" + NameNormalizer.normalize(textName1.getText()) + "]\n\n");
		int k = 0;
		for (Iterator it = names1.iterator(); it.hasNext();) {
			Name n = (Name) it.next();
			textResults.append("NAME #" + (k++) + " " + n.toString()+"\t COMPANY=" + n.isCompany()+"\n");
		}		
		k = 0;
		textResults.append("\nINPUT STRING 2 [" + textName2.getText() +"]\n");
		textResults.append("NORMALIZED FORM [" + NameNormalizer.normalize(textName2.getText()) + "]\n\n");
		for (Iterator it = names2.iterator(); it.hasNext();) {
			Name n = (Name) it.next();
			textResults.append("NAME #" + (k++) + " " + n.toString()+"\t COMPANY=" + n.isCompany()+"\n");
		}
		
		double weights[] = getWeights();
		nameMatcher.setWeightArray(weights);

		textResults.append("\n\t** MATCHING **\n\n");
		
		for (Iterator it1 = names1.iterator(); it1.hasNext();) {
			Name n1 = (Name) it1.next();
			for (Iterator it2 = names2.iterator(); it2.hasNext();) {
				Name n2 = (Name) it2.next();
				textResults.append("MATCHING\t[" + n1 + "]\t vs \t[" + n2 + "]\t SCORE=" + nameMatcher.match(n1,n2)+"\n"); 				
			}			
		}						
	}
	/**
	 * Reset input data form.
	 */
	protected void resetData() {
		// reset weights
		for(int i = 0; i < textWeights.length; i++) {
			textWeights[i].setText("1.0");
		}
		// reset input names
		textName1.setText("");
		textName2.setText("");
		// reset results tab
		textResults.setText("");
		textResults.setEditable(false);
	}

	
	/**
	 * Shows error message.
	 *  
	 * @param err Error message to be shown.
	 */
	protected void showError(String err) {
		JOptionPane.showMessageDialog(this,err,"Input error",JOptionPane.ERROR_MESSAGE);
	}
}
