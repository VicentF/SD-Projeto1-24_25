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
import fctreddit.clients.ImagesClients.ImagesClient;
import fctreddit.clients.ImagesClients.ImagesClientFactory;
import fctreddit.clients.UsersClients.UsersClient;
import fctreddit.clients.UsersClients.UsersClientFactory;
import fctreddit.impl.Vote;
import fctreddit.impl.persistence.Hibernate;

public class JavaContent implements  Content{
    private final Hibernate hibernate;
    private static UsersClient usersClient = null;
    private static ImagesClient imageClient = null;
    private static final UsersClientFactory usersClientFactory = new UsersClientFactory();
    private final static ImagesClientFactory imagesClientFactory = new ImagesClientFactory();
    private final static Logger Log = Logger.getLogger(JavaContent.class.getName());
    private final String serverUri;
    private final Map<String, Object> postLocks = new ConcurrentHashMap<>();

    public JavaContent(String uri) {
        hibernate = Hibernate.getInstance();
        this.serverUri = uri;
        //Log.info("JavaContent :: serverURI = " + serverUri);
    }

    @Override
    public Result<String> createPost(Post post, String userPassword){
        Log.info("JavaContent :: Create Post");
        post.setCreationTimestamp(System.currentTimeMillis());
        post.setPostId(UUID.randomUUID().toString());
        initializeUsersClient();
        Result<User> resUser = usersClient.getUser(post.getAuthorId(), userPassword);
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
        if(post.getParentUrl() == null || post.getParentUrl().isEmpty()){
            postLocks.put(post.getPostId(), new Object());
        } else {
            Log.info("JavaContent :: Post has parentUrl: " + post.getParentUrl());
            postLocks.put(post.getPostId(), new Object());
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
        if(sortOrder == null || sortOrder.isEmpty()){
            query += " ORDER BY p.creationTimestamp ASC";
            sortedKeys = hibernate.jpql(query, String.class);
        } else {
            switch(sortOrder){
                case MOST_UP_VOTES:
                    query += " ORDER BY p.upVote DESC";
                    sortedKeys = hibernate.jpql(query, String.class);
                    break;
                case MOST_REPLIES:
                    String halfParentUrl = serverUri + "/posts/";
                    query += " ORDER BY ( SELECT COUNT(c) FROM Post c WHERE c.parentUrl = CONCAT('" + halfParentUrl + "', p.postId)) DESC";
                    sortedKeys = hibernate.jpql(query, String.class);
                    break;
                default:
                    return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
        }
        for(String postId : sortedKeys){
            Post post = getPost(postId).value();
            Log.info("TIMESTAMP :: " + post.getCreationTimestamp());
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
        Log.info("JavaContent :: serverURI = " + serverUri);
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
        //Log.info("JavaContent :: p.parentUrl = " + serverUri + "/posts/" + postId);
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
        String parentUrl = serverUri + "/posts/" + postId;
        List<Post> replies = hibernate.jpql("SELECT p FROM Post p WHERE p.parentUrl = '" + parentUrl + "'", Post.class);
        if(!replies.isEmpty()){
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        List<Vote> votes = hibernate.jpql("SELECT v FROM Vote v WHERE v.postId = '" + postId + "'", Vote.class);
        if(!votes.isEmpty()){
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        Post oldPost = resPost.value();
        initializeUsersClient();
        Result<User> resUser = usersClient.getUser(oldPost.getAuthorId(), userPassword);
        if (!resUser.isOK()) {
            Log.info("JavaContent :: Trouble getting user");
            return Result.error(resUser.error());
        }
        if(post.getContent() != null && !post.getContent().isEmpty()){
            oldPost.setContent(post.getContent());
        }
        if(post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()){
            oldPost.setMediaUrl(post.getMediaUrl());
        }
        //checkar se é suposto lançar erro quando se tenta updatar algo que não se deve
        try{
            hibernate.update(oldPost);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error updating post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(oldPost);
    }

    private Result<Void> deletePost(Post post){
        try {
            hibernate.delete(post);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error deleting post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        //Image client para apagar a media
        String postId = post.getPostId();
        String parentUrl = serverUri + "/posts/" + postId;
        String query = "SELECT p FROM Post p WHERE p.parentUrl = '" + parentUrl + "'";
        List<Post> postsToUpdate = hibernate.jpql(query, Post.class);
        for(Post p : postsToUpdate){
            deletePost(p);
        }
        List<Vote> votesToUpdate = hibernate.jpql("SELECT v FROM Vote v WHERE v.postId = '" + postId + "'", Vote.class);
        for(Vote v : votesToUpdate){
            try {
                hibernate.delete(v);
            } catch (Exception e) {
                Log.info("JavaContent :: Internal error deleting vote");
                e.printStackTrace();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        
        return Result.ok();
    }

    @Override
    public Result<Void> deletePost(String postId, String userPassword) {
        Log.info("JavaContent :: Delete Post");
        Result<Post> resPost = getPost(postId);
        if(!resPost.isOK()){
            return Result.error(resPost.error());
        }
        Post post = resPost.value();
        String mediaUrl = post.getMediaUrl();
        if(mediaUrl != null && !mediaUrl.isEmpty()){
            Log.info("JavaContent :: Post has mediaUrl: " + mediaUrl);
            Log.info("Checkpoint1");
            String[] split = mediaUrl.split("/");
            String imageId = split[split.length - 1];
            initializeImagesClient();
            /*Log.info("JavaContent :: imageId = " + imageId);
            Log.info("JavaContent :: userId = " + post.getAuthorId());
            Log.info("JavaContent :: password = " + userPassword);*/
            Result<Void> resImage = imageClient.deleteImage(post.getAuthorId(), imageId, userPassword);
            Log.info("Checkpoint2");
            if(!resImage.isOK()){
                Log.info("Checkpoint3");
                return Result.error(resImage.error());
            }
        } else {    
            String authorId = post.getAuthorId();
            initializeUsersClient();
            Result<User> resUser = usersClient.getUser(authorId, userPassword);
            if (!resUser.isOK()) {
                return Result.error(resUser.error());
            }
        }
        deletePost(post);
        return Result.ok();
    }

    //Metodo para evitir repetir código
    private Result<Vote> checkVote(String userId, String postId, String userPassword, Result<Post> resPost) {
        initializeUsersClient();
        Result<User> resUser = usersClient.getUser(userId, userPassword);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        if(!resPost.isOK()){
            return Result.error(resPost.error());
        }
        Vote vote;
        try {
            List<Vote> votes = hibernate.jpql("SELECT v FROM Vote v WHERE v.userId = '" + userId + "' AND v.postId = '" + postId + "'", Vote.class);
            if(!votes.isEmpty()){
                vote = votes.get(0);
            } else {
                vote = null;
            }
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error getting vote");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        return Result.ok(vote);
    }

    @Override
    public Result<Void> upVotePost(String postId, String userId, String userPassword) {
        Result<Post> resPost = getPost(postId);
        Result<Vote> resVote = checkVote(userId, postId, userPassword, resPost);
        if(!resVote.isOK()){
            return Result.error(resVote.error());
        }
        Vote vote = resVote.value();
        if(vote != null){
            return Result.error(Result.ErrorCode.CONFLICT);
        }
        vote = new Vote(userId, postId, true);
        try {
            hibernate.persist(vote);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error persisting vote");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Post post = resPost.value();
        post.setUpVote(post.getUpVote() + 1);
        try {
            hibernate.update(post);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error updating post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeUpVotePost(String postId, String userId, String userPassword) {
        Result<Post> resPost = getPost(postId);
        Result<Vote> resVote = checkVote(userId, postId, userPassword, resPost);
        if(!resVote.isOK()){
            return Result.error(resVote.error());
        }
        Vote vote = resVote.value();
        if(vote == null || !vote.isGood()){
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        try {
            hibernate.delete(vote);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error deleting vote");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Post post = resPost.value();
        post.setUpVote(post.getUpVote() - 1);
        try {
            hibernate.update(post);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error updating post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> downVotePost(String postId, String userId, String userPassword) {
        Result<Post> resPost = getPost(postId);
        Result<Vote> resVote = checkVote(userId, postId, userPassword, resPost);
        if(!resVote.isOK()){
            return Result.error(resVote.error());
        }
        Vote vote = resVote.value();
        if(vote != null){
            return Result.error(Result.ErrorCode.CONFLICT);
        }
        vote = new Vote(userId, postId, false);
        try {
            hibernate.persist(vote);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error persisting vote");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Post post = resPost.value();
        post.setDownVote(post.getDownVote() + 1);
        try {
            hibernate.update(post);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error updating post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Void> removeDownVotePost(String postId, String userId, String userPassword) {
        Result<Post> resPost = getPost(postId);
        Result<Vote> resVote = checkVote(userId, postId, userPassword, resPost);
        if(!resVote.isOK()){
            return Result.error(resVote.error());
        }
        Vote vote = resVote.value();
        if(vote == null || vote.isGood()){
            return Result.error(Result.ErrorCode.CONFLICT);
        }

        try {
            hibernate.delete(vote);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error deleting vote");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        Post post = resPost.value();
        post.setDownVote(post.getDownVote() - 1);
        try {
            hibernate.update(post);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error updating post");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }

        return Result.ok();
    }

    @Override
    public Result<Integer> getupVotes(String postId) {
        Result<Post> resPost = getPost(postId);
        if(!resPost.isOK()){
            return Result.error(resPost.error());
        }
        int upVotes = resPost.value().getUpVote();
        return Result.ok(upVotes);
    }

    @Override
    public Result<Integer> getDownVotes(String postId) {
        Result<Post> resPost = getPost(postId);
        if(!resPost.isOK()){
            return Result.error(resPost.error());
        }
        int downVotes = resPost.value().getDownVote();
        return Result.ok(downVotes);
    }

    @Override
    public Result<Void> deleteAuthor(String userId, String userPassword) {
        Log.info("JavaContent :: Delete Author");
        initializeUsersClient();
        Result<User> resUser = usersClient.getUser(userId, userPassword);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        List<Post> postsToUpdate;
        try{
            postsToUpdate = hibernate.jpql("SELECT p FROM Post p WHERE p.authorId = '" + userId + "'", Post.class);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error deleting authorId");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        for(Post p : postsToUpdate){
            p.setAuthorId(null);
            try {
                hibernate.update(p);
            } catch (Exception e) {
                Log.info("JavaContent :: Internal error updating post");
                e.printStackTrace();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        deleteUserVotes(userId);
        return Result.ok();
    }

    private Result<Void> deleteUserVotes(String userId) {
        List<Vote> votesToUpdate;
        try {
            votesToUpdate = hibernate.jpql("SELECT v FROM Vote v WHERE v.userId = '" + userId + "'", Vote.class);
        } catch (Exception e) {
            Log.info("JavaContent :: Internal error getting user votes");
            e.printStackTrace();
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
        for(Vote v : votesToUpdate){
            Result<Post> resPost = getPost(v.getPostId());
            if(!resPost.isOK()){
                return Result.error(resPost.error());
            }
            Post post = resPost.value();
            if(v.isGood()){
                post.setUpVote(post.getUpVote() - 1);
            } else {
                post.setDownVote(post.getDownVote() - 1);
            }
            try {
                hibernate.update(post);
            } catch (Exception e) {
                Log.info("JavaContent :: Internal error updating post");
                e.printStackTrace();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
            try {
                hibernate.delete(v);
            } catch (Exception e) {
                Log.info("JavaContent :: Internal error deleting vote");
                e.printStackTrace();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        return Result.ok();
    }

    private static void initializeUsersClient() {
        if (usersClient == null) {
            usersClient = usersClientFactory.createClient();
        }
    }

    private static void initializeImagesClient() {
        if (imageClient == null) {
            imageClient = imagesClientFactory.createClient();

        }
    }
    
}
