package fctreddit.clients.ContentClients;

import fctreddit.api.Post;
import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.*;
import fctreddit.impl.grpc.util.DataModelAdaptor;
import io.grpc.*;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrpcContentClient extends ContentClients {

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final ContentGrpc.ContentBlockingStub stub;

    public GrpcContentClient(URI serverURI) {
        Channel channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = ContentGrpc.newBlockingStub(channel);
    }

    public Result<String> createPost(Post post, String password) {

        try {

            ContentProtoBuf.CreatePostResult res = stub.createPost(ContentProtoBuf.CreatePostArgs.newBuilder()
                    .setPost(DataModelAdaptor.Post_to_GrpcPost(post))
                    .setPassword(password)
                    .build());
            return Result.ok(res.getPostId());

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<List<String>> getPosts(Integer timestamp, String sortOrder) {
        try{

            ContentProtoBuf.GetPostsResult res = stub.getPosts(ContentProtoBuf.GetPostsArgs.newBuilder()
                    .setTimestamp(timestamp)
                    .setSortOrder(sortOrder)
                    .build());
            return Result.ok(res.getPostIdList());

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }
    }

    public Result<Post> getPost(String postId) {

        try {

            ContentProtoBuf.GrpcPost res = stub.getPost(ContentProtoBuf.GetPostArgs.newBuilder()
                    .setPostId(postId)
                    .build());
            return Result.ok(DataModelAdaptor.GrpcPost_to_Post(res));

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<List<String>> getPostAnswers(String postId, Integer timeout) {

        try {

            ContentProtoBuf.GetPostsResult res = stub.getPostAnswers(ContentProtoBuf.GetPostAnswersArgs.newBuilder()
                    .setPostId(postId)
                    .setTimeout(timeout)
                    .build());
            return Result.ok(res.getPostIdList());

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<Post> updatePost(String postId, String password, Post post) {

        try {

            ContentProtoBuf.GrpcPost res = stub.updatePost(ContentProtoBuf.UpdatePostArgs.newBuilder()
                    .setPostId(postId)
                    .setPassword(password)
                    .setPost(DataModelAdaptor.Post_to_GrpcPost(post))
                    .build());
            return Result.ok(DataModelAdaptor.GrpcPost_to_Post(res));

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<Void> deletePost(String postId, String password) {

        try {

            ContentProtoBuf.EmptyMessage res = stub.deletePost(ContentProtoBuf.DeletePostArgs.newBuilder()
                    .setPostId(postId)
                    .setPassword(password)
                    .build());
            return Result.ok(null);

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<Void> upVotePost(String postId, String userId, String password) {

        try {

            ContentProtoBuf.EmptyMessage res = stub.upVotePost(ContentProtoBuf.ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return Result.ok(null);

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }


    public Result<Void> removeUpVotePost(String postId, String userId, String password) {

        try {

            ContentProtoBuf.EmptyMessage res = stub.removeUpVotePost(ContentProtoBuf.ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return Result.ok(null);

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }
    public Result<Void> downVotePost(String postId, String userId, String password) {

        try {

            ContentProtoBuf.EmptyMessage res = stub.downVotePost(ContentProtoBuf.ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return Result.ok(null);

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }
    public Result<Void> removeDownVotePost(String postId, String userId, String password) {

        try {

            ContentProtoBuf.EmptyMessage res = stub.removeDownVotePost(ContentProtoBuf.ChangeVoteArgs.newBuilder()
                    .setPostId(postId)
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return Result.ok(null);

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }
    public Result<Integer> getUpVotes(String postId) {

        try {

            ContentProtoBuf.VoteCountResult res = stub.getUpVotes(ContentProtoBuf.GetPostArgs.newBuilder()
                    .setPostId(postId)
                    .build());
            return Result.ok(res.getCount());

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }
    public Result<Integer> getDownVotes(String postId) {

        try {

            ContentProtoBuf.VoteCountResult res = stub.getDownVotes(ContentProtoBuf.GetPostArgs.newBuilder()
                    .setPostId(postId)
                    .build());
            return Result.ok(res.getCount());

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }


    static Result.ErrorCode statusToErrorCode(Status status) {
        return switch(status.getCode()) {
            case OK -> Result.ErrorCode.OK;
            case NOT_FOUND -> Result.ErrorCode.NOT_FOUND;
            case ALREADY_EXISTS -> Result.ErrorCode.CONFLICT;
            case PERMISSION_DENIED -> Result.ErrorCode.FORBIDDEN;
            case INVALID_ARGUMENT -> Result.ErrorCode.BAD_REQUEST;
            case UNIMPLEMENTED -> Result.ErrorCode.NOT_IMPLEMENTED;
            default -> Result.ErrorCode.INTERNAL_ERROR;

        };
    }



}
