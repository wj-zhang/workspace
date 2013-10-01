package org.Spectrums;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.systemsbiology.jrap.stax.MSXMLParser;
import org.systemsbiology.jrap.stax.Scan;

/**
 * This class holds a series of utility static method to compute various statistics for 
 * SWATH spectra
 * @author Jian Wang
 *
 */
public class SWATHStatistics {
	//test if all peaks matched but intensity flat, distribution of projcos
	public static void testProjCosine(){
		String libraryFile = "../mixture_linked/NIST_human_QTOF_2010-03-02.mgf";
		LargeSpectrumLibIterator it = new LargeSpectrumLibIterator(libraryFile);
		//SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		//Spectrum s1 = reader.getSpectrum(1631);
		//System.out.println("lib size: " + lib.getSpectrumList().size());
		//List<Spectrum> specList = lib.getSpectrumList();
		while(it.hasNext()){
			Spectrum s1 = (Spectrum)it.next();
				s1.windowFilterPeaks(6, 25);
			//s1.filterPeaks(30);
			s1.mergePeaks(s1, 0.05);
			s1.sqrtSpectrum();
			double libInt = s1.magnitude();
			libInt = libInt*libInt;
			Spectrum s2 = new Spectrum(s1);
			double projcos = s1.projectedCosine(s2, 0.05);
			double projcos2 = s2.projectedCosine(s1, 0.05);
			//double projcos = s1.cosineSim(s2);
			for(int j = 0; j < s2.getPeak().size(); j++){
				s2.getPeak().get(j).setIntensity(0.5); //fixed intensity
			}
			double projcosflat = s1.projectedCosine(s2, 0.05);
			//double projcos2 = s1.cosineSim(s2);
			System.out.println("matching: " + s1.spectrumName + "\t" + s1.parentMass + "\t" + s1.charge  + "\t" + s1.peptide
					+"\tnumPeaks:\t" + s1.getPeak().size()
					+ "\tcosine:\t" + projcos + "\t" + projcos2 + "\tflat:\t" + projcosflat + "\t" + libInt);

		}
	}
	
	
	public static void testProjCosineOnSpec(){
		String libraryFile = "../mixture_linked/human_heck_1pepFDR_allPSM.mgf";
		String libraryFile2 = "../mixture_linked/msdata/EIF/14164_EIF4A2_no_exclusion_TOF56k_P94.mzXML";
		LargeSpectrumLibIterator it = new LargeSpectrumLibIterator(libraryFile);
		MZXMLReader reader = new MZXMLReader(libraryFile2);
		//SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		//Spectrum s1 = reader.getSpectrum(1631);
		//System.out.println("lib size: " + lib.getSpectrumList().size());
		List<Spectrum> specList = new ArrayList();
		Spectrum prev = new Spectrum(); //dummy
		int counter = 0;
		DecoySpectrumGenerator d = new DecoySpectrumGenerator();
		while(it.hasNext()){
			Spectrum s = (Spectrum)it.next();
			//ss.filterPeaksByIntensity(100);
			//s.computePeaksZScore(0.5);
			//s.filterPeaksByRankScore(2);
			int ScanNum = Integer.parseInt(s.spectrumName.split("\\s+")[2]);
			//Spectrum s0 = reader.getSpectrum(ScanNum);
			//s.score = s0.score;
			//s.upperBound = s0.upperBound;
			double tolerance = 0.5;
			//s.windowFilterPeaks(6, 25);
			//s.mergePeaks(s, tolerance*1.5);
			//s.sqrtSpectrum();
			counter++;
			s.scanNumber = counter;
			if(s.peptide.equals(prev.peptide) && s.charge == prev.charge){
				specList.add(s);
				prev = s;
			}else{
				Spectrum represent = null;
				double maxInt = 0.0;
				for(int i = 0; i < specList.size(); i++){
					Spectrum s1 = specList.get(i);
					double totalInt = s1.magnitude();
					represent = totalInt > maxInt ? s1 : represent;
					maxInt = totalInt > maxInt ? totalInt : maxInt;
				}
				if(specList.size()  < 5){
					specList.clear();
				}
				
				//note we have to compute signals first before normalizing the intensity for computing cosine
				for(int i = 0; i < specList.size(); i++){
					Spectrum s1 = specList.get(i);
					//s1.windowFilterPeaks2(15, 25);
					//s1.filterPeaksByIntensity(0.015);
					double signals1 = SpectrumUtil.getSNR(s1, tolerance);
					s1.upperBound = signals1;
					s1.windowFilterPeaks(6, 50);
					s1.mergePeaks(s1, tolerance);
					s1.sqrtSpectrum();
					s1.removePrecursors(0.5);
					//s1.filterPeaks(10);
				}
				
				for(int i = 0; i < specList.size(); i++){
					Spectrum s1 = specList.get(i);
					double libInt1 = s1.magnitude();
					libInt1 = libInt1*libInt1;
					double signals1 = s1.upperBound;
					double normalizedSig1 = signals1 /(s1.parentMass*s1.charge/100);
					double mean=0.0;
					double mean2 = 0.0;
					double meanShare = 0.0;
					for(int j = 0; j < specList.size(); j++){
						//Spectrum s1 = represent;
						Spectrum s2 = specList.get(j);
						if(s1 == s2){
							continue;
						}
						double libInt2 = s2.magnitude();
						libInt2 = libInt2*libInt2;
						double signals2 = s2.upperBound;
						double normalizedSig2 = signals2 /(s2.parentMass*s2.charge/100);
						//s1.filterPeaks(30);
						//s2.filterPeaks(30);
						d.tolerance = tolerance;
						//Spectrum libSpect = d.generateTarget(s1);
						Spectrum libSpect = s1;
						Spectrum s3 = new Spectrum(libSpect);
						double projcos = libSpect.projectedCosine(s2, tolerance);
						double projcos2 = s2.projectedCosine(libSpect, tolerance);
						double cosine = libSpect.cosine(s2, tolerance);
						for(int p = 0; p < s3.getPeak().size(); p++){
							s3.getPeak().get(p).setIntensity(0.5); //fixed intensity
						}
						double projcosFlat = s3.projectedCosine(s2, tolerance);
						double cosFlat = s3.cosine(s2, tolerance);
						double projcosFlat2 = s3.projectedCosine(libSpect, tolerance);
						double shared = libSpect.sharePeaks(s2, tolerance); 
						mean += projcos;
						mean2+= cosine;
						meanShare += shared;
						//double projcos2 = s1.cosineSim(s2);
						
						System.out.println("matching: " + libSpect.spectrumName + "\t" + s2.spectrumName + "\t"
						+ libSpect.scanNumber + "\t" + s2.scanNumber + "\t"		
						+ libSpect.parentMass + "\t" + libSpect.charge  + "\t" + libSpect.peptide
						+ "\tnumPeaks:\t" + libSpect.getPeak().size() + "\t" + s2.getPeak().size()
								+ "\tcosine:\t" + projcos + "\t" + projcos2 + "\t" + cosine +"\t" + shared
								+"\tflat:\t" + projcosFlat + "\t" + projcosFlat2 + "\t" + cosFlat + "\t" + libInt1 + "\t" + libInt2
								+"\t" + signals1 + "\t" + signals2 +"\t" + normalizedSig1 + "\t" + normalizedSig2);
					}
					mean = mean / (specList.size() - 1);
					mean2 = mean2 / (specList.size() - 1);
					meanShare = meanShare / (specList.size() - 1);
					
					System.out.println("matching: " + s1.spectrumName + "\t" + 
							+ s1.scanNumber + "\t" 	+ s1.parentMass + "\t" + s1.charge  + "\t" + s1.peptide
							+ "\tnumPeaks:\t" + s1.getPeak().size() + "\t"	+ "\tmeanCosine:\t"  + mean 
							+ "\t" + mean2 +"\t" + "\t" + meanShare + "\t" 
							+ libInt1 + "\t" + signals1 + "\t" + normalizedSig1
							+"\t" + s.score + "\t" + s.upperBound);
				}
				specList.clear();
				specList.add(s);
				prev = s;
			}
		}
	}
	
