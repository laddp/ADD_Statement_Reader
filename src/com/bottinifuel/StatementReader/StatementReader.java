/*
 * Created on Jun 10, 2010 by pladd
 *
 *
 *************************************************************************/
 /* Change Log:
 *
 *   Date         Description                                          Pgmr
 *  ------------  --------------------------------------------------   -----
 *  Oct 10, 2013  Allow a file name as input to the removeStatements   carlonc
 *                option. The file is a list of accounts to be
 *                removed, one account per line. The old way using
 *                a string of comma separated accounts is still allowed. 
 *                Look for comment 101013      
 *************************************************************************/
package com.bottinifuel.StatementReader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.bottinifuel.AutoHelpParser;
import com.bottinifuel.FlatFileParser.DateChecker;
import com.bottinifuel.FlatFileParser.DuplicateFinder;
import com.bottinifuel.FlatFileParser.FileFormatException;
import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.FlatFile.FileTypeEnum;
import com.bottinifuel.FlatFileParser.Records.DocumentRecord;
import com.bottinifuel.FlatFileParser.Records.LineItemRecord;
import com.bottinifuel.FlatFileParser.Records.PostingCodeRecord;
import com.bottinifuel.FlatFileParser.Records.Record;
import com.bottinifuel.FlatFileParser.Records.Record.RecordTypeEnum;
import com.bottinifuel.FlatFileParser.Records.StatementRecord;

/**
 * @author pladd
 *
 */
