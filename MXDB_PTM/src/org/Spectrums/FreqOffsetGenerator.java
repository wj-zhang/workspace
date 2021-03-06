package org.Spectrums;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

/**
 * Generate frequencey offset statistics for a set of annotated spectrum
 * @author Jian Wang
 *
 */
public class FreqOffsetGenerator {
	
	public static void generateOffSetTable(double min, double max, double interval){
		int length = (int)Math.floor((max-min)/interval)+1;
		System.out.println("length is: " + length);
		String[] genericPrefix = new String[length];
		String[] genericSuffix = new String[length];
		String[] genericIonType = new String[2*length];
		HashMap<String, Double> freqTable = new HashMap();
		for(int i = 0; i < length; i++){
			Double value = min+i*interval;
			String key = value.toString();
			genericPrefix[i] = "b"+key;
			genericSuffix[i] = "y"+key;
			genericIonType[i] = genericPrefix[i];
			genericIonType[i+length] = genericSuffix[i];
			freqTable.put(genericPrefix[i], value);
			freqTable.put(genericSuffix[i], value);
		}
		Mass.modMap = freqTable;
		Mass.standardIonsType = genericIonType;
		Mass.standardPrefixes = genericPrefix;
		Mass.standardSuffixes = genericSuffix;
		String[] empty = new String[]{};
		TheoreticalSpectrum.prefixIons = genericPrefix;
		TheoreticalSpectrum.suffixIons = genericSuffix;
	}
	
	public static void getFreqOffSet(String spectrumFile){
		generateOffSetTable(-200, 200, 1);
		SpectrumComparator scorer2 = SpectrumUtil.getLinkedPeptideScorer(spectrumFile);
		//SpectrumComparator scorer1 = SpectrumUtil.getRankBaseScorer(spectrumFile);
	}
	
	//we first removed all the explained peaks and then perform freq offset table 
	public static void getUnExplainedFreqOffSet(String spectrumFile){
		SpectrumLib lib = new SpectrumLib(spectrumFile, "MGF");
		for(Iterator<Spectrum> it = lib.getAllSpectrums().iterator(); it.hasNext();){
			Spectrum current = it.next();
			String[] peptides = current.peptide.split("--");
			TheoreticalSpectrum.prefixIons = Mass.standardPrefixes;
			TheoreticalSpectrum.suffixIons = Mass.standardSuffixes;
			int pos1 = peptides[0].indexOf('C')-1;
			int pos2 = peptides[1].indexOf('C')-1;
			LinkedPeptide lp = new LinkedPeptide(current.peptide, current.charge, pos1, pos2);
			//TheoreticalSpectrum linkedSpect = new TheoreticalSpectrum(lp.peptides[0], lp.peptides[1], lp.getCharge(), false);
			current.windowFilterPeaks(8, 25);
			current.computePeakRank();
			current.filterPeaks(100);
			//SpectrumUtil.removeAnnotatedPeaks(current, linkedSpect, 0.5);
			System.out.println("number of peaks remained: " + current.getPeak().size());
		}
		generateOffSetTable(-70, 70, 1);
		LinkedPeptidePeakScoreLearner.offSetFreq = true;
		SpectrumComparator scorer2 = SpectrumUtil.getLinkedPeptideScorer(lib);
		
	}
	
	
	
	public static void main(String[] args){
		//getFreqOffSet("..\\mixture_linked\\lib_sumo3_spectra_long_single_peptide_search.mgf");
		getUnExplainedFreqOffSet("..\\mixture_linked\\lib_disulfide1_mxdbIDs.mgf");
		//getFreqOffSet("..\\MSPLib\\Lib\\yeast.msp");
	}
	
}
