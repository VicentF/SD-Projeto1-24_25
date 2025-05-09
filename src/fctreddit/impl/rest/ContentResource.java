package fctreddit.impl.rest;

import java.util.List;
import java.util.logging.Logger;

import fctreddit.api.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestContent;
import fctreddit.impl.java.JavaContent;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public class ContentResource implements RestContent {
    
    private static Logger Log = Logger.getLogger(ContentResource.class.getName()); 
    
    final Content impl;
    
    public ContentResource(String uri) {
        impl = new JavaContent(uri);
    }
    

    @Override
    public String createPost(Post post, String userPassword) {
        //Log.info("Create Post");

        Result<String> res = impl.createPost(post, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public List<String> getPosts(long timestamp, String sortOrder) {
        //Log.info("Get Posts");
        Result<List<String>> res = impl.getPosts(timestamp, sortOrder);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Post getPost(String postId) {
        //Log.info("Get Post");
        Result<Post> res = impl.getPost(postId);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public List<String> getPostAnswers(String postId, long timeout) {
        //Log.info("Get Post Answers");
        Result<List<String>> res = impl.getPostAnswers(postId, timeout);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Post updatePost(String postId, String userPassword, Post post) {
        //Log.info("Update Post");
        Result<Post> res = impl.updatePost(postId, userPassword, post);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void deletePost(String postId, String userPassword) {
        //Log.info("Delete Post");
        Result<Void> res = impl.deletePost(postId, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void upVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.upVotePost(postId, userId, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void removeUpVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.removeUpVotePost(postId, userId, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void downVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.downVotePost(postId, userId, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public void removeDownVotePost(String postId, String userId, String userPassword) {
        Result<Void> res = impl.removeDownVotePost(postId, userId, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    @Override
    public Integer getupVotes(String postId) {
        Result<Integer> res = impl.getupVotes(postId);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public Integer getDownVotes(String postId) {
        Result<Integer> res = impl.getDownVotes(postId);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void deleteAuthor(String userId, String userPassword) {
        Log.info("Content Resource :: Delete Author HEEEEELP");
        Result<Void> res = impl.deleteAuthor(userId, userPassword);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    //TODO se calhar meter esta situação numa data class para não repetir código
    protected static Status errorCodeToStatus( Result.ErrorCode error ) {
    	Status status =  switch( error) {
    	case NOT_FOUND -> Status.NOT_FOUND; 
    	case CONFLICT -> Status.CONFLICT;
    	case FORBIDDEN -> Status.FORBIDDEN;
    	case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
    	case BAD_REQUEST -> Status.BAD_REQUEST;
    	default -> Status.INTERNAL_SERVER_ERROR;
    	};
    	
    	return status;
    }


    
    
}