public class StatementReader
{
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.out.println("Error: missing argument");
            return;
        }
        
        AutoHelpParser argParser = new AutoHelpParser();

        AutoHelpParser.Option dropAfterDateOption = argParser.addStringOption('a', "dropAfter");
        argParser.addHelp(dropAfterDateOption, "Drop statement items after this date (requires -d)");

        AutoHelpParser.Option dropBeforeDateOption = argParser.addStringOption('b', "dropBefore");
        argParser.addHelp(dropBeforeDateOption, "Drop statement items after this date (requires -d)");
        
        AutoHelpParser.Option dropCategoryOption = argParser.addStringOption('c', "dropCategories");
        argParser.addHelp(dropCategoryOption, "Remove statements in account category(s)");
        
        AutoHelpParser.Option dumpFileOption = argParser.addStringOption('d', "dump");
        argParser.addHelp(dumpFileOption, "Dump contents of first file to dump file");
        
        AutoHelpParser.Option combineFinanceChargeOption = argParser.addStringOption('g', "combineFinanceCharge");
        argParser.addHelp(combineFinanceChargeOption, "Combine finance charge transactions into finance charge record");

        AutoHelpParser.Option helpOption = argParser.addBooleanOption('h', "help");
        argParser.addHelp(helpOption, "Display program help");
        
        AutoHelpParser.Option dropDivisionOption = argParser.addStringOption('i', "dropDivs");
        argParser.addHelp(dropDivisionOption, "Drop statements from divisions (requires -d)");

        AutoHelpParser.Option dropBalanceBelowOption = argParser.addDoubleOption('l', "dropBalanceBelow");
        argParser.addHelp(dropCategoryOption, "Remove statements with balance below");
        
        AutoHelpParser.Option dropLoanToTriplePoption = argParser.addBooleanOption('p', "dropLoanToTripleP");
        argParser.addHelp(dropLoanToTriplePoption, "Remove offsetting TripleP loan items (post code 235)");

        AutoHelpParser.Option removeDuplicatesOption = argParser.addBooleanOption('r', "removeDupes");
        argParser.addHelp(removeDuplicatesOption, "Remove duplicates from first file in list (requires -d)");
        
        AutoHelpParser.Option specialHandlingDivOption = argParser.addStringOption('s', "specialHandlingDiv");
        argParser.addHelp(specialHandlingDivOption, "Set All statements to have special handling");
        
        AutoHelpParser.Option listAllOption = argParser.addBooleanOption('t', "listAll");
        argParser.addHelp(listAllOption, "List all accounts in file");

        AutoHelpParser.Option dropItemsOption = argParser.addStringOption('x', "removeStatements");
        argParser.addHelp(dropItemsOption, "Remove statement for account(s) (requires -d)");
        
        boolean       removeDuplicates         = false;
        FileWriter    dumpFile                 = null;
        List<Integer> dropStatements           = new LinkedList<Integer>();
        Date          dropAfterDate            = null;
        Date          dropBeforeDate           = null;
        List<Integer> dropDivs                 = new LinkedList<Integer>();
        boolean       dropLoanToTripleP        = false;
        Date          combineFinanceChargeDate = null;
        List<Integer> specialHandlingDivs      = new LinkedList<Integer>();
        List<Integer> dropCategories           = new LinkedList<Integer>();
        BigDecimal    dropBalanceBelow         = null;
        boolean       listAll				   = false;

        try
        {
            argParser.parse(args);
            if (Boolean.TRUE.equals(argParser.getOptionValue(helpOption)))
            {
                argParser.printUsage();
                System.exit(0);
                return;
            }

            Object o = argParser.getOptionValue(removeDuplicatesOption);
            if (o != null)
                removeDuplicates = (Boolean)o;
            
            o = argParser.getOptionValue(listAllOption);
            if (o != null)
                listAll = (Boolean)o;

            o = argParser.getOptionValue(dropLoanToTriplePoption);
            if (o != null)
            {
            	dropLoanToTripleP = (Boolean)o;
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping loan items");
            }
            
            o = argParser.getOptionValue(combineFinanceChargeOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when combining finance charges");
                String combineFinanceCharge = (String)o;
                try {
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    combineFinanceChargeDate = df.parse(combineFinanceCharge);
                }
                catch (ParseException e1)
                {
                    System.out.println("Invalid finance charge date format - should be MM/dd/yyyy");
                    System.exit(1);
                }
            }

            o = argParser.getOptionValue(dropItemsOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping items");
                String dropItemList = (String)o;
                // new if leg for 101013 change   
                if (dropItemList.indexOf(',') < 0) {  // option is a file name
                	try {
	                	BufferedReader br = new BufferedReader(new FileReader(dropItemList));
	        			String line;
	                	while ((line = br.readLine()) != null) {
	                		try {
	                		   dropStatements.add(Integer.parseInt(line.trim()));
	                		} catch (NumberFormatException e) { 
	                			throw new Exception("Account numbers must be numeric in " + dropItemList);
	                		}                		 
	        			}
	                	br.close();
                	} catch (IOException e){
                		   throw new Exception("Unable to open file " + dropItemList);
                	}
                }
                else { // option is a list of comma separated accounts
                    for (String item : dropItemList.split("[ ,]"))
                    {
                        dropStatements.add(Integer.parseInt(item));
                    }
                }    
            }
            
            o = argParser.getOptionValue(dropAfterDateOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping items");
                String dropAfter = (String)o;
                try {
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    dropAfterDate = df.parse(dropAfter);
                }
                catch (ParseException e1)
                {
                    System.out.println("Invalid drop after date format - should be MM/dd/yyyy");
                    System.exit(1);
                }
            }

            o = argParser.getOptionValue(dropBeforeDateOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping items");
                String dropBefore = (String)o;
                try {
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    dropBeforeDate = df.parse(dropBefore);
                }
                catch (ParseException e1)
                {
                    System.out.println("Invalid drop before date format - should be MM/dd/yyyy");
                    System.exit(1);
                }
            }

            o = argParser.getOptionValue(dropDivisionOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping divisions");
                String dropDivsList = (String)o;
                for (String item : dropDivsList.split("[ ,]"))
                {
                    dropDivs.add(Integer.parseInt(item));
                }
            }
            
            o = argParser.getOptionValue(specialHandlingDivOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping divisions");
                String dropDivsList = (String)o;
                for (String item : dropDivsList.split("[ ,]"))
                {
                	specialHandlingDivs.add(Integer.parseInt(item));
                }
            }

            o = argParser.getOptionValue(dropCategoryOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping categories");
                String dropCats = (String)o;
                for (String item : dropCats.split("[ ,]"))
                {
                    dropCategories.add(Integer.parseInt(item));
                }
            }
            
            o = argParser.getOptionValue(dropBalanceBelowOption);
            if (o != null)
            {
                if (argParser.getRemainingArgs().length > 1)
                    throw new Exception("Multiple input files not permitted when dropping balances");
                dropBalanceBelow = new BigDecimal((Double)o);
            }

            o = argParser.getOptionValue(dumpFileOption);
            if (o == null)
            {
                if (removeDuplicates)
                    throw new Exception("Dump file required when --removeDupes specified");
                if (dropStatements.size() > 0)
                    throw new Exception("Dump file required when --removeStatements specified");
                if (dropAfterDate != null)
                    throw new Exception("Dump file required when --dropAfter specified");
                if (dropBeforeDate != null)
                    throw new Exception("Dump file required when --dropAfter specified");
                if (dropDivs.size() > 0)
                    throw new Exception("Dump file required when --dropDivs specified");
                if (dropLoanToTripleP)
                	throw new Exception("Dump file required when --dropLoanToTripleP specified");
                if (specialHandlingDivs.size() > 0)
                    throw new Exception("Dump file required when --specialHandlingDivs specified");
                if (combineFinanceChargeDate != null)
                	throw new Exception("Dump file required when --combineFinanceCharge specified");
                if (dropCategories.size() > 0)
                    throw new Exception("Dump file required when --dropCategories specified");
            }
            else
            {
                String dumpFileName = (String)o;
                dumpFile = new FileWriter(dumpFileName);
            }
            
                
                        
        }
        catch (AutoHelpParser.OptionException e)
        {
            System.err.println(e);
            argParser.printUsage();
            System.exit(2);
            return;
        }
        catch (IOException e)
        {
            System.err.println(e);
            System.exit(2);
        }
        catch (Exception e)
        {
            System.err.println(e);
            argParser.printUsage();
            System.exit(2);
            return;
        }

        List<FlatFile> flatFiles = new LinkedList<FlatFile>();

        int totalStatements = 0;
        int totalSpecials = 0;
        BigDecimal totalFinanceCharge = new BigDecimal(0);
        BigDecimal totalBudgetInterest = new BigDecimal(0);
        BigDecimal totalBilled = new BigDecimal(0);

        for (String file : argParser.getRemainingArgs())
        {
            try
            {
                FileReader fr = new FileReader(file);
                System.out.println("\n\n========================================================================\n" +
                                   "Processing statement file " + file + "\n");
                FlatFile ff = new FlatFile(FileTypeEnum.STATEMENT_FILE, file, fr, System.out, 1, false);
                flatFiles.add(ff);

                DateChecker.CheckDates(ff);
                
                totalStatements += ff.Trailer.TotalItems;
                totalSpecials   += ff.getSpecialHandlingCount();
                totalFinanceCharge = totalFinanceCharge.add(ff.getTotalFinanceCharges());
                totalBudgetInterest = totalBudgetInterest.add(ff.getTotalBudgetInterest());
                totalBilled = totalBilled.add(ff.Trailer.TotalAmountBilled);
                
                System.out.println("\n\n\tDate: " + ff.Trailer.FileDateTime);
                System.out.println("\tTotal statements: " + ff.Trailer.TotalItems);
                System.out.println("\tSpecial handling: " + ff.getSpecialHandlingCount());
                System.out.println("\tFinance charges: " + DecimalFormat.getCurrencyInstance().format(ff.getTotalFinanceCharges()));
                System.out.println("\tBudget interest: " + DecimalFormat.getCurrencyInstance().format(ff.getTotalBudgetInterest()));
                System.out.println("\tTotal Billed: " + DecimalFormat.getCurrencyInstance().format(ff.Trailer.TotalAmountBilled));
                System.out.println();

                if (listAll)
                {
                	System.out.println("Acct#\tFCHG");
                	for (DocumentRecord doc : ff.Documents)
                	{
                		StatementRecord stmt = (StatementRecord)doc;

                		System.out.println(stmt.AccountNum + "\t" + stmt.CreditInfoRecord.getFinanceCharge());
                	}
                }
                
                System.out.println("Random sample of accounts in the file:");
                Random random = new Random();
                Set<Integer> randomSelections = new HashSet<Integer>();
                int count = (ff.Documents.size() / 50) > 20 ? (ff.Documents.size() / 50) : 20;
                if (count > ff.Documents.size())
                    count = ff.Documents.size();
                while (randomSelections.size() < count)
                {
                    int index = random.nextInt(ff.Documents.size());
                    if (!randomSelections.contains(index))
                    {
                        randomSelections.add(index);
                        System.out.printf("Acct #%7d - %s\n",
                                          ff.Documents.get(index).AccountNum,
                                          ff.Documents.get(index).Name);
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                return;
            }
            catch (FileFormatException e)
            {
            	System.out.println("e="+e);
                return;
            }
            catch (IOException e)
            {
                return;
            }
        }
        
        if (flatFiles.size() > 1)
        {
            System.out.println("\n\n====================================================\nMulti-statement totals:");
            System.out.println("\tTotal statements: " + totalStatements);
            System.out.println("\tSpecial handling: " + totalSpecials);
            System.out.println("\tFinance charges: " + DecimalFormat.getCurrencyInstance().format(totalFinanceCharge));
            System.out.println("\tBudget interest: " + DecimalFormat.getCurrencyInstance().format(totalBudgetInterest));
            System.out.println("\tTotal Billed: " + DecimalFormat.getCurrencyInstance().format(totalBilled));
        }

        if (dropStatements.size() > 0)
        {
        	System.out.println("\n\n========================================================================\n"); // 101013
        	FlatFile dupFlatFile = flatFiles.get(0);
            List<DocumentRecord> remove = new LinkedList<DocumentRecord>();
            for (Integer item : dropStatements)
            {
                DocumentRecord stmt = dupFlatFile.FindDocument(item.intValue());
                if (stmt != null) {
                	System.out.println("Removing account " + item + " from file"); // 101013
                    remove.add(stmt);
                }    
                else
                    System.out.println("Remove failed for " + item + ": not found in file");
            }

            try
            {
                dupFlatFile.RemoveDocuments(remove);
            }
            catch (Exception e)
            {
                System.out.println("Error removing duplicates - missing duplicate");
            }
        }

        if (dropDivs.size() > 0)
        {
        	FlatFile ff = flatFiles.get(0);
        	List<DocumentRecord> remove = new LinkedList<DocumentRecord>();
        	for (DocumentRecord stmt : ff.Documents)
        	{
        		if (dropDivs.contains(stmt.Division))
        			remove.add(stmt);
        	}
            try
            {
                ff.RemoveDocuments(remove);
            }
            catch (Exception e)
            {
                System.out.println("Error removing divisions - missing duplicate");
            }
        }
        
        if (dropCategories.size() > 0)
        {
        	FlatFile ff = flatFiles.get(0);
        	List<DocumentRecord> remove = new LinkedList<DocumentRecord>();
        	for (DocumentRecord stmt : ff.Documents)
        	{
        		if (dropCategories.contains(stmt.CategoryCode))
        			remove.add(stmt);
        	}
            try
            {
                ff.RemoveDocuments(remove);
            }
            catch (Exception e)
            {
                System.out.println("Error removing categories - missing duplicate");
            }
        }

        if (dropBalanceBelow != null)
        {
        	FlatFile ff = flatFiles.get(0);
        	List<DocumentRecord> remove = new LinkedList<DocumentRecord>();
        	for (DocumentRecord stmt : ff.Documents)
        	{
        		if (stmt.getTotalDue().compareTo(dropBalanceBelow) <= 0)
        			remove.add(stmt);
        	}
            try
            {
                ff.RemoveDocuments(remove);
            }
            catch (Exception e)
            {
                System.out.println("Error removing categories - missing duplicate");
            }
        }

        if (combineFinanceChargeDate != null)
        {
            FlatFile ff = flatFiles.get(0);

            BigDecimal liFC = new BigDecimal(0);

            // Locate finance charge code
            int finance_charge_pc = 0;
            for (PostingCodeRecord pc : ff.PostingCodes.values())
            	if (pc.Description.compareTo("FINANCE CHARGE") == 0)
            	{
            		finance_charge_pc = pc.Code;
            		break;
            	}
            
            for (DocumentRecord doc : ff.Documents)
            {
            	StatementRecord stmt = (StatementRecord)doc;
            	
            	BigDecimal prevFinanceCharge = stmt.CreditInfoRecord.getFinanceCharge();
            	
            	BigDecimal removedAmt = stmt.CombineFinanceCharges(combineFinanceChargeDate, finance_charge_pc, ff);
            	if (removedAmt != null)
            	{
            		liFC = liFC.add(removedAmt);
            		System.out.println("Combined finance charge on Acct# " + stmt.AccountNum);
            		System.out.println("\tOrig FC: $" + prevFinanceCharge);
            		System.out.println("\tFC Li    $" + removedAmt);
            		System.out.println("\tNew FC:  $" + stmt.CreditInfoRecord.getFinanceCharge());
            	}
            }
            
            System.out.println("Line items combined total: " + liFC);
        }
        
        if (dropAfterDate != null || dropBeforeDate != null)
        {
            FlatFile ff = flatFiles.get(0);
            for (DocumentRecord stmt : ff.Documents)
            {
                Collection<Record> removeNewList = new LinkedList<Record>();
                Collection<Record> removeOldList = new LinkedList<Record>();
                boolean removedNewPrevious = false;
                boolean removedOldPrevious = false;
                for (Record r : stmt.Records)
                {
                    if (r instanceof LineItemRecord)
                    {
                    	removedNewPrevious = false;
                    	removedOldPrevious = false;

                    	LineItemRecord li = (LineItemRecord)r;
                        if (dropAfterDate != null  && li.TransDate.after (dropAfterDate))
                        {
                            removeNewList.add(r);
                            removedNewPrevious = true;
                        }
                        if (dropBeforeDate != null && li.TransDate.before(dropBeforeDate))
                        {
                        	// Only remove old paid off keyoff transactions
                        	if (stmt.OpenItemFlag != 'K' || !li.Keyoff || li.Dollars.floatValue() == 0.0)
                        	{
                        		removeOldList.add(r);
                        		removedOldPrevious = true;
                        	}
                        }
                    }
                    else if (removedNewPrevious == true &&
                            r.RecordType == RecordTypeEnum.LINE_ITEM_ADDR)
                        removeNewList.add(r);
                    else if (removedOldPrevious == true &&
                            r.RecordType == RecordTypeEnum.LINE_ITEM_ADDR)
                        removeOldList.add(r);
                }

                if (removeNewList.size() > 0)
                    ff.RemoveNewLineItems(stmt, removeNewList);
                if (removeOldList.size() > 0)
                    ff.RemoveOldLineItems(stmt, removeOldList);
            }
        }

        if (dropLoanToTripleP)
        {
        	FlatFile ff = flatFiles.get(0);
        	DropLoanToTriplePTrans.fixIt(ff);
        }
        
        Map<Integer, List<FlatFile>> dups = DuplicateFinder.FindDuplicates(flatFiles);
        if (removeDuplicates)
        {
            if (dups == null)
            {
                System.out.println("=================================================\n" +
                                   "Warning: Remove duplicates specified but no dupliates found");
            }
            else
            {
                FlatFile dupFlatFile = flatFiles.get(0);
                List<DocumentRecord> remove = new LinkedList<DocumentRecord>();
                for (Map.Entry<Integer, List<FlatFile>> dup : dups.entrySet())
                {
                    if (dup.getValue().contains(dupFlatFile))
                        remove.add(dupFlatFile.FindDocument(dup.getKey().intValue()));
                }
                try
                {
                    dupFlatFile.RemoveDocuments(remove);
                }
                catch (Exception e)
                {
                    System.out.println("Error removing duplicates - missing duplicate");
                }
            }
        }
        else if (dups != null)
        {
            System.out.println("=================================================\n" +
                               "Warning: " + dups.size() + " Duplicate statements in file: ");
            for (Map.Entry<Integer, List<FlatFile>> dup : dups.entrySet())
            {
                System.out.println("\tAcct #" + dup.getKey() + " in files: ");
                for (FlatFile ff : dup.getValue())
                {
                    System.out.println("\t\t" + ff.FileName);
                }
            }
            System.out.println("=================================================");
        }
        
        if (specialHandlingDivs.size() > 0) {
        	FlatFile dupFlatFile = flatFiles.get(0);
        	for (DocumentRecord stmt : dupFlatFile.Documents)
        	{
        		if (specialHandlingDivs.contains(stmt.Division))
        			stmt.setSpecialHandling(true);
        	}
           	
        }

        if (dumpFile != null)
        	try {
        		for (Record r : flatFiles.get(0).AllRecords)
        			dumpFile.write(r.dump() + "\n");
        		dumpFile.close();
        	}
        	catch (IOException e)
        	{
        		System.out.println("Error writing dump file: " + e);
        		System.exit(1);
        	}
    }
}