	public static void tofSpecStat(){
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_EIF4a2_no_exlcusion_IDA_msgfdbID.mgf";
		LargeSpectrumLibIterator it = new LargeSpectrumLibIterator(libraryFile);
		//SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		//Spectrum s1 = reader.getSpectrum(1631);
		//System.out.println("lib size: " + lib.getSpectrumList().size());
		List<Spectrum> specList = new ArrayList();
		Spectrum prev = new Spectrum(); //dummy
		int counter = 0;
		DecoySpectrumGenerator d = new DecoySpectrumGenerator();
		while(it.hasNext()){
			Spectrum s = (Spectrum)it.next();
			//ss.filterPeaksByIntensity(100);
			//s.computePeaksZScore(0.5);
			//s.filterPeaksByRankScore(2);
			double tolerance = 0.05;
			//s.windowFilterPeaks(6, 25);
			s.mergePeaks(s, tolerance*1.5);
			s.sqrtSpectrum();
			counter++;
			//s.scanNumber = counter;
			if(s.peptide.equals(prev.peptide) && s.charge == prev.charge){
				specList.add(s);
				prev = s;
			}else{
				Spectrum represent = null;
				double minInt=1000000, maxInt = 0, meanInt = 0; 
				for(int i = 0; i < specList.size(); i++){
					Spectrum s1 = specList.get(i);
					double totalInt = s1.magnitude();
					totalInt = totalInt*totalInt;
					represent = totalInt > maxInt ? s1 : represent;
					minInt = totalInt < minInt ? totalInt : minInt;
					maxInt = totalInt > maxInt ? totalInt : maxInt;
					meanInt += totalInt;
				}
			
				double minSig=10000, maxSig=0, meanSigs=0;
				for(int i = 0; i < specList.size(); i++){
						Spectrum s1 = specList.get(i);
						double signals = SpectrumUtil.getSNR(s1, tolerance);
						meanSigs += signals;
						minSig = signals < minSig ? signals : minSig;
						maxSig = signals > maxSig ? signals : maxSig;
				}
				
				int minScan = 1000000, maxScan = 0; 
				for(int i = 0; i < specList.size(); i++){
					Spectrum s1 = specList.get(i);
					int scanNumber = Integer.parseInt((s1.spectrumName.split("\\s+")[2]));
					minScan = scanNumber < minScan ? scanNumber : minScan;
					maxScan = scanNumber > maxScan ? scanNumber : maxScan;
				}
				if(specList.size() > 0){
					meanInt = meanInt / specList.size();
					meanSigs = meanSigs/specList.size();
					Spectrum s1 = specList.get(0);
					System.out.println(s1.peptide + "\t" + s1.charge + "\t" + s1.spectrumName 
							+ "\tsignal-distrib\t" + minSig + "\t" + maxSig + "\t" + meanSigs
							+ "\tspectrumInt-distrib\t" + minInt + "\t" + maxInt + "\t" + meanInt
							+"\tset-size: " + specList.size() +"\tscan-range\t" + minScan + "\t" + maxScan);
				}
				specList.clear();
				specList.add(s);
				prev = s;
			}
		}
	}
	
	
	public static void testProjCosineOnSpecDecoy(){
		String libraryFile = "../mixture_linked/human_heck_1pepFDR_allPSM.mgf";
		String libraryFile2 = "../mixture_linked/human_heck_1pepFDR_msgfdb_DecoyOnly.mgf";
		LargeSpectrumLibIterator it = new LargeSpectrumLibIterator(libraryFile);
		SpectrumLib lib = new SpectrumLib(libraryFile2, "MGF");
		lib.windowFilterPeaks(6, 50);
		double massTolerance = 0.5;
		lib.mergeSpectrum(massTolerance);
		lib.normIntensity();
		while(it.hasNext()){
			Spectrum s = (Spectrum)it.next();
			double signals = SpectrumUtil.getSNR(s, massTolerance);
			s.windowFilterPeaks(6, 50);
			s.mergePeaks(s, massTolerance);
			s.sqrtSpectrum();
			List<Spectrum> specList = lib.getAllSpectrums();
			double max = -1000, maxProj1 = -1000, maxProj2 = -1000;
			Spectrum best1=null, best2=null, best3=null;
			for(int i = 0; i < specList.size(); i++){
				Spectrum libEntry = specList.get(i);
				if(Math.abs(s.parentMass - libEntry.parentMass) < 5){
					double proj1 = s.projectedCosine(libEntry, massTolerance);
					double proj2 = libEntry.projectedCosine(s, massTolerance);
					double cosScore = s.cosine(libEntry, massTolerance);
					best1 = proj1 > maxProj1 ? libEntry : best1;
					best2 = proj2 > maxProj2 ? libEntry: best2;
					best3 = cosScore  > max ? libEntry : best3;
					maxProj1 = proj1 > maxProj1 ? proj1 : maxProj1;
					maxProj2 = proj2 > maxProj2 ? proj2 : maxProj2;
					max = cosScore > max ? cosScore : max;
				}
			}
			double normalizedSig1 = signals /(s.parentMass*s.charge/100);
			System.out.println(s.spectrumName + "\t" + 
					+ s.scanNumber + "\t" 	+ s.parentMass + "\t" + s.charge  + "\t" + s.peptide
					+ "\tnumPeaks:\t" + s.getPeak().size() + "\t"	+ "\tbestCosine:\t"  + maxProj1 
					+ "\t" + maxProj2 +"\t" + max + "\t" 
					+  signals + "\t" + normalizedSig1 + "\t" + s.score + "\t" + s.upperBound);
		}
	}
	
