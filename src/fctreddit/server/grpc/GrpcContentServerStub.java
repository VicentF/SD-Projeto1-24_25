package fctreddit.server.grpc;

import java.util.List;

import fctreddit.api.Post;
import fctreddit.api.java.Content;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ContentGrpc;
import fctreddit.impl.grpc.generated_java.ContentProtoBuf;
import fctreddit.impl.grpc.util.DataModelAdaptor;
import fctreddit.impl.java.JavaContent;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import java.util.logging.Logger;

public class GrpcContentServerStub implements ContentGrpc.AsyncService, BindableService {

    private static String uri;
    private static Content impl;
    private static final Logger Log = Logger.getLogger(GrpcContentServerStub.class.getName());
    public GrpcContentServerStub(String URI) {
        this.uri = URI;
        impl = new JavaContent(uri);
    }

    @Override
    public ServerServiceDefinition bindService() {
        return ContentGrpc.bindService(this);
    }

    @Override
    public void createPost(ContentProtoBuf.CreatePostArgs request, StreamObserver<ContentProtoBuf.CreatePostResult> responseObserver) {

        Result<String> res = impl.createPost(
                DataModelAdaptor.GrpcPost_to_Post(request.getPost()),
                request.getPassword());

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            responseObserver.onNext( ContentProtoBuf.CreatePostResult.newBuilder()
                    .setPostId(res.value())
                    .build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getPosts(ContentProtoBuf.GetPostsArgs request, StreamObserver<ContentProtoBuf.GetPostsResult> responseObserver) {

        Result<List<String>> res = impl.getPosts(
                request.getTimestamp(),
                request.getSortOrder());

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            ContentProtoBuf.GetPostsResult.Builder builder = ContentProtoBuf.GetPostsResult.newBuilder();
            for (String s: res.value()){
                builder.addPostId(s);

            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
    /*public void getPosts(ContentProtoBuf.GetPostsArgs request, StreamObserver<ContentProtoBuf.GetPostsResult> responseObserver) {

        Result<List<String>> res = impl.getPosts(
                request.getTimestamp(),
                request.getSortOrder());

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            for (String s: res.value()){
                responseObserver.onNext(ContentProtoBuf.GetPostsResult.newBuilder()
                        .setPostId(0, s)
                        .build());
            }
            responseObserver.onCompleted();
        }
    }*/

    @Override
    public void getPost(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.GrpcPost> responseObserver) {

        Result<Post> res = impl.getPost(
                request.getPostId()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            responseObserver.onNext(DataModelAdaptor.Post_to_GrpcPost(res.value()));
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getPostAnswers(ContentProtoBuf.GetPostAnswersArgs request, StreamObserver<ContentProtoBuf.GetPostsResult> responseObserver) {
        Result<List<String>> res = impl.getPostAnswers(
                request.getPostId(),
                request.getTimeout()
        );

        if (!res.isOK()){
            Log.info("Error in getPostAnswers: " + res.error());
            responseObserver.onError(errorCodeToStatus(res.error()));
        }
        else {
            ContentProtoBuf.GetPostsResult.Builder builder = ContentProtoBuf.GetPostsResult.newBuilder();

            for (String s : res.value()) {
                builder.addPostId(s); 
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }
    /*public void getPostAnswers(ContentProtoBuf.GetPostAnswersArgs request, StreamObserver<ContentProtoBuf.GetPostsResult> responseObserver) {
        int counter = 0;
        Result<List<String>> res = impl.getPostAnswers(
                request.getPostId(),
                request.getTimeout()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            for (String s: res.value()){
                responseObserver.onNext(ContentProtoBuf.GetPostsResult.newBuilder()
                        .setPostId(counter, s)
                        .build());
                        counter++;
            }
            responseObserver.onCompleted();
        }

    }*/

    @Override
    public void updatePost(ContentProtoBuf.UpdatePostArgs request, StreamObserver<ContentProtoBuf.GrpcPost> responseObserver) {

        Result<Post> res = impl.updatePost(
                request.getPostId(),
                request.getPassword(),
                DataModelAdaptor.GrpcPost_to_Post(request.getPost())
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            responseObserver.onNext(DataModelAdaptor.Post_to_GrpcPost(res.value()));
            responseObserver.onCompleted();
        }

    }

    @Override
    public void deletePost(ContentProtoBuf.DeletePostArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {

        Result<Void> res = impl.deletePost(
                request.getPostId(),
                request.getPassword()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void upVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {

        Result<Void> res = impl.upVotePost(
                request.getPostId(),
                request.getUserId(),
                request.getPassword()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void removeUpVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {

        Result<Void> res = impl.removeUpVotePost(
                request.getPostId(),
                request.getUserId(),
                request.getPassword()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void downVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {

        Result<Void> res = impl.downVotePost(
                request.getPostId(),
                request.getUserId(),
                request.getPassword()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void removeDownVotePost(ContentProtoBuf.ChangeVoteArgs request, StreamObserver<ContentProtoBuf.EmptyMessage> responseObserver) {

        Result<Void> res = impl.removeDownVotePost(
                request.getPostId(),
                request.getUserId(),
                request.getPassword()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getUpVotes(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.VoteCountResult> responseObserver) {

        Result<Integer> res = impl.getupVotes(
                request.getPostId()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.VoteCountResult.newBuilder()
                    .setCount(res.value()).build());
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getDownVotes(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.VoteCountResult> responseObserver) {

        Result<Integer> res = impl.getDownVotes(
                request.getPostId()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext(  ContentProtoBuf.VoteCountResult.newBuilder()
                    .setCount(res.value()).build());
            responseObserver.onCompleted();
        }

    }

    /*public void getUpVotes(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.VoteCountResult> responseObserver) {

        Result<Integer> res = impl.getupVotes(
                request.getPostId()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.VoteCountResult.newBuilder().build());
            responseObserver.onCompleted();
        }

    }*/
    /*public void getDownVotes(ContentProtoBuf.GetPostArgs request, StreamObserver<ContentProtoBuf.VoteCountResult> responseObserver) {

        Result<Integer> res = impl.getDownVotes(
                request.getPostId()
        );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ContentProtoBuf.VoteCountResult.newBuilder().build());
            responseObserver.onCompleted();
        }

    }*/

    protected static Throwable errorCodeToStatus(Result.ErrorCode error ) {
        var status =  switch( error) {
            case NOT_FOUND -> io.grpc.Status.NOT_FOUND;
            case CONFLICT -> io.grpc.Status.ALREADY_EXISTS;
            case FORBIDDEN -> io.grpc.Status.PERMISSION_DENIED;
            case NOT_IMPLEMENTED -> io.grpc.Status.UNIMPLEMENTED;
            case BAD_REQUEST -> io.grpc.Status.INVALID_ARGUMENT;
            default -> io.grpc.Status.INTERNAL;
        };

        return status.asException();
    }

}
