package fctreddit.clients.UsersClients;

import fctreddit.api.User;
import fctreddit.api.java.Result;

public abstract class UsersClient {
    //public abstract Result<String> createUser(User user); // Create a new user
    public abstract Result<User> getUser(String userId, String password); // Get a user by ID and password
    //public abstract Result<String> updateUser(User user); // Update an existing user
    //public abstract Result<String> deleteUser(String userId, String password); // Delete a user by ID and passwordpublic abstract Result<String> getUserIdByEmail(String email); // Get a user ID by email address
}
