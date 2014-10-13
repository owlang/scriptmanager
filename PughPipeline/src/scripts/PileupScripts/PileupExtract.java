package scripts.PileupScripts;

import java.io.File;
import java.util.Vector;

import objects.BEDCoord;
import objects.PileupParameters;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.util.CloseableIterator;

public class PileupExtract implements Runnable{
	PileupParameters param;
	File BAM;
	Vector<BEDCoord> INPUT;
	int index;
	int subsetsize;
	SAMFileReader inputSam;
		
	public void run() {
		inputSam = new SAMFileReader(BAM, new File(BAM + ".bai"));
		for(int x = index; x < index + subsetsize; x++) {
			extract(INPUT.get(x));		
		}
		inputSam.close();
	}
	
	public PileupExtract(PileupParameters p, File b, Vector<BEDCoord> i, int current, int length) {
		param = p;
		BAM = b;
		INPUT = i;
		index = current;
		subsetsize = length;
	}
	
	public void extract(BEDCoord read) {
		double[] TAG_S1 = null;
		double[] TAG_S2 = null;
		int SHIFT = param.getShift();
		int STRAND = param.getStrand();
			
		//Correct Window Size for proper transformations
		int WINDOW = (read.getStop() - read.getStart()) + ((param.getBin() / 2) * 2);
		int QUERYWINDOW = 0;
		if(param.getTrans() == 1) {
			WINDOW = (read.getStop() - read.getStart()) + (param.getBin() * param.getSmooth() * 2);
			QUERYWINDOW = (param.getBin() * param.getSmooth()); 
		}
		else if(param.getTrans() == 2) {
			WINDOW = (read.getStop() - read.getStart()) + (param.getBin() * param.getStdSize() * param.getStdNum() * 2);
			QUERYWINDOW = (param.getBin() * param.getStdSize() * param.getStdNum()); 
		}
		TAG_S1 = new double[WINDOW];
		if(STRAND == 0) TAG_S2 = new double[WINDOW];
			
		//SAMRecords are 1-based
		CloseableIterator<SAMRecord> iter = inputSam.query(read.getChrom(), read.getStart() - QUERYWINDOW - SHIFT - 1, read.getStop() + QUERYWINDOW + SHIFT + 1, false);
		while (iter.hasNext()) {
			//Create the record object 
			SAMRecord sr = iter.next();
			
			//Must be PAIRED-END mapped, mate must be mapped, must be read1
			if(sr.getReadPairedFlag()) {
				if(sr.getProperPairFlag()) {
					if((sr.getFirstOfPairFlag() && param.getRead() == 0) || (!sr.getFirstOfPairFlag() && param.getRead() == 1) || param.getRead() == 2) {
						int FivePrime = sr.getUnclippedStart() - 1;
						if(sr.getReadNegativeStrandFlag()) { 
							FivePrime = sr.getUnclippedEnd();
							//SHIFT DATA HERE IF NECCESSARY
							FivePrime -= SHIFT;
						} else { FivePrime += SHIFT; }
						FivePrime -= (read.getStart() - QUERYWINDOW);
						
	                       //Increment Final Array keeping track of pileup
						if(FivePrime >= 0 && FivePrime < TAG_S1.length) {
							if(STRAND == 0) {
								if(!sr.getReadNegativeStrandFlag() && read.getDir().equals("-")) { TAG_S2[FivePrime] += 1; }
					        	else if(sr.getReadNegativeStrandFlag() && read.getDir().equals("+")) { TAG_S2[FivePrime] += 1; }
					        	else if(!sr.getReadNegativeStrandFlag() && read.getDir().equals("+")) { TAG_S1[FivePrime] += 1; }
					        	else if(sr.getReadNegativeStrandFlag() && read.getDir().equals("-")) { TAG_S1[FivePrime] += 1;}
							} else {
								TAG_S1[FivePrime] += 1;
							}
						}
					}
				}
			} else if(param.getRead() == 0 || param.getRead() == 2) {
				//Also outputs if not paired-end since by default it is read-1
				int FivePrime = sr.getUnclippedStart() - 1;
				if(sr.getReadNegativeStrandFlag()) { 
					FivePrime = sr.getUnclippedEnd();
					//SHIFT DATA HERE IF NECCESSARY
					FivePrime -= SHIFT;
				} else { FivePrime += SHIFT; }
				FivePrime -= (read.getStart() - QUERYWINDOW);
				
				//Increment Final Array keeping track of pileup
				if(FivePrime >= 0 && FivePrime < TAG_S1.length) {
					if(STRAND == 0) {
						if(!sr.getReadNegativeStrandFlag() && read.getDir().equals("-")) { TAG_S2[FivePrime] += 1; }
						else if(sr.getReadNegativeStrandFlag() && read.getDir().equals("+")) { TAG_S2[FivePrime] += 1; }
						else if(!sr.getReadNegativeStrandFlag() && read.getDir().equals("+")) { TAG_S1[FivePrime] += 1; }
						else if(sr.getReadNegativeStrandFlag() && read.getDir().equals("-")) { TAG_S1[FivePrime] += 1;}
					} else {
						TAG_S1[FivePrime] += 1;
					}
				}
			}
			if(read.getDir().equals("-")) {
				TAG_S1 = TransformArray.reverseTran(TAG_S1);
				TAG_S2 = TransformArray.reverseTran(TAG_S2);
			}
		}
		iter.close();
		
		//Perform Binning here
		double[] binF = new double[TAG_S1.length];
		double[] binR = null;
		if(TAG_S2 != null) binR = new double[TAG_S2.length]; 
		
		for(int j = 0; j < TAG_S1.length; j++) {
			for(int k = j - (param.getBin() / 2); k <= j + (param.getBin() / 2); k++) {
				if(k < 0) k = 0;
				if(k >= TAG_S1.length) k = j + (param.getBin() / 2) + 1;
				else  {
					binF[k] += TAG_S1[j];
					if(binR != null) binR[k] += TAG_S2[j];
				}
			}
		}
		double[] finalF = new double[TAG_S1.length / param.getBin()];
		double[] finalR = null;
		if(TAG_S2 != null) finalR = new double[TAG_S2.length / param.getBin()];
		
		for(int x = (param.getBin() / 2); x < TAG_S1.length - (param.getBin() / 2); x += param.getBin()) {
			finalF[(x - (param.getBin() / 2)) / param.getBin()] = TAG_S1[x];
			if(TAG_S2 != null) finalR[(x - (param.getBin() / 2)) / param.getBin()] = TAG_S2[x];
		}
		
		//Perform Tag Standardization Here
		if(param.getStandard()) {
			for(int i = 0; i < finalF.length; i++) {
				if(finalF != null) finalF[i] /= param.getRatio();
				if(finalR != null) finalR[i] /= param.getRatio();
			}
		}
		
		read.setFstrand(finalF);
		read.setRstrand(finalR);
	}
}