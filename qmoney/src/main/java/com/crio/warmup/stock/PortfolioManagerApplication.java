
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  // TODO: CRIO_TASK_MODULE_REST_API
  // Copy the relavent code from #mainReadFile to parse the Json into
  // PortfolioTrade list.
  // Now That you have the list of PortfolioTrade already populated in module#1
  // For each stock symbol in the portfolio trades,
  // Call Tiingo api
  // (https://api.tiingo.com/tiingo/daily/<ticker>/prices?startDate=&endDate=&token=)
  // with
  // 1. ticker = symbol in portfolio_trade
  // 2. startDate = purchaseDate in portfolio_trade.
  // 3. endDate = args[1]
  // Use RestTemplate#getForObject in order to call the API,
  // and deserialize the results in List<Candle>
  // Note - You may have to register on Tiingo to get the api_token.
  // Please refer the the module documentation for the steps.
  // Find out the closing price of the stock on the end_date and
  // return the list of all symbols in ascending order by its close value on
  // endDate
  // Test the function using gradle commands below
  // ./gradlew run --args="trades.json 2020-01-01"
  // ./gradlew run --args="trades.json 2019-07-01"
  // ./gradlew run --args="trades.json 2019-12-03"
  // And make sure that its printing correct results.

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portFolioTrade = portFolioData(new String[] { args[0] });
    ArrayList<TotalReturnsDto> totalReturnsDtos = new ArrayList<TotalReturnsDto>();
    RestTemplate restTemplate = new RestTemplate();

    for (int i = 0; i < portFolioTrade.length; i++) {

      String url = String.format(
          "https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=fec122050b37e7e417c05ce375212f00372f4897",
          portFolioTrade[i].getSymbol(), portFolioTrade[i].getPurchaseDate(), args[1]);

      String result = restTemplate.getForObject(url, String.class);
      List<TiingoCandle> collection = objectMapper.readValue(result, 
          new TypeReference<ArrayList<TiingoCandle>>() {});

      listOfTradeObject(portFolioTrade, totalReturnsDtos, i, collection);
    }

    Collections.sort(totalReturnsDtos, TotalReturnsDto.ageComparator);
    List<String> expected = tradeList(totalReturnsDtos);

    return expected;
  }

  private static void listOfTradeObject(PortfolioTrade[] portFolioTrade, 
      ArrayList<TotalReturnsDto> totalReturnsDtos, int i,
      List<TiingoCandle> collection) {

    Candle last = collection.get(collection.size() - 1);
    totalReturnsDtos.add(new TotalReturnsDto(portFolioTrade[i].getSymbol(), last.getClose()));
  }

  private static List<String> tradeList(ArrayList<TotalReturnsDto> totalReturnsDtos) {
    List<String> expected = new ArrayList<String>();
    Iterator it = totalReturnsDtos.iterator();

    while (it.hasNext()) {
      expected.add(((TotalReturnsDto) it.next()).getSymbol());
    }
    return expected;
  }

  public static PortfolioTrade[] portFolioData(String[] args) 
      throws IOException, URISyntaxException {

    File filePath = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portFolioTrade = objectMapper.readValue(filePath, PortfolioTrade[].class);

    return portFolioTrade;
  }

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    List<String> expected = new ArrayList<String>();
    File filePath = resolveFileFromResources(args[0]);
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portFolioTrade = objectMapper.readValue(filePath, PortfolioTrade[].class);

    for (int i = 0; i < portFolioTrade.length; i++) {
      expected.add(portFolioTrade[i].getSymbol());
    }

    return expected;
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader()
        .getResource(filename).toURI()).toFile();
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/"
        + "rakeshkumariiit1-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@373ebf74";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplicationTest.mainReadFile()";
    String lineNumberFromTestFileInStackTrace = "22";

    return Arrays.asList(new String[] { valueOfArgument0, 
        resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainReadQuotes(args));

  }

}
