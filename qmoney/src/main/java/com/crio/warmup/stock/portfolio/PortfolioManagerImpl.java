
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
      throws JsonProcessingException, StockQuoteServiceException {

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

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, 
      StockQuoteServiceException, ExecutionException {

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    List<Future<List<Candle>>> futureList = new ArrayList<Future<List<Candle>>>();
    
    for (int i = 0; i < portfolioTrades.size(); i++) {
      Future<List<Candle>> future = executor.submit(new Task(stockQuotesService, 
          (portfolioTrades.get(i)).getSymbol(), 
          (portfolioTrades.get(i)).getPurchaseDate(), endDate));
      futureList.add(future);
    }
    int i = 0;

    for (Future<List<Candle>> fut : futureList) {
      List<Candle> candle = fut.get();
      Double buyPrice = (candle.get(0)).getOpen();
      Double sellPrice = (candle.get(candle.size() - 1)).getClose();
      Double totalReturn = (sellPrice - buyPrice) / buyPrice;
      long daysBetween = ChronoUnit.DAYS.between(
          portfolioTrades.get(i).getPurchaseDate(), endDate);
      Double annualizedReturn = Math.pow((1 + totalReturn), (365.0 / daysBetween)) - 1;
      annualizedReturns.add(new AnnualizedReturn(portfolioTrades.get(i).getSymbol(), 
          annualizedReturn, totalReturn));
      i++;
    }
    
    executor.shutdown();
    Collections.sort(annualizedReturns, getComparator());
    return annualizedReturns;
  }
  
  public static class Task implements Callable<List<Candle>> {
    private StockQuotesService stockQuotesService;
    private String symbol;
    private LocalDate from;
    private LocalDate to;
    
    public Task(StockQuotesService stockQuotesService, 
        String symbol, LocalDate from, LocalDate to) {
      this.stockQuotesService = stockQuotesService;
      this.symbol = symbol;
      this.from = from;
      this.to = to;
    }  
  
    @Override
    public List<Candle> call() throws Exception {
      return stockQuotesService.getStockQuote(symbol, from, to);
    }
  }
}

