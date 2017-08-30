package com.mformad.cfloor;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.coinfloor.CoinfloorExchange;
import org.knowm.xchange.coinfloor.dto.account.CoinfloorBalance;
import org.knowm.xchange.coinfloor.dto.markedata.CoinfloorTicker;
import org.knowm.xchange.coinfloor.service.CoinfloorAccountServiceRaw;
import org.knowm.xchange.coinfloor.service.CoinfloorMarketDataServiceRaw;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Date;

import static java.time.LocalTime.now;

/**
 * Hello world!
 *
 */
public class App {

    static BigDecimal position = BigDecimal.ZERO;
    static BigDecimal profit = BigDecimal.ZERO;
    static BigDecimal entryPercent = BigDecimal.valueOf(1);
    static BigDecimal exitPercent = BigDecimal.valueOf(2);
    public static void main(String[] args) throws Exception {


        // Use the factory to get Bitstamp exchange API using default settings
        Exchange coinfloor = createExchange(args);

        // Interested in the public market data feed (no authentication)
        MarketDataService marketDataService = coinfloor.getMarketDataService();
//        generic(marketDataService);
        raw((CoinfloorMarketDataServiceRaw) marketDataService);

        AccountService accountService = coinfloor.getAccountService();
        //generic(accountService);
        //raw((CoinfloorAccountServiceRaw)accountService);
    }

    private static void generic(MarketDataService marketDataService) throws IOException {

        Ticker ticker = marketDataService.getTicker(CurrencyPair.BTC_GBP);

        System.out.println(ticker.toString());
    }

    private static void raw(CoinfloorMarketDataServiceRaw marketDataService) throws Exception {
        while(true) {
            CoinfloorTicker tick = marketDataService.getCoinfloorTicker(CurrencyPair.BTC_GBP);
            BigDecimal spread = tick.getAsk().subtract(tick.getBid());
            BigDecimal fair = tick.getAsk().subtract(spread.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            BigDecimal percent = tick.getVwap()
                    .subtract(fair)
                    .divide(tick.getVwap(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            System.out.println(now() + " : percent=" + percent + " @ " + tick.toString());

            if(position.compareTo(BigDecimal.ZERO) == 0) {
                if (percent.compareTo(BigDecimal.ZERO) > 0) {
                    if (percent.compareTo(entryPercent) >= 0) {
                        position = BigDecimal.ONE;
                        profit = tick.getAsk().negate();
                        System.out.println(now() +
                                " : BUY @ " + tick.getAsk() +
                                " , POSITION @ " + position +
                                " , PROFIT @ " + profit);
                    }
                }
            } else if(position.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal profitPercent = position
                        .subtract(tick.getBid())
                        .divide(position, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if(profitPercent.compareTo(exitPercent)> 0) {
                    profit = profit.add(tick.getBid());
                    position = BigDecimal.ZERO;
                    System.out.println(now() +
                            " : SELL @ " + tick.getBid() +
                            " , POSITION @ " + position +
                            " , PROFIT @ " + profit);
                }
            }

            Thread.sleep(1000 * 30);
        }
    }

    private static void generic(AccountService accountService) throws IOException {
        try {
            // Get the account information
            AccountInfo accountInfo = accountService.getAccountInfo();
            System.out.println("AccountInfo as String: " + accountInfo.toString());

            String depositAddress = accountService.requestDepositAddress(Currency.BTC);
            System.out.println("Deposit address: " + depositAddress);

            String withdrawResult = accountService.withdrawFunds(Currency.BTC, new BigDecimal(1).movePointLeft(4), "XXX");
            System.out.println("withdrawResult = " + withdrawResult);
        } catch (Exception ex) {
            System.out.println("ex = " + ex);
        }
    }

    private static void raw(CoinfloorAccountServiceRaw accountService) throws IOException {
        try {
            CoinfloorBalance balance = accountService.getCoinfloorBalance(CurrencyPair.BTC_GBP);
            System.out.println("BitstampBalance: " + balance);
        } catch (Exception ex) {
            System.out.println("ex: " + ex);
        }
    }

    public static Exchange createExchange(String[] args) {
        ExchangeSpecification exSpec = new CoinfloorExchange().getDefaultExchangeSpecification();
        exSpec.setUserName(args[0]);
//        exSpec.setApiKey(args[1]);
//        exSpec.setSecretKey(args[2]);
//        exSpec.setPassword(args[1]);
        return ExchangeFactory.INSTANCE.createExchange(exSpec);
    }
}