	public static void getSWATHFiltering(){
		String queryFile = "../mixture_linked/msdata/UPS_Ecoli/14342_UPS1-400fm_SWATH_5600.mzXML";
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps_plusDecoy2_test.mgf";
		String resultFile = "../mixture_linked/t00";
		MZXMLReader reader = new MZXMLReader(queryFile);
		//ConsensusSpectrumReader reader = new ConsensusSpectrumReader(queryFile);
		//reader.numNeighbors = 0;
		//reader.minInt = 0;
		SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		List<String> results = Utils.FileIOUtils.createListFromFile(resultFile);
		for(int i = 0; i < results.size(); i++){
			String[] tokens = results.get(i).split("\\s+");
			if(tokens.length < 6  || lib.getSpectra(tokens[4] + "." + tokens[6]) == null){
				continue; //skipping non-formatted lines
			}
			String pepKey = tokens[4] + "." + tokens[6];
			System.out.println(pepKey);
			Spectrum libEntry = lib.getSpectra(pepKey).get(0);
			int swathScan = Integer.parseInt(tokens[1]);
			Spectrum swathSpec = reader.getSpectrum(swathScan);
			//System.out.println(swathSpec);
			libEntry.windowFilterPeaks2(6, 25);
			//swathSpec.filterPeaksByIntensity(50);
			//swathSpec.toRelIntensity();
			//swathSpec.filterPeaksByIntensity(0.03);
			swathSpec.filterPeaks(1000);
			//swathSpec.windowFilterPeaks2(15, 25);
			//swathSpec.filterPeaks(700);
			swathSpec.computePeaksZScore(0.5);
			//swathSpec.filterPeaksByRankScore(300);
			double[] shared = swathSpec.projectedShare(libEntry, 0.05, 220);
			System.out.println("ZPeakStat\t"+tokens[4]+"@"+tokens[6] + "\t" +  tokens[8] + "\t" + tokens[1] + "\t" +  tokens[7] 
			         + "\t" + swathSpec.getPeak().size() +"\t"
					+ libEntry.getPeak().size() + "\t"+ shared[0] +"\t" + shared[1]);
		}
	}
	
	
	public static void getSWATHInterference(){
		String queryFile = "../mixture_linked/msdata/UPS_Ecoli/14344_UPS1_400fm_Ecolilysate_SWATH_5600.mzXML";
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps_plusDecoy2_test2.mgf";
		String ssmResultFile = "../mixture_linked/ACG_swathdevelopment_14344_Swath_SSMs.txt";
		String locationFile = "../mixture_linked/t00";
		MZXMLReader reader = new MZXMLReader(queryFile);
		//ConsensusSpectrumReader reader = new ConsensusSpectrumReader(queryFile);
		//reader.numNeighbors = 0;
		//reader.minInt = 0;
		SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		List<String> ssmResults = Utils.FileIOUtils.createListFromFile(ssmResultFile);
		List<String> results = Utils.FileIOUtils.createListFromFile(locationFile);
		Map<Integer, List<String>> ssmMap = new HashMap<Integer, List<String>>();
		double minSim = 0.82;
		double minPeaks = 11;		
		for(int i = 0; i < ssmResults.size(); i++){
			String[] tokens = ssmResults.get(i).split("\\t");
			int scan = Integer.parseInt(tokens[1]);
			if(!ssmMap.containsKey(scan)){
				ssmMap.put(scan, new ArrayList<String>());
			}
			List<String> matches = ssmMap.get(scan);
			matches.add(tokens[4] + "." + tokens[6]);
		}
		
		Set<Integer> keys = ssmMap.keySet();
		for(Iterator<Integer> it = keys.iterator(); it.hasNext();){
			int swathScan = it.next();
			Spectrum swathSpec = reader.getSpectrum(swathScan);
			//System.out.println(swathSpec);
			swathSpec.windowFilterPeaks2(15, 25);
			//swathSpec.filterPeaks(700);	
			List<String> matches = ssmMap.get(swathScan);
			if(matches == null){
				continue;
			}
			if(matches.size() <= 1){
				continue;
			}
			System.out.println("mixture matches: " + matches.size());
			for(int j = 0; j < matches.size(); j++){
				Spectrum sameSwath = new Spectrum();
				String pepKey1 = matches.get(j);
				Spectrum libSpect = lib.getSpectra(pepKey1).get(0);
				libSpect.mergePeaks(libSpect, 0.05);
				libSpect.filterPeaks(30);
				for(int i = 0; i < matches.size(); i++){
					if(i != j){
						String pepKey2 = matches.get(i);
						System.out.println("key " + pepKey2);
						if(lib.getSpectra(pepKey2) != null){
							Spectrum match = lib.getSpectra(pepKey2).get(0);
							match.mergePeaks(match, 0.05);
							match.filterPeaks(30);
							sameSwath.getPeak().addAll(match.getPeak());
						}
					}
				}
				Collections.sort(sameSwath.getPeak(), PeakMassComparator.comparator);
				Spectrum proj = swathSpec.project(sameSwath, 0.05);
				proj.sqrtSpectrum();
				libSpect.sqrtSpectrum();
				double share = sameSwath.sharePeaks(libSpect, 0.05);
				double score = libSpect.projectedCosine(proj, 0.05);
				double score2 = libSpect.projectedCosine(swathSpec, 0.05);
				System.out.println("Shared-stat:\t" + swathScan + "\t" + libSpect.peptide + "\tShared-Stat\t" +  + share + "\t" + score +"\t" + score2);					
			}
		}
	}
	
