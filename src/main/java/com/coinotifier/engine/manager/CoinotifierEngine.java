package com.coinotifier.engine.manager;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import com.coinotifier.core.config.ConfigUtil;
import com.coinotifier.engine.threads.OrdersRefresh;
import com.coinotifier.engine.threads.ProfileRefresh;
import com.coinotifier.telegram.bot.TelegramBot;
import com.coinotifier.telegram.handler.TelegramUserHandler;

@Service
public class CoinotifierEngine {

	@Autowired
	private ConfigUtil configUtil;

	@Autowired
	private TelegramUserHandler telegramUserHandler;

	@Autowired
	private ProfileRefresh profileRefresh;

	@Autowired
	private OrdersRefresh ordersRefresh;

	final static Logger logger = LoggerFactory.getLogger(CoinotifierEngine.class);

	private TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
	private TelegramBot telegramBot;

	private static final String TOKEN_PARAM_NAME = "telegram.token";
	private static final String BOT_NAME_PARAM = "telegram.botname";

	@PostConstruct
	public void init() {

		try {

			telegramBot = new TelegramBot(telegramUserHandler,
					configUtil.getEnvironment().getRequiredProperty(TOKEN_PARAM_NAME),
					configUtil.getEnvironment().getRequiredProperty(BOT_NAME_PARAM));

			telegramUserHandler.initAndLoadUsers(telegramBot);
			telegramBotsApi.registerBot(telegramBot);

			Thread profileRefreshThread = new Thread(profileRefresh);
			profileRefreshThread.start();

			ordersRefresh.setTelegramUserHandler(telegramUserHandler);
			Thread ordersRefreshThread = new Thread(ordersRefresh);
			ordersRefreshThread.start();

		} catch (TelegramApiRequestException e) {
			logger.error("Couldn't start Telegram Bot", e);
		} catch (Exception e) {
			logger.error("General Error", e);
		}
	}

}
