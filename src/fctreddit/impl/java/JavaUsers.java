package fctreddit.impl.java;
import java.util.List;
import java.util.logging.Logger;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.java.Users;
import fctreddit.clients.ContentClients.RestContentClient;
import fctreddit.clients.ImagesClients.RestImagesClient;
import fctreddit.impl.persistence.Hibernate;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

@Singleton
public class JavaUsers implements Users{

    private final Hibernate hibernate;
	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());
	private static RestContentClient contentClient = null;
	private static RestImagesClient imageClient = null;

    public JavaUsers() {
        hibernate = Hibernate.getInstance();
	}

    @Override
    public Result<String> createUser(User user){
		//Log.info("Erm JAVA_USERS");
        String userId = user.getUserId();
		if (!isValid(userId, user.getPassword(), user.getFullName(), user.getEmail())) {
            System.out.println("User object invalid.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
		}

		try {
			hibernate.persist(user);
		} catch (Exception e) {
			e.printStackTrace(); //Most likely the exception is due to the user already existing...
			return Result.error(Result.ErrorCode.CONFLICT);
		}
		
		return Result.ok(userId);
    }

    @Override
    public Result<User> getUser(String userId, String password){
		User user = null;
		try {
			user = hibernate.get(User.class, userId);
		} catch (Exception e) {
			Log.info("JavaUsers :: Internal error getting user");
			e.printStackTrace();
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}

		// Check if user exists
		if (user == null) {
            System.out.println("User does not exist.");
            return Result.error(Result.ErrorCode.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
            System.out.println("Password is incorrect.");
            return Result.error(Result.ErrorCode.FORBIDDEN);
		}
		return Result.ok(user);
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user){
        // Check if user is valid
		Result<User> oldUserRes = this.getUser(userId, password);
        if(!oldUserRes.isOK()){
            return Result.error(oldUserRes.error());
        }

        User oldUser = oldUserRes.value();

		if(isValid(user.getFullName())) {
			oldUser.setFullName(user.getFullName());
		}
		if(isValid(user.getEmail())) {
			oldUser.setEmail(user.getEmail());
		}
		if(isValid(user.getPassword())) {
			oldUser.setPassword(user.getPassword());
		}
		try{
			hibernate.update(oldUser);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
		return Result.ok(oldUser);
    }

	private static void initializeContentClient() {
		if (contentClient == null) {
			contentClient = new RestContentClient();
		}
	}

	private static void initializeImagesClient() {
		if (imageClient == null) {
			imageClient = new RestImagesClient();
		}
	}

    @Override
    public Result<User> deleteUser(String userId, String password){
		//Log.info("JavaUsers :: deleteUser() - userId: " + userId + ", password: " + password);
		Result<User> userRes = this.getUser(userId, password);
        if(!userRes.isOK()){
            return Result.error(userRes.error());
        }
		initializeContentClient();
		contentClient.deleteAuthor(userId, password);

		initializeImagesClient();
		String[] split = userRes.value().getAvatarUrl().split("/");
		String avatarId = split[split.length - 1];
		imageClient.deleteImage(userId, avatarId, password);
		
        User user = userRes.value();
		try {
			hibernate.delete(user);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
		return Result.ok(user);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern){
		try {
			List<User> list = hibernate.jpql("SELECT u FROM User u WHERE u.userId LIKE '%" + pattern +"%'", User.class);
            for(User user : list){
                user.setPassword("");
            }
			return Result.ok(list);
		} catch (Exception e) {
            return Result.error(ErrorCode.INTERNAL_ERROR);
		}
    }

	private boolean isValid(String... params) {
        for (String param : params) {
            if (param == null || param.isBlank()) return false;
        }
        return true;
    }
}
