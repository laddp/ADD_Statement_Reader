#include <stdlib.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <sstream>
#include <locale>
using namespace std;

#include "RecordFactory.h"
#include "pml_moneypunct.h"

bool opt_a   = false;
bool opt_n   = false;
bool opt_r   = false;
bool opt_d   = false;
int  opt_old = 0;

ofstream dumpFile;

int main(int argc, char *argv[])
{
    if (argc < 2)
    {
        cerr << "Error: Missing arguments\n"
             << "Usage: " << argv[0] << " [-anor] [-d {dump file}] [--] {statement file(s)}\n"
             << "   -a       : Print out account numbers in file\n"
             << "   -n       : Print out accounts that please-pay doesn't match balance\n"
             << "   -o       : old format statement\n"
             << "   -r       : print out each record type\n"
             << "   -d <f>   : dump statements with credit balance to file <f>\n"
            ;
        exit(1);
    }

    int arg = 0;
    bool done = false;
    while (!done)
    {
        arg +=1;
		if      (strcmp(argv[arg], "-a") == 0) { opt_a = true; }
        else if (strcmp(argv[arg], "-n") == 0) { opt_n = true; }
        else if (strcmp(argv[arg], "-r") == 0) { opt_r = true; }
        else if (strcmp(argv[arg], "-o") == 0) { opt_old++; }
        else if (strcmp(argv[arg], "--") == 0) { done = true; }
        else if (strcmp(argv[arg], "-d") == 0)
        {
        	opt_d = true;
        	dumpFile.open(argv[arg+1], ios::binary|ios::trunc|ios::out);
        	if (!dumpFile)
        	{
        		cerr << "Error opening dump file \"" << argv[arg+1] << "\"\n";
        		exit(1);
        	}
        	arg++;
        }
        else { done = true; }
    }

    cerr.flags(ios_base::fixed|ios_base::showpoint);
    cout.flags(ios_base::fixed|ios_base::showpoint);
    cout << setprecision(2);
    cerr << setprecision(2);

	locale loc; // standard locale
	static pml_moneypunct pml("");
	static locale my_loc(loc, &pml); // modify locale with my currency format
	locale old = cout.imbue(my_loc); // imbue modified locale onto cout

	RecordFactory &rf = RecordFactory::getFactory();

    for (int i = arg; i < argc; i++)
    {
        ifstream stmtfile(argv[i], ios::binary);
        if (!stmtfile)
        {
            cerr << "Error: Unable to open file \"" << argv[i] << "\", skipping...\n";
        }

        int rc = rf.parseStatements(stmtfile, argv[i], opt_a, opt_r);
        if (rc)
        {
            cerr << "Error: parse error in statement file \"" << argv[i] << "\", continuing\n";
        }
    }

	if (opt_d) dumpFile.close();

	rf.CheckForDups();
	rf.PrintInfo();

    return 0;
}
