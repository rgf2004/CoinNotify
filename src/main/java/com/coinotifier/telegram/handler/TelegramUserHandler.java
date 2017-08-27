package com.coinotifier.telegram.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.objects.Update;

import com.coinotifier.core.config.ConfigUtil;
import com.coinotifier.data.handler.ProfileHandler;
import com.coinotifier.data.handler.UserHandler;
import com.coinotifier.data.model.ProfileModel;
import com.coinotifier.data.model.UserModel;
import com.coinotifier.engine.beans.User;
import com.coinotifier.engine.profile.Profile;
import com.coinotifier.engine.profile.ProfileFactory;
import com.coinotifier.telegram.bot.TelegramBot;
import com.coinotifier.telegram.bot.UserCommand;

@Service
public class TelegramUserHandler {

	private static final Logger logger = LoggerFactory.getLogger(TelegramUserHandler.class);

	private TelegramBot telegramBot;

	@Autowired
	private ConfigUtil configUtil;

	@Autowired
	private UserHandler userHandler;

	@Autowired
	private ProfileHandler profileHanlder;

	private Map<Long, User> users = new HashMap<>();

	public void initAndLoadUsers(TelegramBot bot) {

		this.telegramBot = bot;

		logger.info("Start Loading Telegram Users ...");
		List<UserModel> usersList = userHandler.findAll();
		for (UserModel userModel : usersList) {

			User user = new User(userModel);
			List<ProfileModel> profiles = profileHanlder.getProfilesByUserID(userModel.getUserId());

			for (ProfileModel profileModel : profiles) {
				Profile profile;
				try {
					profile = ProfileFactory.createProfile(ProfileType.fromString(profileModel.getProfileType()),
							profileModel.getToken(), profileModel.getSecret(), userModel.getChatID(), this.telegramBot);

					user.getProfiles().add(profile);

				} catch (Exception e) {
					logger.error("Error While Initilizing profile of User {}", user, e);
				}

			}

			if (user.getProfiles().size() > 0)
				user.setStatus(UserStatus.ACTIVE);

			users.put(user.getUserChatID(), user);
			logger.info("Existing user has been loaded [{}]", user);
		}
		logger.info("Ending Loading Telegram Users ...");
	}

	public void handleIncomingMessage(Update update) {
		// We check if the update has a message and the message has text
		logger.info("Incoming Update [{}]", update);
		if (update.hasMessage() && update.getMessage().hasText()) {
			try {
				User user = users.get(update.getMessage().getChatId());
				if (user == null) {
					// first time case
					HandleNewUser(update);
				} else {
					HandleExistingUser(user, update);
				}
			} catch (Exception e) {
				logger.error("Error Occured", e);
			}
		}
	}

	private void HandleExistingUser(User user, Update update) {

		String message = "";

		try {

			if (update.getMessage().getText().startsWith("/")) {

				UserCommand command = UserCommand.fromString(update.getMessage().getText());

				switch (command) {

				case ADD_NEW_PROFILE:
					if (user.getProfiles().isEmpty()) {
						user.backupUserStatus();
						user.setStatus(UserStatus.ADD_NEW_PROFILE);
						handleAddNewProfile(user, update.getMessage().getText());
					} else {
						message = String.format("You have already added a profile, currently Coinotify supports adding one profile.");
						telegramBot.sendMessage(user.getUserChatID(), message, null);
						break;
					}
					break;

				case GET_ORDERS:
					handleGetOrderCommand(user);
					break;

				case BALANCE:
					handleGetBalance(user);
					break;

				case CONTACTUS:
					message = String.format(
							"If you want to contact the developer you can do so at @RamyFeteha or ramyfeteha@gmail.com");
					telegramBot.sendMessage(user.getUserChatID(), message, null);
					break;

				case VERSION:
					String version = configUtil.getAppVersion();
					message = String.format(version);
					telegramBot.sendMessage(user.getUserChatID(), message, null);
				}
				;
			} else {
				if (user.getStatus() == UserStatus.WAITING_FOR_PROFILE_NAME
						|| user.getStatus() == UserStatus.WAITING_FOR_API
						|| user.getStatus() == UserStatus.WAITING_FOR_SECRET) {
					handleAddNewProfile(user, update.getMessage().getText());
					return;
				} else {
					message = String.format("Invalid Command, Please use a valid command");

					telegramBot.sendMessage(user.getUserChatID(), message, null);
				}

			}

		} catch (Exception e) {

			logger.error("Error Occured", e);

			message = String.format("Invalid Command, Please use a valid command");

			telegramBot.sendMessage(user.getUserChatID(), message, null);
		}

	}

	private void handleGetBalance(User user) {
		String message = "";
		try {
			boolean balanceSent = false;
			if (user.getStatus() != UserStatus.ACTIVE) {
				message = String.format("User is not active, please add profile using command /add_profile");
				telegramBot.sendMessage(user.getUserChatID(), message, null);
				return;
			}
			List<Profile> userProfiles = user.getProfiles();

			if (userProfiles.isEmpty()) {
				message = String.format("User doesn't have profiles, please add profile using command /add_profile");
				telegramBot.sendMessage(user.getUserChatID(), message, null);
				return;
			}

			for (Profile profile : userProfiles) {
				if (profile.sendBalance())
					balanceSent = true;
			}

			if (!balanceSent) {
				message = String.format("You don't have balance entries");
				telegramBot.sendMessage(user.getUserChatID(), message, null);
				return;
			}
		} catch (Exception e) {
			logger.error("Error Occured", e);

			message = String.format("Error Occured, please try this command later or /contact the developer");

			telegramBot.sendMessage(user.getUserChatID(), message, null);
		}
	}

