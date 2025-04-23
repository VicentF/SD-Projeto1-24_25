package fctreddit.impl.java;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import fctreddit.api.Post;
import fctreddit.api.User;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.clients.UsersClients.RestUsersClient;
import fctreddit.impl.persistence.Hibernate;

public class JavaContent implements  Content{
    //well i'll be damned
    //private final Map<String, Post> posts;
    private final Hibernate hibernate;
    private final RestUsersClient client;
    private final static Logger Log = Logger.getLogger(JavaContent.class.getName());
    private static String serverUri;
    private final Map<String, Object> postLocks = new ConcurrentHashMap<>();

    public JavaContent(String serverUri) {
        hibernate = Hibernate.getInstance();
        this.serverUri = serverUri;
        //posts = new ConcurrentHashMap<>();
        client = new RestUsersClient();
    }

    @Override
    public Result<String> createPost(Post post, String userPassword){
        post.setCreationTimestamp(System.currentTimeMillis());
        post.setPostId(UUID.randomUUID().toString());
        Result<User> resUser = client.getUser(post.getAuthorId(), userPassword);
        if (!resUser.isOK()) {
            //Log.info("JavaContent :: User not found");
            return Result.error(resUser.error());
        }

        try{
            hibernate.persist(post);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error persisting post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if(post.getParentUrl() == null){
            postLocks.put(post.getPostId(), new Object());
        } else {
            //Log.info("JavaContent :: Post has parentUrl: " + post.getParentUrl());
            String[] split = post.getParentUrl().split("/");
            String parentPostId = split[split.length - 1];
            Object lock = postLocks.get(parentPostId);
            synchronized(lock){
                lock.notifyAll();
            }
        }
        return Result.ok(post.getPostId());
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder){
        List<String> sortedKeys;
        String query = "";
        if(timestamp > 0){
            query += "SELECT p.postId FROM Post p WHERE p.creationTimestamp >= " + timestamp + " AND p.parentUrl IS NULL";
        } else {
            query += "SELECT p.postId FROM Post p WHERE p.parentUrl IS NULL";
        }
        if(sortOrder == null){
            query += " ORDER BY p.creationTimestamp ASC";
            sortedKeys = hibernate.jpql(query, String.class);
        } else {
            switch(sortOrder){
                case MOST_UP_VOTES:
                    query += " ORDER BY p.upVote DESC, p.creationTimestamp ASC";
                    sortedKeys = hibernate.jpql(query, String.class);
                    break;
                case MOST_REPLIES:
                    query += " ORDER BY ( SELECT COUNT(c) FROM Post c WHERE c.parentUrl LIKE CONCAT('%', p.postId, '%')) DESC";
                    sortedKeys = hibernate.jpql(query, String.class);
                    break;
                default:
                    return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
        }
        return Result.ok(sortedKeys);
    }

    @Override
    public Result<Post> getPost(String postId) {
        //Log.info("JavaContent :: Get Post");
        Post post = null;
        try{
            post = hibernate.get(Post.class, postId);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error getting post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        if(post == null){
            //Log.info("JavaContent :: Post not found");
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else{
            return Result.ok(post);
        }
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout){
        //Log.info("JavaContent :: Get Post Answers");
        if(!this.getPost(postId).isOK()){
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        if(maxTimeout > 0){
            Object lock = postLocks.get(postId);
            try {
                synchronized(lock){
                    lock.wait(maxTimeout);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.info("JavaContent :: Internal error sleeping thread");
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        //Log.info("JavaContent :: p.parentUrl = " + serverUri + "/" + postId);
        String parentUrl = serverUri + "/posts/" + postId;
        String query = "SELECT p.postId FROM Post p WHERE p.parentUrl = '" + parentUrl + "' ORDER BY p.creationTimestamp ASC";
        List<String> sortedKeys = hibernate.jpql(query, String.class);
        return Result.ok(sortedKeys);
    }

    @Override
    public Result<Post> updatePost(String postId, String userPassword, Post post) {
        Result<Post> resPost = getPost(postId);
        if(!resPost.isOK()){
            return Result.error(resPost.error());
        }
        Post oldPost = resPost.value();
        if(post.getContent() != null){
            oldPost.setContent(post.getContent());
        }
        if(post.getMediaUrl() != null){
            oldPost.setMediaUrl(post.getMediaUrl());
        }
        //checkar se é suposto lançar erro quando se tenta updatar algo que não se deve
        hibernate.update(oldPost);
        return Result.ok(oldPost);
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        Result<User> resUser = client.getUser(postId, userPassword);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        hibernate.delete(postId);
        //mandar a media(images) para o lixo as well
        String query = "SELECT p FROM Post p WHERE p.parentUrl LIKE CONCAT('%', :postId, '%')";
        List<Post> postsToUpdate = hibernate.jpql(query, Post.class);
        for(Post post : postsToUpdate){
            post.setParentUrl(null);
            hibernate.update(post);
        }
        return Result.ok();
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'upVotePost'");
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeUpVotePost'");
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'downVotePost'");
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeDownVotePost'");
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getupVotes'");
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDownVotes'");
    }

    //apagar os userIds dos posts do user userId
    /*public Result<Post> deleteUserId(String userId) {
        // DELETE FROM Post WHERE authorId LIKE '%userId%'
        List<String> postsToUpdate = posts.entrySet().stream()
            .filter(entry -> userId.equals(entry.getValue().getAuthorId()))
            .map(Map.Entry::getKey)
            .toList();
        for (String key : postsToUpdate) {
            Post post  = null;
            try{
                post = hibernate.get(Post.class, key);
            } catch (Exception e) {
                Log.info("JavaContent :: Internal error deleting userId");
                e.printStackTrace();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            post.setAuthorId(null);
            try {
                hibernate.update(post);
            } catch (Exception e) {
                Log.info("JavaContent :: Internal error updating post");
                e.printStackTrace();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.ok(null);
    }*/

    private boolean isValid(String... params) {
        for (String param : params) {
            if (param == null || param.isBlank()) return false;
        }
        return true;
    }
    
}
