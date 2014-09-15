/*
 * Created on Jun 14, 2010 by pladd
 *
 */
package com.bottinifuel.StatementReader;

import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.Records.LineItemRecord;
import com.bottinifuel.FlatFileParser.Records.Record;
import com.bottinifuel.FlatFileParser.Records.Record.RecordTypeEnum;

/**
 * @author pladd
 *
 */
public class FixLocations
{
    public static void fixIt(FlatFile s)
    {
        LineItemRecord groupLead = null;
        for (Record r : s.AllRecords)
        {
            if (r.RecordType == RecordTypeEnum.LINE_ITEM)
            {
                LineItemRecord li = (LineItemRecord)r;
                if (groupLead == null)
                    groupLead = li;
                else if (groupLead.RefNum.equals(li.RefNum) &&
                         groupLead.TransDate.equals(li.TransDate))
                {
                    if (li.getLocNum()  == 0 &&
                            li.getLocType() == ' ')
                    {
                        li.setLocNum (groupLead.getLocNum());
                        li.setLocType(groupLead.getLocType());
                    }
                }
                else
                    groupLead = li;
            }
        }
    }
}
