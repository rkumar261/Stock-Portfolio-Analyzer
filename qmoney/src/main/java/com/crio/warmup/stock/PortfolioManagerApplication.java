package com.crio.warmup.stock;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) 
      throws IOException, URISyntaxException {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    ArrayList<TotalReturnsDto> totalReturnsDtos = mianReadQuotesData(args);
    PortfolioTrade[] portfolioTrades = portFolioData(args);
    int listSize = totalReturnsDtos.size();
    for (int i = 0; i < listSize; i++) {
      Double buyPrice = totalReturnsDtos.get(i).getOpenPrice();
      Double sellPrice = (totalReturnsDtos.get(i)).getClosingPrice();
      annualizedReturns
          .add(calculateAnnualizedReturns((LocalDate.parse(args[1])), 
          portfolioTrades[i], buyPrice, sellPrice));
    }
    Collections.sort(annualizedReturns, AnnualizedReturn.annualizeReturnComparator);
    return annualizedReturns;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // annualized returns should be calculated in two steps -
  // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value
  // Store the same as totalReturns
  // 2. calculate extrapolated annualized returns by scaling the same in years
  // span. The formula is
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  // Store the same as annualized_returns
  // return the populated list of AnnualizedReturn for all stocks,
  // Test the same using below specified command. The build should be successful
  // ./gradlew test --tests
  // PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    long daysBetween = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    Double annualizedReturn = Math.pow((1 + totalReturn), (365.0 / daysBetween)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    ArrayList<TotalReturnsDto> totalReturnsDtos = mianReadQuotesData(args);

    Collections.sort(totalReturnsDtos, TotalReturnsDto.closingPriceComparator);
    List<String> expected = tradeList(totalReturnsDtos);

    return expected;
  }

  private static ArrayList<TotalReturnsDto> mianReadQuotesData(String[] args)
      throws IOException, URISyntaxException, JsonProcessingException, JsonMappingException {
    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portFolioTrade = portFolioData(new String[] { args[0] });
    ArrayList<TotalReturnsDto> totalReturnsDtos = 
        new ArrayList<TotalReturnsDto>();
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
    return totalReturnsDtos;
  }

  private static void listOfTradeObject(PortfolioTrade[] portFolioTrade, 
      ArrayList<TotalReturnsDto> totalReturnsDtos,
      int i, List<TiingoCandle> collection) {

    Candle last = collection.get(collection.size() - 1);
    Candle first = collection.get(0);
    System.out.println(portFolioTrade[i].getSymbol());
    System.out.println(last.getClose());
    totalReturnsDtos.add(new TotalReturnsDto(portFolioTrade[i].getSymbol(), 
        last.getClose(), first.getOpen()));
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
    PortfolioTrade[] portFolioTrade = portFolioData(args);

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

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory,
  // Create PortfolioManager using PortfoliomanagerFactory,
  // Refer to the code from previous modules to get the List<PortfolioTrades> and
  // endDate, and
  // call the newly implemented method in PortfolioManager to calculate the
  // annualized returns.
  // Test the same using the same commands as you used in module 3
  // use gralde command like below to test your code
  // ./gradlew run --args="trades.json 2020-01-01"
  // ./gradlew run --args="trades.json 2019-07-01"
  // ./gradlew run --args="trades.json 2019-12-03"
  // where trades.json is your json file
  // Confirm that you are getting same results as in Module3.


  // private static String readFileAsString(String file) {
  //   return null;
  // }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) 
      throws Exception {
    // String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    // String contents = readFileAsString(file);
    RestTemplate restTemplate = new RestTemplate();
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager("tiingo", restTemplate);
    PortfolioTrade[] portfolioTrades = portFolioData(args);
    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  
  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}
