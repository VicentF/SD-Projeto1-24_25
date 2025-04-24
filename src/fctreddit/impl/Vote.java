package fctreddit.impl;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Vote {

    @Id
    private String userId;
    @Id
    private String postId;
    private boolean isGood;


    public Vote(String userId, String postId, boolean isGood){
        this.userId = userId;
        this.postId = postId;
        this.isGood = isGood;
    }

    public Vote() {}

    public String getUserId(){
        return userId;
    }
    public String getPostId(){
        return postId;
    }
    public boolean isGood(){
        return isGood;
    }

}