	//we observe low intensity peaks in SWATH and in general in the 5600 seem to have low variabilty
	//i.e. fixed intensity value, want to test to what extended this is true 
	public static void testSWATHIntensityVar(){
		String spectrumFile = "../mixture_linked/msdata/gringar/swath_development/14344_UPS1_400fm_Ecolilysate_SWATH_5600.mzXML";
		//String spectrumFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps_plusDecoy2_test.mgf";
		MZXMLReader reader = new MZXMLReader(spectrumFile);
		//LargeSpectrumLibIterator reader = new LargeSpectrumLibIterator(spectrumFile);
		int Bins = 501;
		double[] IntRange = new double[Bins];
		long[] count = new long[Bins];
		double[] means = new double[Bins];
		double[] variances = new double[Bins];
		double minInt = 0;
		double stepInt = 1;
		for(int i = 0; i < IntRange.length; i++){
			IntRange[i] = minInt+i*stepInt;
		}
		IntRange[Bins-1] = Double.MAX_VALUE;
		while(reader.hasNext()){
			Spectrum s = (Spectrum)reader.next();
			//if(true) continue;
			//s.windowFilterPeaks2(15, 25);
			s.filterPeaksByIntensity(30);
			s.computePeaksZScore(0.5);
			List<Peak> pList = s.getPeak();
			for(int i = 0; i < pList.size(); i++){
				Peak p = pList.get(i);
				int index = ArrayUtils.getIntervalIndex(p.getIntensity(), IntRange);
				if(index < IntRange.length){
					if(count[index] > 0){	
						means[index]=(count[index]/(double)(count[index]+1))*means[index] + p.getIntensity()/(count[index]+1);
					}else{
						means[index]+= p.getIntensity();
					}
					count[index]+=1;
				}
			}
			if(s.scanNumber > 70000){
				break;
			}
		}
		reader = new MZXMLReader(spectrumFile);
		//reader = new LargeSpectrumLibIterator(spectrumFile);
		//computing var
		while(reader.hasNext()){
			Spectrum s = (Spectrum)reader.next();
			List<Peak> pList = s.getPeak();
			for(int i = 0; i < pList.size(); i++){
				Peak p = pList.get(i);
				int index = ArrayUtils.getIntervalIndex(p.getIntensity(), IntRange);
				if(index < IntRange.length){
					double dev = p.getIntensity() - means[index];
					variances[index] += dev*dev / count[index];
				}
			}
			if(s.scanNumber > 70000){
				break;
			}

		}
		
		for(int i = 0; i < means.length; i++){
			System.out.println(minInt+i*stepInt + "\t" + means[i] + "\t" + variances[i] +"\t" + count[i]);
		}
		
	}
	
	
	public static void testSWATHFiltering(){
		String queryFile = "../mixture_linked/msdata/UPS_Ecoli/14342_UPS1-400fm_SWATH_5600.mzXML";
		MZXMLReader reader = new MZXMLReader(queryFile);
		Spectrum s = reader.getSpectrum(37521);
		s.computePeaksZScore(0.5);
		//s.filterPeaksByRankScore(300);
		System.out.println(s);
		while(reader.hasNext()){
			//Spectrum s = reader.next();
			//if(s.scanNumber < 20000){
			//	continue;
			//}
			//s.toRelIntensity();
			//s.computePeaksZScore(0.9);
		}
		
		
	}
	
	
	
	
	//checking the varaition of masses
	public static void testSWATHMassVar(){
		String spectrumFile = "../mixture_linked/msdata/gringar/swath_development/14344_UPS1_400fm_Ecolilysate_SWATH_5600.mzXML";
		//String spectrumFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps_plusDecoy2_test.mgf";
		MZXMLReader reader = new MZXMLReader(spectrumFile);
		//LargeSpectrumLibIterator reader = new LargeSpectrumLibIterator(spectrumFile);
		double maxMass = 2000;
		double resolution = 0.05;
		int Bins = 40001;
		double[] MassRange = new double[Bins];
		long[] count = new long[Bins];
		double[] means = new double[Bins];
		double[] variances = new double[Bins];
		double minInt = 0;
		double stepInt = resolution;
		for(int i = 0; i < MassRange.length; i++){
			MassRange[i] = minInt+i*stepInt;
		}
		MassRange[Bins-1] = Double.MAX_VALUE;
		while(reader.hasNext()){
			Spectrum s = (Spectrum)reader.next();
			s.windowFilterPeaks2(15, 25);
			//s.filterPeaksByIntensity(30);
			//s.computePeaksZScore(0.5);
			List<Peak> pList = s.getPeak();
			for(int i = 0; i < pList.size(); i++){
				Peak p = pList.get(i);
				double pmass = p.getMass()*0.9995;
				int index = (int)(pmass / resolution);
				if(pmass > maxMass){
					index = MassRange.length-1;
				}
				if(index < MassRange.length){
					if(count[index] > 0){	
						means[index]=(count[index]/(double)(count[index]+1))*means[index] + p.getIntensity()/(count[index]+1);
					}else{
						means[index]+= pmass;
					}
					count[index]+=1;
				}
			}
			if(s.scanNumber > 70000){
				break;
			}
		}
		reader = new MZXMLReader(spectrumFile);
		//reader = new LargeSpectrumLibIterator(spectrumFile);
		//computing var
		while(reader.hasNext()){
			Spectrum s = (Spectrum)reader.next();
			List<Peak> pList = s.getPeak();
			for(int i = 0; i < pList.size(); i++){
				Peak p = pList.get(i);
				double pmass = p.getMass()*0.9995;
				int index = (int)(pmass / resolution);
				if(index < MassRange.length){
					double dev = p.getIntensity() - means[index];
					variances[index] += dev*dev / count[index];
				}
			}
			if(s.scanNumber > 70000){
				break;
			}

		}
		
		for(int i = 0; i < means.length; i++){
			System.out.println(minInt+i*stepInt + "\t" + means[i] + "\t" + variances[i] +"\t" + count[i]);
		}
		
	}
	
