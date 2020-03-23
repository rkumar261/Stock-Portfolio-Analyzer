
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException, StockQuoteServiceException {

    List<Candle> candle = new ArrayList<Candle>();
    ObjectMapper objectMapper = getObjectMapper();

    try {
      String url = buildUri(symbol);
      String result = restTemplate.getForObject(url, String.class);
      AlphavantageDailyResponse collection = objectMapper.readValue(result, 
          AlphavantageDailyResponse.class);
      for (Map.Entry<LocalDate, AlphavantageCandle> entry : collection.getCandles().entrySet()) {
        LocalDate date = entry.getKey();
        entry.getValue().setDate(date);
        if ((date.isEqual(to) || date.isEqual(from)) || (date.isBefore(to) && date.isAfter(from))) {
          candle.add(entry.getValue());
        }
      }
    } catch (JsonProcessingException e) {
      throw new StockQuoteServiceException(e.getMessage(), e.getCause());
    } catch (NullPointerException e) {
      throw new StockQuoteServiceException(e.getMessage(), e.getCause());
    } catch (RuntimeException e) {
      throw new StockQuoteServiceException(e.getMessage(), e.getCause());
    }
    
    Collections.reverse(candle);
    return candle;
  }

  protected String buildUri(String symbol) {
    String demo = "B1O4W8AOHM6BMYI6";
    String uriTemplate = String.format(
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=%s&outputsize=full&apikey=%s",
        symbol, demo);
    return uriTemplate;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // new
  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  // Update the method signature to match the signature change in the interface.
  // Start throwing new StockQuoteServiceException when you get some invalid
  // response from
  // Alphavangate, or you encounter a runtime exception during Json parsing.
  // Make sure that the exception propagates all the way from PortfolioManager,
  // so that the external user's of our API are able to explicitly handle this
  // exception upfront.
  // CHECKSTYLE:OFF

}
