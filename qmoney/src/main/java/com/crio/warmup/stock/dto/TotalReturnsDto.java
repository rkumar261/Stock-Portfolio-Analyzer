
package com.crio.warmup.stock.dto;

import java.util.Comparator;

public class TotalReturnsDto {

  private String symbol;
  private Double closingPrice;
  private Double openingPrice;

  public TotalReturnsDto(String symbol, Double closingPrice, Double openingPrice) {
    this.symbol = symbol;
    this.closingPrice = closingPrice;
    this.openingPrice = openingPrice;
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

  public Double getOpenPrice() {
    return openingPrice; 
  }

  
  public void setOpenPrice(Double openingPrice) {
    this.openingPrice = openingPrice;
  }

  public static final Comparator<TotalReturnsDto> closingPriceComparator = 
      new Comparator<TotalReturnsDto>() {         
        
    @Override         
    public int compare(TotalReturnsDto jc1, TotalReturnsDto jc2) {             
      return (jc2.getClosingPrice() > jc1.getClosingPrice() ? -1
              : (jc2.getClosingPrice().equals(jc1.getClosingPrice()) ? 0 : 1));
    }     
  };
}