	public static void getLibraryStat(){
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps_plusDecoy2_test.mgf";
		SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		lib.windowFilterPeaks(6, 25);
		lib.normIntensity();
		List<Spectrum> specList = lib.getSpectrumList();
		List<double[]> scores = new ArrayList<double[]>();
		for(int rank = 1; rank < 100; rank ++){
			double[] projCosines = new double[specList.size()];
			for(int i = 0; i < specList.size(); i++){
				Spectrum entry = specList.get(i);
				entry.mergePeaks(entry, 0.05);
				Spectrum copy = new Spectrum(entry);
				copy.filterPeaks(rank);
				System.out.println("number peaks: " + copy.getPeak().size());
				projCosines[i] = entry.projectedCosine(copy, 0.05);
				if(projCosines[i] > 0.85){
					//System.out.println(entry);
				}
				System.out.println("rank: " + rank + "\tprojcos: " + projCosines[i]);
				
			}
		}
	}
	
	
	public static void getLibraryStat2(){
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_UPSEcoli_IDA_PSMs.mgf";
		LargeSpectrumLibIterator iter = new LargeSpectrumLibIterator(libraryFile);
		while(iter.hasNext()){
			Spectrum s = (Spectrum)iter.next();
			s.filterPeaksByIntensity(150);
			s.windowFilterPeaks(6, 25);
			System.out.println("Number of peaks " + s.getPeak().size());
		}
	}
	
	
	public static void getLibraryStat3(){
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps_plusDecoy2_test.mgf";
		SpectrumLib lib = new SpectrumLib(libraryFile, "MGF");
		lib.windowFilterPeaks(6, 25);
		lib.normIntensity();
		List<Spectrum> specList = lib.getSpectrumList();
		List<double[]> scores = new ArrayList<double[]>();
		for(int i = 0; i < specList.size(); i++){
			Spectrum entry = specList.get(i);
			entry.mergePeaks(entry, 0.05);
			for(int j = i+1; j < specList.size(); j++){	
				Spectrum entry2 = specList.get(j);
				entry2.mergePeaks(entry2, 0.05);
				//System.out.println("number peaks: " + copy.getPeak().size());
				if(Math.abs(entry.parentMass - entry2.parentMass) < 25){
					double score = entry.projectedCosine(entry2, 0.05);
					double score2 = entry2.projectedCosine(entry, 0.05);
					double scoreFull = entry.cosine(entry2, 0.05);
					double share = entry.sharePeaks(entry2, 0.05);
					if(scoreFull > 0.5 && share > 10){
						System.out.println(entry.peptide +"@" + entry.charge + "\t" + entry2.peptide + "@" + entry2.charge 
								+ "\t" + entry.spectrumName + "\t" + entry2.spectrumName + "\t" 
								+ score + "\t" + score2 + "\t" + scoreFull);
					}
				}
			}
		}
	}
	
