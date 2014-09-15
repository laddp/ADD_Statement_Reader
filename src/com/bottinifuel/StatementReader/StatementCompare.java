/**
 * 
 */
package com.bottinifuel.StatementReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.bottinifuel.FlatFileParser.FileFormatException;
import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.FlatFile.FileTypeEnum;
import com.bottinifuel.FlatFileParser.Records.Record;
import com.bottinifuel.FlatFileParser.Records.StatementRecord;

/**
 * @author laddp
 *
 */
public class StatementCompare {

	public static void CompareIgnoreStatementDateAndAssociatedFlags(String [] args)
	{
        try
        {
            FileReader f1r = new FileReader(args[0]);
            System.out.println("\n========================================================================\n" +
                               "Processing statement file " + args[0] + "\n");
            FlatFile ff1 = new FlatFile(FileTypeEnum.STATEMENT_FILE, args[0], f1r, System.out, 1, false);

            FileReader f2r = new FileReader(args[1]);
            System.out.println("\n\n========================================================================\n" +
                               "Processing statement file " + args[1] + "\n");
            FlatFile ff2 = new FlatFile(FileTypeEnum.STATEMENT_FILE, args[1], f2r, System.out, 1, false);
            
            @SuppressWarnings("unused")
			StatementRecord ff1_stmt = null;
            @SuppressWarnings("unused")
			StatementRecord ff2_stmt = null;
            
        	if (ff1.AllRecords.size() != ff2.AllRecords.size())
        		System.out.println("Different number of records... " +
        				ff1.AllRecords.size() + "/" + ff2.AllRecords.size() + 
        				" this isn't going to go well at some point.");
        	
        	int rmin = ff1.AllRecords.size() > ff2.AllRecords.size() ? ff2.AllRecords.size() : ff1.AllRecords.size(); 
            for (int i = 0; i < rmin; i++)
            {
            	int j;
            	if (i > 5600)
            		j = i + 5;
            	else
            		j = i;
            	Record r1 = ff1.AllRecords.get(i);
            	Record r2 = ff2.AllRecords.get(j);
            	
        		if (r1 instanceof StatementRecord)
        			ff1_stmt = (StatementRecord)r1;
        		if (r2 instanceof StatementRecord)
        			ff2_stmt = (StatementRecord)r2;
            	
            	String r1d = r1.dump();
            	String r2d = r2.dump();
            	
            	if (r1d.compareTo(r2d) != 0)
            	{
            		if (r1 instanceof StatementRecord && r2 instanceof StatementRecord)
            		{
            			String range1r1 = r1d.substring(0, 274);
            			String range1r2 = r2d.substring(0, 274);

            			String range2r1 = r1d.substring(275, 309);
            			String range2r2 = r2d.substring(275, 309);
            			
            			String range3r1 = r1d.substring(315, 358);
            			String range3r2 = r2d.substring(315, 358);
            			
            			String range4r1 = r1d.substring(368);
            			String range4r2 = r2d.substring(368);
            			@SuppressWarnings("unused")
						int foo;
						if (range1r1.equals(range1r2) &&
            				range2r1.equals(range2r2) &&
            				range3r1.equals(range3r2) &&
            				range4r1.equals(range4r2))
            				continue;
            			else
            				foo = 0;
            		}
            		System.out.println("Line " + i + " mismatch:\n\t" + r1d + "\n\t" + r2d);
            	}
            }
        }
        catch (FileNotFoundException e)
        {
            System.out.println(e);
            return;
        }
        catch (FileFormatException e)
        {
            return;
        }
        catch (IOException e)
        {
            return;
        }
	}
	
