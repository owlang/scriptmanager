package window_interface.Read_Analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFrame;

import util.ArrayUtilities;
import scripts.Read_Analysis.AggregateData;

@SuppressWarnings("serial")
public class AggregateDataOutput extends JFrame {
	
	private ArrayList<File> INPUT = null;
	private File OUT_PATH = null;
	private boolean MERGE = true;
	private int ROWSTART = 1;
	private int COLSTART = 1;
	private int METRIC = 0;
	private PrintStream OUT;
	
	public AggregateDataOutput(ArrayList<File> in, File out, boolean m, int r, int c, int index) {
		INPUT = in;
		OUT_PATH = out;
		MERGE = m;
		ROWSTART = r;
		COLSTART = c;
		METRIC = index;
	}
	
	public void run() throws IOException {
		if(!MERGE) {
			for(int x = 0; x < INPUT.size(); x++) {
				outputFileScore(INPUT.get(x));
		        firePropertyChange("file", x, x + 1);								
			}
		} else {
			ArrayList<ArrayList<Double>> MATRIX = new ArrayList<ArrayList<Double>>();
			ArrayList<ArrayList<String>> MATRIXID = new ArrayList<ArrayList<String>>();
			for(int x = 0; x < INPUT.size(); x++) {
				Scanner scan = new Scanner(INPUT.get(x));
				ArrayList<Double> scorearray = new ArrayList<Double>();
				ArrayList<String> idarray = new ArrayList<String>();
				int count = 0;
				while (scan.hasNextLine()) {
					String line = scan.nextLine(); // Line 0
					//Skip lines until desired row start
					while(count < ROWSTART) {
						line = scan.nextLine();
						count++;
					}
					
					//Split array, assume first element is ID, irrespective of column start
					String[] ID = line.split("\t");
					idarray.add(ID[0]);
					
					double[] numarray = new double[ID.length];
					for(int y = COLSTART - 1; y < ID.length; y++) {
						try { numarray[y] = Double.parseDouble(ID[y]); }
						catch(NumberFormatException nfe) { numarray[y] = Double.NaN; }
						
					}
					if(METRIC == 0) { scorearray.add(ArrayUtilities.getSum(numarray)); }
					else if(METRIC == 1) { scorearray.add(ArrayUtilities.getAverage(numarray)); }
					else if(METRIC == 2) { scorearray.add(ArrayUtilities.getMedian(numarray)); }
					else if(METRIC == 3) { scorearray.add(ArrayUtilities.getMode(numarray)); }
					else if(METRIC == 4) { scorearray.add(ArrayUtilities.getMin(numarray)); }
					else if(METRIC == 5) { scorearray.add(ArrayUtilities.getMax(numarray)); }
					else if(METRIC == 6) { scorearray.add(ArrayUtilities.getPositionalVariance(numarray)); }
					count++;
	
				}
				scan.close();
				MATRIX.add(scorearray);
				MATRIXID.add(idarray);
			}

			String name = "ALL_SCORES.out";
			if(OUT_PATH != null) { OUT = new PrintStream(new File(OUT_PATH.getCanonicalPath() + File.separator + name)); }
			else { OUT = new PrintStream(new File(name)); }

			//Check all arrays are the same size
			int ARRAYLENGTH = MATRIX.get(0).size();
			boolean ALLSAME = true;
			for(int x = 0; x < MATRIX.size(); x++) {
				if(MATRIX.get(x).size() != ARRAYLENGTH || MATRIXID.get(x).size() != ARRAYLENGTH) {
					ALLSAME = false;
					OUT.println("Different number of rows between:\n" + INPUT.get(0).getName() + "\n" + INPUT.get(x).getName());
				}
			}
			
			if(ALLSAME) {
				for(int x = 0; x < INPUT.size(); x++) { OUT.print("\t" + INPUT.get(x).getName()); }
				OUT.println();
				for(int x = 0; x < MATRIX.get(0).size(); x++) {
					OUT.print(MATRIXID.get(0).get(x));
					for(int y = 0; y < MATRIX.size(); y++) { OUT.print("\t" + MATRIX.get(y).get(x)); }
					OUT.println();
				}
			}
			OUT.close();
		}
	}

	public void outputFileScore(File IN) throws FileNotFoundException, IOException {
		String[] name = IN.getName().split("\\.");
		String NEWNAME = "";
		for(int x = 0; x < name.length - 1; x++) {
			if(x == name.length - 2) { NEWNAME += (name[x]); }
			else { NEWNAME += (name[x] + "."); }
		}
		
		if(OUT_PATH != null) { OUT = new PrintStream(new File(OUT_PATH.getCanonicalPath() + File.separator + NEWNAME + "_SCORES.out"));
		} else { OUT = new PrintStream(new File(NEWNAME + "_SCORES.out")); }

		if(METRIC == 0) { OUT.println("\tSum"); }
		else if(METRIC == 1) { OUT.println("\tAverage"); }
		else if(METRIC == 2) { OUT.println("\tMedian"); }
		else if(METRIC == 3) { OUT.println("\tMode"); }
		else if(METRIC == 4) { OUT.println("\tMin"); }
		else if(METRIC == 5) { OUT.println("\tMax"); }
		else if(METRIC == 6) { OUT.println("\tPositionalVariance"); }
		
		Scanner scan = new Scanner(IN);
		int count = 0;
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			count++;
			
			//Skip lines until desired row start
			while(count < ROWSTART) {
				line = scan.nextLine();
				count++;
			}
			
			String[] ID = line.split("\t");
			double[] numarray = new double[ID.length];
			for(int x = COLSTART - 1; x < ID.length; x++) {
				try { numarray[x] = Double.parseDouble(ID[x]); }
				catch(NumberFormatException nfe) { numarray[x] = Double.NaN; }
				
			}
			if(METRIC == 0) { OUT.println(ID[0] + "\t" + ArrayUtilities.getSum(numarray)); }
			else if(METRIC == 1) { OUT.println(ID[0] + "\t" + ArrayUtilities.getAverage(numarray)); }
			else if(METRIC == 2) { OUT.println(ID[0] + "\t" + ArrayUtilities.getMedian(numarray)); }
			else if(METRIC == 3) { OUT.println(ID[0] + "\t" + ArrayUtilities.getMode(numarray)); }
			else if(METRIC == 4) { OUT.println(ID[0] + "\t" + ArrayUtilities.getMin(numarray)); }
			else if(METRIC == 5) { OUT.println(ID[0] + "\t" + ArrayUtilities.getMax(numarray)); }
			else if(METRIC == 6) { OUT.println(ID[0] + "\t" + ArrayUtilities.getPositionalVariance(numarray)); }
			count++;
		}
		scan.close();
		OUT.close();
	}
}