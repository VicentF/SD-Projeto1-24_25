package fctreddit.impl.java;
import java.util.List;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Result.ErrorCode;
import fctreddit.api.java.Users;
import fctreddit.impl.persistence.Hibernate;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

@Singleton
public class JavaUsers implements Users{

    //private final Map<String, User> users;
    private final Hibernate hibernate;
    /*private JavaImages images;
    private JavaContent content;*/

    public JavaUsers() {
        hibernate = Hibernate.getInstance();
		//this.users = new ConcurrentHashMap<>();
	}

    /*public void setImages(JavaImages images){
        this.images = images; 
    }

    public void setContent(JavaContent content){
        this.content = content;
    }*/

    @Override
    public Result<String> createUser(User user){
        String userId = user.getUserId();
		if (userId == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
            System.out.println("User object invalid.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
		}

		try {
			hibernate.persist(user);
		} catch (Exception e) {
			e.printStackTrace(); //Most likely the exception is due to the user already existing...
			Result.error(Result.ErrorCode.CONFLICT);
		}
		
		return Result.ok(userId);
    }

    /*public Result<String> createUser(User user) {
        String userId = user.getUserId();
        if(users.containsKey(userId)){
            System.out.println("User already exists.");
            return Result.error(Result.ErrorCode.CONFLICT);
        }
        if(userId == null){
            System.out.println("UserId is null");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        users.put(userId, user);
        System.out.printf("User added: ID %s; Full name: %s; Email: %s; Password: %s ", userId, user.getFullName(), user.getEmail(), user.getPassword());
        return Result.ok(userId);

    }*/

    @Override
    public Result<User> getUser(String userId, String password){
		// Check if user is valid
		if (userId == null || password == null) {
			System.out.println("UserId or password null.");
            return Result.error(Result.ErrorCode.BAD_REQUEST);
		}

		User user = null;
		try {
			user = hibernate.get(User.class, userId);
		} catch (Exception e) {
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

    /*public Result<User> getUser(String userId, String password) {
        if(!users.containsKey(userId)){
            System.out.println("User doesn't exist");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        User user = users.get(userId);
        if(!user.getPassword().equals(password)){
            System.err.println("Wrong password");
            Result.error(Result.ErrorCode.FORBIDDEN);
        }

        System.out.println("Here's the user my boy");
        return Result.ok(user);
    }*/

    @Override
    public Result<User> updateUser(String userId, String password, User user){
        // Check if user is valid
		Result<User> oldUserRes = this.getUser(userId, password);
        if(!oldUserRes.isOK()){
            return Result.error(oldUserRes.error());
        }

        User oldUser = oldUserRes.value();

		if(user.getFullName() != null) {
			oldUser.setFullName(user.getFullName());
		}
		if(user.getEmail() != null) {
			oldUser.setEmail(user.getEmail());
		}
		/*if(user.getAvatar() != null) {
			oldUser.setAvatarURI(user.getAvatar());
		}*/
		if(user.getPassword() != null) {
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

    /*@Override
    public Result<User> updateUser(String userId, String password, User user) {
        Result<User> getRes = getUser(userId, password);
        if(!getRes.isOK()){
            return Result.error(getRes.error());
        }
        User oldUser = getRes.value();
        if(user.getAvatarUrl() != null){
            oldUser.setAvatarUrl(user.getAvatarUrl());
        }
        if(user.getEmail() != null){
            oldUser.setEmail(user.getEmail());
        }
        if(user.getFullName() != null){
            oldUser.setFullName(user.getFullName());
        }
        if(user.getPassword() != null){
            oldUser.setPassword(user.getPassword());
        }
        if(user.getFullName() != null){
            oldUser.setFullName(user.getFullName());
        }
        return Result.ok(oldUser);
    }*/

    @Override
    public Result<User> deleteUser(String userId, String password){
		Result<User> userRes = this.getUser(userId, password);
        if(!userRes.isOK()){
            return Result.error(userRes.error());
        }
        User user = userRes.value();
		try {
			hibernate.delete(user);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
		}
        //Ainda precisa de comunicar com o content e o images para remover imagens e authorId's, quando estiverem tratados os
        //clientes desses dois serviços, trata-se disso
		return Result.ok(user);
    }

    /*@Override
    public Result<User> deleteUser(String userId, String password) {
        Result<User> getRes = getUser(userId, password);
        if(!getRes.isOK()){
            return Result.error(getRes.error());
        }
        users.remove(userId);
        //calma lá que também é preciso remover o seu avatar, coisa que ainda vou descobrir como vou fazer
        //tmb é preciso remover a referência a este user em todos os posts que ele fez
        //content.deleteUserId(userId);
        return Result.ok(getRes.value());
    }*/

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

    /*@Override
    public Result<List<User>> searchUsers(String pattern) {
        // TODO meter as passwords a "" IMPORTANTE
        List<User> result;
        if(pattern.isEmpty()){
            result = users.values().stream().toList();
        } else {
        result = users.values().stream()
                .filter(user -> user.getFullName().contains(pattern) || user.getEmail().contains(pattern))
                .toList();
        }
        return Result.ok(result);
    }*/
}