	private void handleGetOrderCommand(User user) {
		String message = "";
		boolean ordersSent = false;
		List<Profile> userProfiles = user.getProfiles();

		if (userProfiles.isEmpty()) {
			message = String.format("User doesn't have profiles, please add profile using command /add_profile");
			telegramBot.sendMessage(user.getUserChatID(), message, null);
			return;
		}

		for (Profile profile : userProfiles) {
			if (profile.sendOrders())
				ordersSent = true;
		}

		if (!ordersSent) {
			message = String.format("You don't have any open orders");
			telegramBot.sendMessage(user.getUserChatID(), message, null);
			return;
		}

	}

	private void handleAddNewProfile(User user, String text) {
		String message = "";

		if (user.getStatus() == UserStatus.HAS_NO_PROFILES || user.getStatus() == UserStatus.ADD_NEW_PROFILE) {
			message = String.format("Please select the Profile Type");
			List<String> profiles = new ArrayList<>();

			for (ProfileType profile : ProfileType.values()) {
				profiles.add(profile.getValue());
			}

			telegramBot.sendMessage(user.getUserChatID(), message, profiles);
			user.setStatus(UserStatus.WAITING_FOR_PROFILE_NAME);
			return;
		} else if (user.getStatus() == UserStatus.WAITING_FOR_PROFILE_NAME) {
			try {
				ProfileType profile = ProfileType.fromString(text);

				user.setCachedProfileType(profile);

				message = String.format("Please Enter API Key");
				telegramBot.sendMessage(user.getUserChatID(), message, null);

				user.setStatus(UserStatus.WAITING_FOR_API);

			} catch (IllegalArgumentException e) {

				message = String.format("Invalid Profile Name, Please enter the command again");
				telegramBot.sendMessage(user.getUserChatID(), message, null);

				user.setStatus(UserStatus.HAS_NO_PROFILES);
			}
		} else if (user.getStatus() == UserStatus.WAITING_FOR_API) {
			message = String.format("Please Enter Secret");
			telegramBot.sendMessage(user.getUserChatID(), message, null);

			user.setCachedApiKey(text);

			user.setStatus(UserStatus.WAITING_FOR_SECRET);

		} else if (user.getStatus() == UserStatus.WAITING_FOR_SECRET) {
			// check api & key then save them and update user status to active
			// and tell him that everthing is ok
			user.setCachedSecret(text);

			saveNewProfile(user);

		}

	}

	private void saveNewProfile(User user) {

		String message = "";

		ProfileModel profileModel = new ProfileModel();
		profileModel.setProfileType(user.getCachedProfileType().getValue());
		profileModel.setToken(user.getCachedApiKey());
		profileModel.setSecret(user.getCachedSecret());
		profileModel.setUser(user.getUserModel());

		try {

			Profile profile = ProfileFactory.createProfile(user.getCachedProfileType(), user.getCachedApiKey(),
					user.getCachedSecret(), user.getUserChatID(), this.telegramBot);
			user.getProfiles().add(profile);
			user.setStatus(UserStatus.ACTIVE);

			profileHanlder.saveProfile(profileModel);

			message = String.format("Profile is valid and has been added to your account");
			telegramBot.sendMessage(user.getUserChatID(), message, null);

		} catch (Exception e) {

			user.revertUserStatus();
			logger.error("Error Occured", e);
			message = String.format(
					"Invalid Profile API or Secret, please try adding it again using command /add_profile and follow the steps");
			telegramBot.sendMessage(user.getUserChatID(), message, null);
		}

	}

	private void HandleNewUser(Update update) {
		User user = SaveNewUser(update);

		if (user != null) // start adding profile process
		{
			String message = "";
			message = String.format(
					"In order to use this bot, you should add at least one profile for bittrex or polonix by entering the api key and secret which you can get from their website");

			telegramBot.sendMessage(user.getUserChatID(), message, null);

			message = String.format(
					"When you have your api key and secret please type command /add_profile and follow the steps");

			telegramBot.sendMessage(user.getUserChatID(), message, null);

		}
	}

	private User SaveNewUser(Update update) {
		UserModel userModel = new UserModel();
		userModel.setChatID(update.getMessage().getFrom().getId());
		userModel.setFirstName(update.getMessage().getFrom().getFirstName());
		userModel.setLastName(update.getMessage().getFrom().getLastName());
		userModel.setUserName(update.getMessage().getFrom().getUserName());

		String message = "";

		try {
			User user = saveUser(userModel);
			if (user != null) {

				message = String.format(
						"Hello %s, Welcome to Coinofity Bot which will help you to deal with CryptoCurrencies trading",
						userModel.getFirstName() != null ? userModel.getFirstName() : "My Friend");

				return user;
			}

		} catch (Exception e) {
			logger.error("Error Occured", e);

			message = "Sorry Coinotify Bot is under maintenance now please visit it later ";

			return null;

		} finally {

			telegramBot.sendMessage(userModel.getChatID(), message, null);

		}

		return null;
	}

	private User saveUser(UserModel userModel) throws Exception {

		boolean isInserted = userHandler.saveUser(userModel);
		if (isInserted) {
			User user = new User(userModel);
			user.setStatus(UserStatus.HAS_NO_PROFILES);
			users.put(user.getUserChatID(), user);
			return user;
		} else {
			return null;
		}
	}

	public Map<Long, User> getUsers() {
		return new HashMap<>(users);
	}

}
