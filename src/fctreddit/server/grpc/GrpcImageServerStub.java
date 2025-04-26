package fctreddit.server.grpc;

import com.google.protobuf.ByteString;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;
import fctreddit.impl.grpc.util.DataModelAdaptor;
import fctreddit.impl.java.JavaImages;
import fctreddit.impl.java.JavaUsers;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;

import java.net.URI;

public class GrpcImageServerStub implements ImageGrpc.AsyncService, BindableService {

    String uri;
    Image impl = new JavaImages(uri);

    public GrpcImageServerStub(String uri) {
        this.uri = uri;
    }

    @Override
    public ServerServiceDefinition bindService() {
        return ImageGrpc.bindService(this);
    }

    @Override
    public void createImage(ImageProtoBuf.CreateImageArgs request, StreamObserver<ImageProtoBuf.CreateImageResult> responseObserver) {

        Result<String> res = impl.createImage(
                request.getUserId(),
                request.getImageContents().toByteArray(),
                request.getPassword());

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            responseObserver.onNext( ImageProtoBuf.CreateImageResult.newBuilder()
                    .setImageId( res.value() ).build() );
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getImage(ImageProtoBuf.GetImageArgs request, StreamObserver<ImageProtoBuf.GetImageResult> responseObserver) {

        Result<byte[]> res = impl.getImage( request.getUserId(), request.getImageId() );

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            responseObserver.onNext( ImageProtoBuf.GetImageResult.newBuilder()
                    .setData( ByteString.copyFrom( res.value() ) )
                    .build());
            responseObserver.onCompleted();
        }


    }

    @Override
    public void deleteImage(ImageProtoBuf.DeleteImageArgs request, StreamObserver<ImageProtoBuf.DeleteImageResult> responseObserver) {

        Result<Void> res = impl.deleteImage(request.getUserId(), request.getImageId(), request.hasPassword() ? request.getPassword() : null);

        if (!res.isOK())
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            responseObserver.onNext( ImageProtoBuf.DeleteImageResult.newBuilder()
                    .build() );
            responseObserver.onCompleted();
        }

    }

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
