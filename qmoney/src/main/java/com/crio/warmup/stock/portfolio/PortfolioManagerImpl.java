
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {
  private StockQuotesService stockQuotesService;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  public PortfolioManagerImpl(RestTemplate restTemplate) {
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
    return stockQuotesService.getStockQuote(symbol, from, to);
  }

  // extra add
  public List<AnnualizedReturn> calculateAnnualizedReturn(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException, NullPointerException {

    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    try {
      for (int i = 0; i < portfolioTrades.size(); i++) {
        List<Candle> candle = getStockQuote((portfolioTrades.get(i)).getSymbol(),
            (portfolioTrades.get(i)).getPurchaseDate(), endDate);
        Double buyPrice = (candle.get(0)).getOpen();
        Double sellPrice = (candle.get(candle.size() - 1)).getClose();
        Double totalReturn = (sellPrice - buyPrice) / buyPrice;
        long daysBetween = ChronoUnit.DAYS.between(
            portfolioTrades.get(i).getPurchaseDate(), endDate);
        Double annualizedReturn = Math.pow((1 + totalReturn), (365.0 / daysBetween)) - 1;
        annualizedReturns.add(new AnnualizedReturn(portfolioTrades.get(i).getSymbol(), 
            annualizedReturn, totalReturn));
      }
    } catch (StockQuoteServiceException e) {
      throw new StockQuoteServiceException(e.getMessage());
    } catch (JsonProcessingException e) {
      throw new StockQuoteServiceException(e.getMessage(), e.getCause());
    } catch (RuntimeException e) {
      throw new StockQuoteServiceException(e.getMessage(), e.getCause());
    }

    Collections.sort(annualizedReturns, getComparator());
    return annualizedReturns;
  }
}