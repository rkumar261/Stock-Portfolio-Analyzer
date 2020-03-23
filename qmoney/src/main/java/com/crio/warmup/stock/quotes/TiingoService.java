
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.client.RestTemplate;


public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  // Now we will be separating communication with Tiingo from PortfolioManager.
  // Generate the functions as per the declarations in the interface and then
  // Move the code from PortfolioManagerImpl#getSTockQuotes inside newly created
  // method.
  // Run the tests using command below -
  // ./gradlew test --tests TiingoServiceTest and make sure it passes.

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  // Write a method to create appropriate url to call tiingo service.

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) 
      throws JsonProcessingException, StockQuoteServiceException {

    List<Candle> candle = new ArrayList<Candle>();
    ObjectMapper objectMapper = getObjectMapper();
    String url = buildUri(symbol, from, to);
    String result = restTemplate.getForObject(url, String.class);

    List<TiingoCandle> collection = objectMapper.readValue(result, 
        new TypeReference<ArrayList<TiingoCandle>>() {});

    candle.add(collection.get(0));
    candle.add(collection.get(collection.size() - 1));
    
    return candle;
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate = String.format(
        "https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=fec122050b37e7e417c05ce375212f00372f4897",
        symbol, startDate.toString(), endDate.toString());
    return uriTemplate;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }





  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  Update the method signature to match the signature change in the interface.
  //  Start throwing new StockQuoteServiceException when you get some invalid response from
  //  Tiingo, or if Tiingo returns empty results for whatever reason,
  //  or you encounter a runtime exception during Json parsing.
  //  Make sure that the exception propagates all the way from
  //  PortfolioManager#calculateAnnualisedReturns,
  //  so that the external user's of our API are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF


}
