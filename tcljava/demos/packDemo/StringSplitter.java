/*
 * StringSplitter.java
 *
 * Copyright (c) 1998 Mo Dejong
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: StringSplitter.java,v 1.1 1999/05/08 05:08:04 dejong Exp $
 *
 */


/**
 * This class is just a single static method called
 * that can be used to split the contents of a string
 * based on a split char. The string is broken up into
 * elements and returned as an array of String objects.
 */


import java.util.Vector;


public class StringSplitter {

  public static String[] split(String in, char splitchar) {

    // first we copy the contents of the string into
    // an array for quick processing
    
    int i;
    
    // create an array that is as big as the input
    // str plus one for an extra split char
    
    int len = in.length();
    char[] str = new char[len + 1];
    in.getChars(0,len,str,0);
    str[len++] = splitchar;
    
    int wordstart = 0;
    
    // make a vector that will hold our elements
    Vector words = new Vector(2);
    
    /*
    for (i=0; i < len; i++) {
      System.out.println(str[i] + " : " + i );
    }
    */
    
    for (i=0; i < len; i++) {
      
      // compare this char to the split char
      // if they are the same the we need to
      // add the last word to the array
      
      if (str[i] == splitchar) {
	
	//System.out.println("split char found at " + i);
	
	if (wordstart <= (i-1)) {
	  words.addElement( new String(str, wordstart, i - wordstart) );
	}

	wordstart = i + 1;
	
      }
    }
    
    
    // create an array that is as big as the number
    // of elements in the vector and copy over and return
    
    String[] ret = new String[ words.size() ];
    words.copyInto(ret);  
    return ret;
  }
  




  //just for testing
  
  public static void main(String[] args) {
    
    int i;
    
    testsplit("hello there folks how are things");
    testsplit(" hello there folks how are things ");
    testsplit("  hello   there  folks how are  things");
    testsplit(" 1  2 3 4 5 6  7  8 9");
        
  }
  
  public static void testsplit(String split) {
    int i;
    
    System.out.println("Split str is \"" + split + "\"");
    
    String[] arr = split(split, ' ');

    for (i=0; i<arr.length; i++) {
      System.out.println( i + " : \"" + arr[i] + "\"");	
    }
    
  }
  
 
}
