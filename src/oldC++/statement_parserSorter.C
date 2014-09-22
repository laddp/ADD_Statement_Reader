#include <stdlib.h>
#include <unistd.h>
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
bool opt_f   = false;
bool opt_s   = false;
bool opt_i   = false;
bool opt_e   = false;
bool patricks_special_flag = false;
int  file_ver = 4;
string *opt_fa  = 0;
string *opt_fb  = 0;
extern set<const char *> includeFiles;
extern set<const char *> excludeFiles;

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
             << "   -d <f>   : dump statements to file <f>\n"
             << "   -s       : sort the records for the dump\n"
             << "   -fb mm/dd/yy : filter out before date\n"
             << "   -fa mm/dd/yy : filter out after date\n"
             << "   -f       : filter statements\n"
             << "   -p       : Patrick's special flag -- see the code :)\n"
            ;
        exit(1);
    }

    int arg = 0;
    bool done = false;
    while (!done)
    {
        arg +=1;
        if (arg >= argc)
        {
            cerr << "Error, no input files specified!\n";
            exit(2);
        }
       	if      (strcmp(argv[arg], "-a") == 0) { opt_a = true; }
        else if (strcmp(argv[arg], "-n") == 0) { opt_n = true; }
        else if (strcmp(argv[arg], "-r") == 0) { opt_r = true; }
        else if (strcmp(argv[arg], "-f") == 0) { opt_f = true; }
        else if (strcmp(argv[arg], "-s") == 0) { opt_s = true; }
        else if (strcmp(argv[arg], "-p") == 0) { patricks_special_flag = true; }
        else if (strcmp(argv[arg], "-o") == 0) { file_ver--; }
        else if (strcmp(argv[arg], "--") == 0) { done = true; }
        else if (strcmp(argv[arg], "-fa") == 0)
        {
            opt_f = true;
            opt_fa = new string(argv[arg+1]);
            arg++;
        }
        else if (strcmp(argv[arg], "-fb") == 0)
        {
            opt_f = true;
            opt_fb = new string(argv[arg+1]);
            arg++;
        }
        else if (strcmp(argv[arg], "-i") == 0)
        {
            opt_i = true;
            opt_f = true; // implied
            includeFiles.insert(argv[arg+1]);
            arg++;
        }
        else if (strcmp(argv[arg], "-e") == 0)
        {
            opt_e = true;
            opt_f = true; // implied
            excludeFiles.insert(argv[arg+1]);
            arg++;
        }
        else if (strcmp(argv[arg], "-d") == 0)
        {
            opt_d = true;
            char *dumpFileName = argv[arg+1];
            if (access(dumpFileName, F_OK) == 0)
            {
                cerr << "Error: dump file already exists!\n";
                exit(2);
            }
            dumpFile.open(dumpFileName, ios::binary|ios::trunc|ios::out);
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
    locale olde = cerr.imbue(my_loc); // imbue modified locale onto cerr

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
