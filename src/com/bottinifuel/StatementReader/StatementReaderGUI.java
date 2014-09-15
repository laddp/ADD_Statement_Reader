package com.bottinifuel.StatementReader;

/*************************************************************************************
* Change Log:
* 
*   Date         Description                                        Pgmr
*  ------------  ------------------------------------------------   -----
*  Aug 12, 2014  Record length changed for ADDs 14 upgrade.        carlonc
*  version 1.1   Changes took place in ADD_FF_Parser
*                Look for comment 081214
***************************************************************************************/   

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.bottinifuel.FlatFileParser.DuplicateFinder;
import com.bottinifuel.FlatFileParser.FileFormatException;
import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.FlatFile.FileTypeEnum;
import com.bottinifuel.FlatFileParser.Records.CreditInfoRecord;
import com.bottinifuel.FlatFileParser.Records.DocumentRecord;
import com.bottinifuel.FlatFileParser.Records.StatementRecord;
import javax.swing.JCheckBox;

public class StatementReaderGUI {

	public String version = "v1.1";  
	private JFrame mainFrame;
	private JList<File> inputFileList;

	private JFileChooser inputChooser;
	private DefaultListModel<File> inputFileListModel;
	private JTextArea outputText;
	private JButton runButton;
	private JCheckBox chckbxDropTriplepLoan;
	private JCheckBox chckbxCheckBudgets;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					StatementReaderGUI window = new StatementReaderGUI();
					window.mainFrame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public StatementReaderGUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the mainFrame.
	 */
	private void initialize() {
		mainFrame = new JFrame();
		mainFrame.setTitle("ADD Statement Reader "+version+" For ADDs v14");
		mainFrame.setBounds(100, 100, 800, 650);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel centerPanel = new JPanel();
		mainFrame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		GridBagLayout gbl_centerPanel = new GridBagLayout();
		gbl_centerPanel.columnWeights = new double[]{1.0, 0.0};
		gbl_centerPanel.columnWidths = new int[]{0, 0};
		gbl_centerPanel.rowWeights = new double[]{1.0, 0.0, 0.0};
		gbl_centerPanel.rowHeights = new int[]{50, 0, 0};
		centerPanel.setLayout(gbl_centerPanel);

		inputFileListModel = new DefaultListModel<File>();

		JScrollPane listScrollPane = new JScrollPane();
		GridBagConstraints gbc_listScrollPane = new GridBagConstraints();
		gbc_listScrollPane.weighty = 1.0;
		gbc_listScrollPane.insets = new Insets(5, 5, 5, 5);
		gbc_listScrollPane.fill = GridBagConstraints.BOTH;
		gbc_listScrollPane.gridy = 0;
		gbc_listScrollPane.gridx = 0;
		centerPanel.add(listScrollPane, gbc_listScrollPane);
		inputFileList = new JList<File>(inputFileListModel);
		listScrollPane.setViewportView(inputFileList);

		JPanel listControlsPanel = new JPanel();
		GridBagConstraints gbc_listControlsPanel = new GridBagConstraints();
		gbc_listControlsPanel.insets = new Insets(5, 5, 5, 5);
		gbc_listControlsPanel.fill = GridBagConstraints.BOTH;
		gbc_listControlsPanel.gridy = 0;
		gbc_listControlsPanel.gridx = 1;
		centerPanel.add(listControlsPanel, gbc_listControlsPanel);
		GridBagLayout gbl_listControlsPanel = new GridBagLayout();
		listControlsPanel.setLayout(gbl_listControlsPanel);

		JButton addBtn = new JButton("Add...");
		addBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (inputChooser == null)
				{
					inputChooser = new JFileChooser("S:/Accounts Receivable/Billing/Statements/");
					inputChooser.setMultiSelectionEnabled(true);
					FileNameExtensionFilter mfaFilter = new FileNameExtensionFilter("Statements (.MFA)", "MFA");
					inputChooser.setFileFilter(mfaFilter);
				}
				int rc = inputChooser.showOpenDialog(mainFrame);
				if (rc == JFileChooser.APPROVE_OPTION)
				{
					for (File f : inputChooser.getSelectedFiles())
					{
						inputFileListModel.addElement(f);
					}
					if (inputFileListModel.getSize() > 0)
						runButton.setEnabled(true);
				}
			}
		});
		GridBagConstraints gbc_addBtn = new GridBagConstraints();
		gbc_addBtn.anchor = GridBagConstraints.NORTHWEST;
		gbc_addBtn.insets = new Insets(0, 0, 0, 5);
		gbc_addBtn.gridx = 0;
		gbc_addBtn.gridy = 0;
		listControlsPanel.add(addBtn, gbc_addBtn);

		JButton removeBtn = new JButton("Remove");
		removeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				for (int index : inputFileList.getSelectedIndices())
					inputFileListModel.remove(index);
				if (inputFileListModel.getSize() == 0)
					runButton.setEnabled(false);
			}
		});
						
		GridBagConstraints gbc_removeBtn = new GridBagConstraints();
		gbc_removeBtn.anchor = GridBagConstraints.NORTHWEST;
		gbc_removeBtn.gridx = 0;
		gbc_removeBtn.gridy = 1;
		listControlsPanel.add(removeBtn, gbc_removeBtn);
		
		JScrollPane outputScrollPane = new JScrollPane();
		GridBagConstraints gbc_outputScrollPane = new GridBagConstraints();
		gbc_outputScrollPane.insets = new Insets(5, 5, 5, 5);
		gbc_outputScrollPane.weighty = 5.0;
		gbc_outputScrollPane.fill = GridBagConstraints.BOTH;
		gbc_outputScrollPane.gridwidth = 2;
		gbc_outputScrollPane.gridx = 0;
		gbc_outputScrollPane.gridy = 2;
		centerPanel.add(outputScrollPane, gbc_outputScrollPane);
		
		outputText = new JTextArea();
		outputText.setEditable(false);
		outputText.setFont(new Font("Courier New", Font.PLAIN, 11));
		outputScrollPane.setViewportView(outputText);
		
		JLabel messagesLabel = new JLabel("Messages:");
		GridBagConstraints gbc_messagesLabel = new GridBagConstraints();
		gbc_messagesLabel.weighty = -1.0;
		gbc_messagesLabel.weightx = -1.0;
		gbc_messagesLabel.anchor = GridBagConstraints.WEST;
		gbc_messagesLabel.insets = new Insets(5, 5, 5, 5);
		gbc_messagesLabel.gridx = 0;
		gbc_messagesLabel.gridy = 1;
		centerPanel.add(messagesLabel, gbc_messagesLabel);
		
		JPanel inputLabelPanel = new JPanel();
		mainFrame.getContentPane().add(inputLabelPanel, BorderLayout.NORTH);
		GridBagLayout gbl_inputLabelPanel = new GridBagLayout();
		gbl_inputLabelPanel.columnWidths = new int[]{365, 54, 0};
		gbl_inputLabelPanel.rowHeights = new int[]{14, 0};
		gbl_inputLabelPanel.columnWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		gbl_inputLabelPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		inputLabelPanel.setLayout(gbl_inputLabelPanel);
		
		JLabel lblInputFiles = new JLabel("Input Files:");
		GridBagConstraints gbc_lblInputFiles = new GridBagConstraints();
		gbc_lblInputFiles.insets = new Insets(5, 5, 5, 5);
		gbc_lblInputFiles.anchor = GridBagConstraints.LINE_START;
		gbc_lblInputFiles.gridx = 0;
		gbc_lblInputFiles.gridy = 0;
		inputLabelPanel.add(lblInputFiles, gbc_lblInputFiles);
		JPanel bottomPanel = new JPanel();
		mainFrame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		runButton = new JButton("Run");
		runButton.setEnabled(false);
		runButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doRun();
			}
		});
		
		chckbxCheckBudgets = new JCheckBox("Check Budgets");
		bottomPanel.add(chckbxCheckBudgets);

		chckbxDropTriplepLoan = new JCheckBox("Drop TripleP Loan Transactions");
		bottomPanel.add(chckbxDropTriplepLoan);

		bottomPanel.add(runButton);
		
		JButton saveButton = new JButton("Save...");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doSave();
			}
		});
		bottomPanel.add(saveButton);
	}

	private void doRun() {
		List<FlatFile> flatFiles = new LinkedList<FlatFile>();

        int totalStatements = 0;
        int totalSpecials = 0;
        BigDecimal totalFinanceCharge = new BigDecimal(0);
        BigDecimal totalBudgetInterest = new BigDecimal(0);
        BigDecimal totalBilled = new BigDecimal(0);

        outputText.setText(null);
        
        for (Object fileObj : inputFileListModel.toArray())
        {
			try {
	        	File file = (File)fileObj;
	        	FileReader fr;
				fr = new FileReader(file);
	            outputText.append("========================================================================\n" +
                        "Processing statement file " + file + "\n");
	            
	            ByteArrayOutputStream messages = new ByteArrayOutputStream();
	            
	            FlatFile ff = new FlatFile(FileTypeEnum.STATEMENT_FILE, file.getName(), fr, new PrintStream(messages), 1, false);
	            flatFiles.add(ff);

                totalStatements += ff.Trailer.TotalItems;
                totalSpecials   += ff.getSpecialHandlingCount();
                totalFinanceCharge = totalFinanceCharge.add(ff.getTotalFinanceCharges());
                totalBudgetInterest = totalBudgetInterest.add(ff.getTotalBudgetInterest());
                totalBilled = totalBilled.add(ff.Trailer.TotalAmountBilled);

                int currentCount = 0;
                int pastDue1Count = 0;
                int pastDue2Count = 0;
                int pastDue3Count = 0;
                int pastDue4Count = 0;
                
                BigDecimal currentDollars = new BigDecimal(0);
                BigDecimal pastDue1Dollars = new BigDecimal(0);
                BigDecimal pastDue2Dollars = new BigDecimal(0);
                BigDecimal pastDue3Dollars = new BigDecimal(0);
                BigDecimal pastDue4Dollars = new BigDecimal(0);
                
                for (DocumentRecord document : ff.Documents)
                {
                	if (document instanceof StatementRecord)
                	{
                		StatementRecord stmt = (StatementRecord)document;
                		if (stmt.CreditInfoRecord != null)
                		{
                			CreditInfoRecord credit = stmt.CreditInfoRecord;
                			currentDollars  = currentDollars .add(credit.CurrentDollars);
                			pastDue1Dollars = pastDue1Dollars.add(credit.PastDue1);
                			pastDue2Dollars = pastDue2Dollars.add(credit.PastDue2);
                			pastDue3Dollars = pastDue3Dollars.add(credit.PastDue3);
                			pastDue4Dollars = pastDue4Dollars.add(credit.PastDue4);
                			
                			if (credit.PastDue4.compareTo(BigDecimal.ZERO) > 0)
                				pastDue4Count++;
                			else if (credit.PastDue3.compareTo(BigDecimal.ZERO) > 0)
                				pastDue3Count++;
                			else if (credit.PastDue2.compareTo(BigDecimal.ZERO) > 0)
                				pastDue2Count++;
                			else if (credit.PastDue1.compareTo(BigDecimal.ZERO) > 0)
                				pastDue1Count++;
                			else
                				currentCount++;
                		}
                	}
                }

                NumberFormat currency = DecimalFormat.getCurrencyInstance();
                outputText.append("\n\tDate: " + ff.Trailer.FileDateTime + '\n');
                outputText.append("\tTotal statements: " + ff.Trailer.TotalItems + '\n');
                outputText.append("\tSpecial handling: " + ff.getSpecialHandlingCount() + '\n');
                outputText.append("\tFinance charges:  " + currency.format(ff.getTotalFinanceCharges()) + '\n');
                outputText.append("\tBudget interest:  " + currency.format(ff.getTotalBudgetInterest()) + '\n');
                outputText.append("\tTotal Billed:     " + currency.format(ff.Trailer.TotalAmountBilled)   + "\n");
                outputText.append("\tAging Summary (Curr/30-59/60-89/90-119/120+):\n\t\t" +
                		currentCount + " / " + pastDue1Count + " / " + pastDue2Count + " / " + pastDue3Count + " / " + pastDue4Count + "\n\t\t" +
                		currency.format(currentDollars)  + " / " + currency.format(pastDue1Dollars) + " / " +
                		currency.format(pastDue2Dollars) + " / " + currency.format(pastDue3Dollars) + " / " +
                		currency.format(pastDue4Dollars) + "\n\n");

                outputText.append(messages.toString());
                
                if (chckbxCheckBudgets.isSelected())
                	outputText.append(CheckBudgetBalances.CheckBudgetBalancesInFile(ff));
                
                outputText.append("\n\nRandom sample of accounts in the file:\n");
                Random random = new Random();
                Set<Integer> randomSelections = new HashSet<Integer>();
                int count = (ff.Documents.size() / 50) > 20 ? (ff.Documents.size() / 50) : 20;
                if (count > ff.Documents.size())
                    count = ff.Documents.size();
                Formatter fmt = new Formatter();
                while (randomSelections.size() < count)
                {
                    int index = random.nextInt(ff.Documents.size());
                    if (!randomSelections.contains(index))
                    {
                        randomSelections.add(index);
                        fmt.format("Acct #%7d - %s\n",
                                  ff.Documents.get(index).AccountNum,
                                  ff.Documents.get(index).Name);
                    }
                }
                outputText.append(fmt.toString());
                outputText.append("\n\n");
                fmt.close();
			} catch (FileNotFoundException e) {
				outputText.append(e.toString());
			} catch (FileFormatException e) {
				outputText.append(e.toString());
			} catch (IOException e) {
				outputText.append(e.toString());
			}
        }

        if (flatFiles.size() > 1)
        {
            outputText.append("\n\n====================================================\nMulti-statement totals:\n");
            outputText.append("\tTotal statements: " + totalStatements + '\n');
            outputText.append("\tSpecial handling: " + totalSpecials + '\n');
            outputText.append("\tFinance charges: " + DecimalFormat.getCurrencyInstance().format(totalFinanceCharge) + '\n');
            outputText.append("\tBudget interest: " + DecimalFormat.getCurrencyInstance().format(totalBudgetInterest) + '\n');
            outputText.append("\tTotal Billed: " + DecimalFormat.getCurrencyInstance().format(totalBilled) + '\n');
        }
        
        Map<Integer, List<FlatFile>> dups = DuplicateFinder.FindDuplicates(flatFiles);
        if (dups != null)
        {
            outputText.append("=================================================\n" +
                              "WARNING: " + dups.size() + " Duplicate statements in file: \n");
            for (Map.Entry<Integer, List<FlatFile>> dup : dups.entrySet())
            {
            	outputText.append("\tAcct #" + dup.getKey() + " in files: \n");
                for (FlatFile ff : dup.getValue())
                {
                	outputText.append("\t\t" + ff.FileName + '\n');
                }
            }
            outputText.append("=================================================\n");
        }
        else
        {
        	outputText.append("Duplicate checking complete - no duplicates found\n");
        }
	}

	private void doSave() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		JFileChooser outputFileChooser = new JFileChooser("S:/Accounts Receivable/Billing/Statements/");
		 outputFileChooser.setSelectedFile(new File(df.format(new Date()) + ".txt"));
		int rc = outputFileChooser.showSaveDialog(mainFrame);
		if (rc == JFileChooser.APPROVE_OPTION)
		{
			try {
				FileOutputStream out = new FileOutputStream(outputFileChooser.getSelectedFile());
				PrintStream printout = new PrintStream(out);
				printout.append(outputText.getText());
				out.close();
			} catch (FileNotFoundException e) {
				outputText.append(e.toString());
			} catch (IOException e) {
				outputText.append(e.toString());
			}
		}
	}
}