	public static void getS2NR(){
		String libraryFile = "../mixture_linked/ACG_swathdevelopment_P94_UPS_Ecoli_MSGFDB_IDs_uniqpeps.mgf";
		LargeSpectrumLibIterator iter = new LargeSpectrumLibIterator(libraryFile);
		while(iter.hasNext()){
			Spectrum s = (Spectrum)iter.next();
			if(!s.peptide.equals("AAPSFGGTTGTLQDAFDLEALK")){
				//continue;
			}
			//s.toRelIntensity();
			s.computePeaksZScore(0.5);
			//System.out.println(s);
			int minZ = 200;
			int signalCount = 0;
			for(int i = 0; i < s.getPeak().size(); i++){
				Peak p = s.getPeak().get(i);
				if(p.getRank() > minZ){
					signalCount++;
				}
			}
			System.out.println(s.spectrumName + "\t" + s.peptide + "@" + s.charge  + "\tNumber of signal peaks:\t" + signalCount);
		}
	}
	
	
	public static void testDeIsotop(){
		String queryFile = "../mixture_linked./msdata/UPS_Ecoli/1ugEcoli_400fmol_UPS2_swath_2012-12-11.mzXML";
		MZXMLReader reader = new MZXMLReader(queryFile);
		int minScan = 30000;
		int maxScan = 31000;
		for(int i = minScan; i < maxScan; i++){
			Spectrum swath = reader.getSpectrum(i);
			swath.deIsoPeaks(swath, 0.03);
			System.out.println(swath);
		}
				
	}
	
