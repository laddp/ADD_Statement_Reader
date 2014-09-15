/**
 * 
 */
package com.bottinifuel.StatementReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.bottinifuel.FlatFileParser.FileFormatException;
import com.bottinifuel.FlatFileParser.FlatFile;
import com.bottinifuel.FlatFileParser.FlatFile.FileTypeEnum;
import com.bottinifuel.FlatFileParser.Records.Record;
import com.bottinifuel.FlatFileParser.Records.StatementRecord;
import com.bottinifuel.FlatFileParser.Records.Record.RecordTypeEnum;

/**
 * @author laddp
 *
 */
public class StatementCombine {

	/**
	 * @param args
	 * 
	 * Combine two statement files together - place the document with more transactions in the output file
	 */
	public static void main(String[] args) {
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

				if (ff1.Documents.size() != ff2.Documents.size())
				{
					System.out.println("Document counts are different... this won't work...");
					return;
				}

		        FileWriter dumpFile = new FileWriter(args[2]);

		        for (Record r : ff1.AllRecords)
		        {
		        	if (r.RecordType == RecordTypeEnum.STATEMENT)
		        		break;
		        	dumpFile.write(r.dump() + "\n");
		        }
		        
				for (int i = 0; i < ff1.Documents.size(); i++)
				{
					StatementRecord d1 = (StatementRecord)ff1.Documents.get(i);
					StatementRecord d2 = (StatementRecord)ff2.Documents.get(i);

					if (d1.AccountNum != d2.AccountNum)
					{
						System.out.println("!!!!!!!!!!!!!!!!!!!! Mismatch in account numbers! Line #" + i);
						dumpFile.close();
						return;
					}

					if (d1.Records.size() >= d2.Records.size())
					{
						dumpFile.write(d1.dump() + "\n");
						for (Record r : d1.Records)
							dumpFile.write(r.dump() + "\n");
					}
					else if (d1.Records.size() < d2.Records.size())
					{
						dumpFile.write(d2.dump() + "\n");
						for (Record r : d2.Records)
							dumpFile.write(r.dump() + "\n");
					}
				}

				dumpFile.write(ff1.Trailer.dump() + "\n");
				dumpFile.close();
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
	}
}
