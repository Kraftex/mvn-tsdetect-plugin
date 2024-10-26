package es.upm.grise.profundizacion.wc;

import java.io.BufferedReader;
import java.io.IOException;

public class Counter {
	
	int numberCharacters = 0;
	int numberLines = 0;
	int numberWords = 0;
	
	public Counter(BufferedReader br) throws IOException {

	    int character = br.read();
        int prev = ' ';

	    while (character != -1) {
	    	numberCharacters++;
	    	
	    	if((character==' '||character=='\t'||character=='\n') && prev!=' '&&prev!='\t'&&prev!='\n') {
	    		numberWords++;
	    	}

	    	if(character == '\n') { numberLines++; }

	    	prev = character;
	    	character = br.read();
	    }
	    
	    if (prev!=' ' && prev!='\t' && prev!='\n') {
			numberWords++;
	    }
		
	}

	public int getNumberCharacters() {
		return numberCharacters;
	}

	public int getNumberLines() {
		return numberLines;
	}
	
	public int getNumberWords() {
		return numberWords;
	}
	
}