	//we perform a histogram on peak intensity in order to estimate signal to noise ratio
	public static int[] histPeakInt(Spectrum s, int numBins){
		SpectrumMap map = new SpectrumMap(s);
		double maxInt = map.intensityMap.lastKey();
		int[] peakCounts = new int[numBins];
		double interval = maxInt / numBins;
		System.out.println("interval: " + interval);
		System.out.println("total peaks:\t" + s.getPeak().size());
		double[] intervals = new double[numBins+1];
		for(int i = 1; i < intervals.length; i++){
			intervals[i] = interval*i;
		}
		for(int i = 0; i < numBins; i++){
			peakCounts[i] = map.intensityMap.subMap(intervals[i], intervals[i+1]).size();
		}
		
		int maxCount = 0;
		int maxInd = 0;
		for(int i = 0; i < peakCounts.length; i++){
			maxInd = peakCounts[i] > maxCount ? i : maxInd;
			maxCount = peakCounts[i] > maxCount ? peakCounts[i] : maxCount;
			System.out.println("bin " + intervals[i] +"\t" + peakCounts[i]);
		}
		SortedMap sub =  map.intensityMap.subMap(intervals[maxInd], intervals[maxInd+1]);
		List<Double> intensitties = new ArrayList();
		intensitties.addAll(sub.keySet());
		double binAvgInt = intensitties.get(((int)(intensitties.size()*0.5)));
		double bin25Percent = intensitties.get(((int)(intensitties.size()*0.25)));
		double bin75Percent = intensitties.get(((int)(intensitties.size()*0.75)));
		System.out.println("max peak at bin: " + maxInd + "\t" + maxCount + "\t" 
				+ binAvgInt +"\t" + bin25Percent + "\t" + bin75Percent);
		return peakCounts;
	}
	
