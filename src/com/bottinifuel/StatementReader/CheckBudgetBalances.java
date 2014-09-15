package com.bottinifuel.StatementReader;

import java.math.BigDecimal;

import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.Records.DocumentRecord;
import com.bottinifuel.FlatFileParser.Records.StatementRecord;

public class CheckBudgetBalances {

	static public String CheckBudgetBalancesInFile(FlatFile ff)
	{
		if (ff.FileType != FlatFile.FileTypeEnum.STATEMENT_FILE)
		{
			return "Can't check budget balances - not a statement file!";
		}
		
		String rc = "\tAcct#\tCat\tBal\tTotDue\tBgtAmt\tNonBgt\tBgtPDue\tBgtPPay\n";
		
		for (DocumentRecord doc : ff.Documents)
		{
			StatementRecord stmt = (StatementRecord)doc;
			if (!stmt.StatementType.contains("B") && !stmt.StatementType.equals("SS"))
				continue;

			String acct = Integer.toString(stmt.AccountNum);
			
			rc += "\t";
			
			String err = "";
			if (stmt.StatementType.contains("B"))
			{
				if (stmt.getTotalDue().equals(
						stmt.NonBudgetCharges.
						add(stmt.BudgetPayment.
								add(stmt.PastDueBudgetAmount.
										subtract(stmt.PrepayBudgetCredit)))))
					;
				else if (stmt.getTotalDue().compareTo(BigDecimal.ZERO) == 0 &&
						stmt.PrepayBudgetCredit.compareTo(stmt.BudgetPayment.add(stmt.PastDueBudgetAmount)) >= 0)
				{
				}
				else
				{
					err = "*";
				}
			}
			else
			{
				if (stmt.getCurrentBalance().compareTo(BigDecimal.ZERO) >= 0 &&
					stmt.getCurrentBalance().compareTo(stmt.getTotalDue()) != 0)
					err = "*";
			}

			rc += String.format("%8s ", err + acct);
			
			rc +=  "" +
				   stmt.CategoryCode + '\t' +
				   stmt.getCurrentBalance() + '\t' +
				   stmt.getTotalDue() + '\t' +
				   stmt.BudgetPayment + '\t' +
				   stmt.NonBudgetCharges + '\t' +
				   stmt.PastDueBudgetAmount + '\t' +
				   stmt.PrepayBudgetCredit + '\n';
		}
		
		return rc;
	}
}
