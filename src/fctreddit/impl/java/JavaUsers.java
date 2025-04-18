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

    private final Hibernate hibernate;

    public JavaUsers() {
        hibernate = Hibernate.getInstance();
	}

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
}