	public static void getRTDifferences(){
		String resultFile = "../mixture_linked/t1"; 
		String resultFile2 = "../mixture_linked/ACG_swathdevelopment_14341_msgfdb_1pepFDR.txt";
		String swathFile = "../mixture_linked/msdata/UPS_Ecoli/14342_UPS1-400fm_SWATH_5600.mzXML";
		String idaFile = "../mixture_linked/msdata/UPS_Ecoli/14341_UPS1-400fm_IDA_5600.mzXML";
		List<String> results  = Utils.FileIOUtils.createListFromFile(resultFile);
		List<String> results2 = Utils.FileIOUtils.createListFromFile(resultFile2);
		Map<String, Integer> idMap = new HashMap();
		MZXMLReader reader = new MZXMLReader(swathFile);
		MZXMLReader reader2 = new MZXMLReader(idaFile);
		MSXMLParser parser = reader.getParser();
		MSXMLParser parser2 = reader2.getParser();
		for(int i = 0; i < results2.size(); i++){
			String[] tokens = results2.get(i).split("\\t");
			String peptide = tokens[7].substring(2, tokens[7].length()-2);
			//System.out.println("putting peptide " + tokens[7] + "\t" + peptide);
			idMap.put(peptide+"@"+tokens[6], Integer.parseInt(tokens[1]));
		}
		for(int i = 0; i < results.size(); i++){
			String[] tokens = results.get(i).split("\\t");
			Scan s = parser.rap(Integer.parseInt(tokens[1]));
			double rt1 = SWATHUtils.getRT(s);
			System.out.println("Getting peptide: " + tokens[4]);
			Integer idaScanNum = idMap.get(tokens[4] +"@"+tokens[6]);
			if(idaScanNum == null){
				continue;
			}
			Scan s2 = parser2.rap(idaScanNum);
			double rt2 = SWATHUtils.getRT(s2);
			System.out.println(tokens[4] + "\t" + tokens[6] + "\tRT-difference:\t" + (rt1-rt2));
		}
		
	}
	
	public static void testHistInt(){
		String spectrumFile = "../mixture_linked./msdata/UPS_Ecoli/14342_UPS1-400fm_SWATH_5600.mzXML";
		MZXMLReader reader = new MZXMLReader(spectrumFile);
		Spectrum s = reader.getSpectrum(27514	);
		//s.windowFilterPeaks2(15, 25);
		s.filterPeaks(1000);
		//s.toRelIntensity();
		//s.filterPeaksByIntensity(0.01);
		s.computePeaksZScore(0.5);
		//histPeakInt(s, 1000);
		//System.out.println(s);
	}
	
	public static void main(String[] args){
		//simpleCosTest();
		//testSWATHIntensityVar();
		//getSWATHFiltering();
		getSWATHInterference();
		//testProjCosine();
		//testProjCosineOnSpec();
		//tofSpecStat();
		//testProjCosineOnSpecDecoy();
		//testSWATHMassVar();
		//testSWATHFiltering();
		//getLibraryStat();
		//getLibraryStat2();
		//getLibraryStat3();
		//getS2NR();
		//testDeIsotop();
		//testHistInt();
		//getRTDifferences();
	}

}