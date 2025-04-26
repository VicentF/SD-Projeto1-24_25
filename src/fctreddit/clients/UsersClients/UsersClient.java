package fctreddit.clients.UsersClients;

import fctreddit.api.User;
import fctreddit.api.java.Result;

public abstract class UsersClient {

    public abstract Result<User> getUser(String userId, String password); // Get a user by ID and password

}
