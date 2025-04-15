package fctreddit.impl.java;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import jakarta.inject.Singleton;

@Singleton
public class JavaUsers implements Users{

    private final Map<String, User> users;
    private JavaImages images;
    private JavaContent content;

    public JavaUsers() {
		this.users = new ConcurrentHashMap<>();
	}

    public void setImages(JavaImages images){
        this.images = images; 
    }

    public void setContent(JavaContent content){
        this.content = content;
    }

    @Override
    public Result<String> createUser(User user) {
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

    }

    @Override
    public Result<User> getUser(String userId, String password) {
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
    }

    @Override
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
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        Result<User> getRes = getUser(userId, password);
        if(!getRes.isOK()){
            return Result.error(getRes.error());
        }
        users.remove(userId);
        //calma lá que também é preciso remover o seu avatar, coisa que ainda vou descobrir como vou fazer
        //tmb é preciso remover a referência a este user em todos os posts que ele fez
        content.deleteUserId(userId);
        return Result.ok(getRes.value());
    }

    @Override
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
    }
}
