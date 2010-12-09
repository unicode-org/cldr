package org.unicode.cldr.unittest;

import com.ibm.icu.text.UCharacterIterator;

public class TestUCharacterIterator {
  public static void main(String[] args) {
    
  }
  
  /**
   * Test class that traverses HTML.
   * @author markdavis
   *
   */
  static class Utf8Iterator extends UCharacterIterator {
    char[] buffer;
    int[] sourceIndexToExternal;
    int[] externalIndexToSource;
    
    @Override
    public int current() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getLength() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getIndex() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int next() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int previous() {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void setIndex(int index) {
    }

    @Override
    public int getText(char[] fillIn, int offset) {
      // TODO Auto-generated method stub
      return 0;
    }
    
  }
}