	public static void CompareLookForDifferentRecordCounts(String [] args)
	{
        try
        {
            FileReader f1r = new FileReader(args[0]);
            System.out.println("\n========================================================================\n" +
                               "Processing statement file " + args[0] + "\n");
            FlatFile ff1 = new FlatFile(FileTypeEnum.STATEMENT_FILE, args[0], f1r, System.out, 1, false);

            FileReader f2r = new FileReader(args[1]);
            System.out.println("\n\n========================================================================\n" +
                               "Processing statement file " + args[1] + "\n");
            FlatFile ff2 = new FlatFile(FileTypeEnum.STATEMENT_FILE, args[1], f2r, System.out, 1, false);
            
            int d1Bigger = 0;
            int d2Bigger = 0;
            int same     = 0;
            
            if (ff1.Documents.size() != ff2.Documents.size())
            	System.out.println("Document counts are different... this won't go well somewhere...");
            
            int max = (ff1.Documents.size() > ff2.Documents.size()) ? ff2.Documents.size() : ff1.Documents.size();
            for (int i = 0; i < max; i++)
            {
            	StatementRecord d1 = (StatementRecord)ff1.Documents.get(i);
            	StatementRecord d2 = (StatementRecord)ff2.Documents.get(i);
            	
            	if (d1.AccountNum != d2.AccountNum)
            	{
            		System.out.println("!!!!!!!!!!!!!!!!!!!! Mismatch in account numbers! Line #" + i);
            		continue;
            	}

            	boolean print = false;
            	if (d1.Records.size() > d2.Records.size())
            	{
            		d1Bigger++;
            		print = true;
            		for (int j = 0; i < d2.Records.size(); i++)
            		{
            			Record d1r = d1.Records.get(j);
            			Record d2r = d2.Records.get(j);
            			
            			String d1s = d1r.dump();
            			String d2s = d2r.dump();
            			
            			if (d1s.compareTo(d2s) != 0)
            				System.out.println("Line #" + i +
            						"\n\t" + d1s +
            						"\n\t" + d2s);
            		}
            		System.out.println("Extra record: D1: " + d1.Records.get(d1.Records.size()-1) +
            				"\n\t" + d1.Records.get(d1.Records.size()-1).dump());
            	}
            	else if (d1.Records.size() < d2.Records.size())
            	{
            		d2Bigger++;
            		print = true;
            		for (int j = 0; i < d1.Records.size(); i++)
            		{
            			Record d1r = d1.Records.get(j);
            			Record d2r = d2.Records.get(j);
            			
            			String d1s = d1r.dump();
            			String d2s = d2r.dump();
            			
            			if (d1s.compareTo(d2s) != 0)
            				System.out.println("Line #" + i +
            						"\n\t" + d1s +
            						"\n\t" + d2s);
            		}
            		System.out.println("Extra record: D2: " + d2.Records.get(d2.Records.size()-1) +
            				"\n\t" + d2.Records.get(d2.Records.size()-1).dump());
            	}
            	else
            	{
            		same++;
            		for (int j = 0; i < d1.Records.size(); i++)
            		{
            			Record d1r = d1.Records.get(j);
            			Record d2r = d2.Records.get(j);
            			
            			String d1s = d1r.dump();
            			String d2s = d2r.dump();
            			
            			if (d1s.compareTo(d2s) != 0)
            				System.out.println("Line #" + i +
            						"\n\t" + d1s +
            						"\n\t" + d2s);
            		}
            	}
            	
            	if (print)
            	{
            		if (Math.abs(d1.Records.size() - d2.Records.size()) > 1)
            			System.out.print("Difference > 1 - ");
            		System.out.println("Acct #" + d1.AccountNum + 
            				" D1 trans: " + d1.Records.size() +
            				" D2 trans: " + d2.Records.size());
            	}
        		if (d1.CreditInfoRecord.getFinanceCharge().compareTo(d2.CreditInfoRecord.getFinanceCharge()) != 0)
        			System.out.println("Acct #" + d1.AccountNum + 
            				" D1 fchg: " + d1.CreditInfoRecord.getFinanceCharge() +
            				" D2 fchg: " + d2.CreditInfoRecord.getFinanceCharge());
            }
            
            System.out.println("Same: " + same + " D1 bigger: " + d1Bigger + " D2 bigger: " + d2Bigger);
        }
        catch (FileNotFoundException e)
        {
            System.out.println(e);
            return;
        }
        catch (FileFormatException e)
        {
            return;
        }
        catch (IOException e)
        {
            return;
        }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		CompareIgnoreStatementDateAndAssociatedFlags(args);
		CompareLookForDifferentRecordCounts(args);
	}

}
