package fctreddit.impl.grpc.util;

import fctreddit.api.Post;
import fctreddit.api.User;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;

public class DataModelAdaptor {

    public static User GrpcUser_to_User(UsersProtoBuf.GrpcUser from) {
        return new User(
                from.getUserId(),
                from.getEmail(),
                from.getFullName(),
                from.getPassword());
    }


    public static UsersProtoBuf.GrpcUser User_to_GrpcUser(User from) {
        return UsersProtoBuf.GrpcUser.newBuilder()
                .setUserId(from.getUserId())
                .setPassword(from.getPassword())
                .setEmail(from.getEmail() != null ? from.getEmail() : "")
                .setFullName(from.getFullName() != null ? from.getFullName() : "")
                .build();
        }

    public static Post GrpcPost_to_Post(ContentProtoBuf.GrpcPost from) {
        return new Post(
                from.getPostId(),
                from.getAuthorId(),
                from.getCreationTimestamp(),
                from.getContent(),
                from.getMediaUrl(),
                from.getParentUrl(),
                from.getUpVote(),
                from.getDownVote());
    }

    public static ContentProtoBuf.GrpcPost Post_to_GrpcPost(Post from) {
        ContentProtoBuf.GrpcPost.Builder b = ContentProtoBuf.GrpcPost.newBuilder()
                .setPostId(from.getPostId())
                .setAuthorId(from.getAuthorId())
                .setCreationTimestamp(from.getCreationTimestamp())
                .setContent(from.getContent())
                .setMediaUrl(from.getMediaUrl())
                .setParentUrl(from.getParentUrl())
                .setUpVote(from.getUpVote())
                .setDownVote(from.getDownVote());
        return b.build();
    }

}
