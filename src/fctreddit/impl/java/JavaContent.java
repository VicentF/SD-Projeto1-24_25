package fctreddit.impl.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import fctreddit.api.Post;
import fctreddit.api.User;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.clients.UsersClients.RestUsersClient;
import fctreddit.impl.persistence.Hibernate;

public class JavaContent implements  Content{
    //well i'll be damned
    private final Map<String, Post> posts;
    //private JavaUsers users;
    //private JavaImages images;
    private final Hibernate hibernate;

    public JavaContent() {
        hibernate = Hibernate.getInstance();
        posts = new ConcurrentHashMap<>();
    }

    /*public void setUsers(JavaUsers users) {
        this.users = users;
    }*/

    /*public void setImages(JavaImages images) {
        this.images = images;
    }*/

    @Override
    public Result<String> createPost(Post post, String userPassword) {
        RestUsersClient client = new RestUsersClient();
        Result<User> resUser = client.getUser(post.getAuthorId(), userPassword);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        //tou a assumir que não há posts com o mesmo id,
        //e se houver, substitui o post antigo, tmb não é dificil de fazer, caso necessário
        posts.put(post.getPostId(), post);
        return Result.ok(post.getPostId());
    }

    public Result<String> createPostHibernate(Post post, String userPassword){
        RestUsersClient client = new RestUsersClient();
        Result<User> resUser = client.getUser(post.getAuthorId(), userPassword);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        hibernate.persist(post);
        return Result.ok(post.getPostId());
    }

    @Override
    public Result<List<String>> getPosts(long timestamp, String sortOrder) {
        List<String> sortedKeys = new ArrayList<>();
        Stream<Map.Entry<String, Post>> stream = posts.entrySet().stream();
        if(timestamp > 0){
            stream = posts.entrySet().stream()
            .filter(entry -> entry.getValue().getCreationTimestamp() >= timestamp)
            .filter(entry -> entry.getValue().getParentUrl() == null);
        }
        if (sortOrder == null) {
            sortedKeys = stream.sorted(Comparator.comparing(entry -> entry.getValue().getCreationTimestamp()))
                .map(Map.Entry :: getKey)
                .toList();
        } else {
            switch(sortOrder){
                case MOST_UP_VOTES:
                    sortedKeys = stream
                    .sorted(Comparator.comparing((Map.Entry<String, Post> entry) -> entry.getValue().getUpVote())
                    .reversed().thenComparing(entry -> entry.getValue().getCreationTimestamp()))
                    .map(Map.Entry :: getKey)
                    .toList();
                    break;
                
                case MOST_REPLIES:
                    //TODO: implement this
                    break;
                
                default:
                    return Result.error(Result.ErrorCode.BAD_REQUEST);
            }
        }
        return Result.ok(sortedKeys);
    }

    public Result<List<String>> getPostsHibernate(long timestamp, String sortOrder){
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
        if(!posts.containsKey(postId)){
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else{
            return Result.ok(posts.get(postId));
        }
    }

    public Result<Post> getPostHibernate(String postId) {
        Post post = hibernate.get(Post.class, postId);
        if(post == null){
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } else{
            return Result.ok(post);
        }
    }

    @Override
    public Result<List<String>> getPostAnswers(String postId, long maxTimeout) {
        if(maxTimeout > 0){
            try {
                Thread.sleep(maxTimeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        if(!posts.containsKey(postId)){
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        List<String> sortedKeys = posts.entrySet().stream()
            .filter(entry -> postId.equals(entry.getValue().getParentUrl()))
            .sorted(Comparator.comparing(entry -> entry.getValue().getCreationTimestamp()))
            .map(Map.Entry :: getKey)
            .toList();
        return Result.ok(sortedKeys);
    }

    public Result<List<String>> getPostAnswersHibernate(String postId, long maxTimeout){
        if(maxTimeout > 0){
            try {
                Thread.sleep(maxTimeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.error(Result.ErrorCode.INTERNAL_ERROR);
            }
        }
        if(!this.getPost(postId).isOK()){
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        String query = "SELECT p.postId FROM Post p WHERE p.parentUrl LIKE CONCAT('%', :postId, '%')";
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
        return Result.ok(oldPost);
    }

    public Result<Post> updatePostHibernate(String postId, String userPassword, Post post) {
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
        RestUsersClient client = new RestUsersClient();
        Result<User> resUser = client.getUser(postId, userPassword);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        posts.remove(postId);
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
    public Result<Post> deleteUserId(String userId) {
        // DELETE FROM Post WHERE authorId LIKE '%userId%'
        List<String> postsToUpdate = posts.entrySet().stream()
            .filter(entry -> userId.equals(entry.getValue().getAuthorId()))
            .map(Map.Entry::getKey)
            .toList();
        for (String key : postsToUpdate) {
            posts.get(key).setAuthorId(null);
        }
        return Result.ok(null);
    }
    
}
