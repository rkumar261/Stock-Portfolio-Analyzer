
package com.crio.warmup.stock.dto;

import java.util.Comparator;

public class TotalReturnsDto {

  private String symbol;
  private Double closingPrice;

  public TotalReturnsDto(String symbol, Double closingPrice) {
    this.symbol = symbol;
    this.closingPrice = closingPrice;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public Double getClosingPrice() {
    return closingPrice;
  }

  public void setClosingPrice(Double closingPrice) {
    this.closingPrice = closingPrice;
  }

  public static final Comparator<TotalReturnsDto> ageComparator = 
      new Comparator<TotalReturnsDto>() {         
        
    @Override         
    public int compare(TotalReturnsDto jc1, TotalReturnsDto jc2) {             
      return (jc2.getClosingPrice() > jc1.getClosingPrice() ? -1
              : (jc2.getClosingPrice().equals(jc1.getClosingPrice()) ? 0 : 1));
    }     
  };
}
