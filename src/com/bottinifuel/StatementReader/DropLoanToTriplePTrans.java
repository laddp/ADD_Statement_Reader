/*
 * Created on Jun 14, 2010 by pladd
 *
 */
package com.bottinifuel.StatementReader;

import java.math.BigDecimal;
import java.util.LinkedList;

import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.Records.DocumentRecord;
import com.bottinifuel.FlatFileParser.Records.LineItemRecord;
import com.bottinifuel.FlatFileParser.Records.Record;
import com.bottinifuel.FlatFileParser.Records.Record.RecordTypeEnum;

/**
 * @author pladd
 *
 */
public class DropLoanToTriplePTrans
{
    public static void fixIt(FlatFile ff)
    {
        for (DocumentRecord d: ff.Documents)
        {
        	BigDecimal dropTotal = new BigDecimal(0);
        	LinkedList<Record> toDrop = new LinkedList<Record>();

        	for (Record r : d.Records)
        		if (r.RecordType == RecordTypeEnum.LINE_ITEM)
        		{
        			LineItemRecord li = (LineItemRecord)r;
        			if (li.TransCode == 235)
        			{
        				dropTotal = dropTotal.add(li.Dollars);
        				toDrop.add(li);
        			}
        		}
        	
        	if (toDrop.size() > 0)
        		if (dropTotal.compareTo(BigDecimal.ZERO) == 0)
        			ff.RemoveNewLineItems(d, toDrop);
        		else
        			System.out.println("Non-offsetting TripleP item(s) in statement for " + d.AccountNum);
        }
    }
}
