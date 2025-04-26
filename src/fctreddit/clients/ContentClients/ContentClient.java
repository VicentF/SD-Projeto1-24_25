package fctreddit.clients.ContentClients;

import fctreddit.api.java.Result;

public abstract class ContentClient {
    public abstract Result<Void> deleteAuthor(String userId, String userPassword);
}
