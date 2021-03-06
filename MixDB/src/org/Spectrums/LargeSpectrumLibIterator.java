package org.Spectrums;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * When spectrum lib is large we avoid loading all of them into memory at once
 * instead we read from the files
 * @author Jian Wang
 *
 */
public class LargeSpectrumLibIterator<T> implements Iterator<T>{
	private String spectrumFile;
	private Spectrum nextSpectrum;
	private BufferedReader buff;
	private boolean isProceed=true;
	private boolean hasNext = false;
	public LargeSpectrumLibIterator(String filename){
		this.spectrumFile = filename;
		try{
			BufferedReader buff = new BufferedReader(new FileReader(this.spectrumFile));
			this.buff = buff;
			this.nextSpectrum = new Spectrum();
		}catch(IOException ioe){
			System.out.println(ioe.getMessage());
			ioe.printStackTrace();
		}
	}
	
	public List<Spectrum> readAllSpectra(){
		List<Spectrum> specList = new ArrayList<Spectrum>();
		int counter = 0;
		while(this.hasNext()){
			Spectrum s = (Spectrum)this.next();
			s.scanNumber = counter++;
			specList.add(s);
		}
		System.out.println("read in total spectra: " + specList.size());
		return specList;
	}
	
	@Override
	public boolean hasNext() {
		if(!isProceed){
			return hasNext;
		}
		if(nextSpectrum.readSpectrumFromMGF(buff)){
		//if(nextSpectrum.readSpectrumFromMS2(buff)){
			this.hasNext = true;
			isProceed = false;
			return true;
		}else{
			this.hasNext = false;
			this.nextSpectrum = new Spectrum();
			isProceed = false;
			return false;
		}
	}

	@Override
	public T next() {
		if(this.hasNext()){
			isProceed = true;
			Spectrum ret = this.nextSpectrum;
			this.nextSpectrum = new Spectrum();
			return (T)ret;             //not very good way to do this
		}else{
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}
	
	//restart the iterator
	public void reStart(){
		try{
			BufferedReader buff = new BufferedReader(new FileReader(this.spectrumFile));
			this.buff = buff;
			this.nextSpectrum = new Spectrum();
		}catch(IOException ioe){
			System.out.println(ioe.getMessage());
			ioe.printStackTrace();
		}
	}

}
