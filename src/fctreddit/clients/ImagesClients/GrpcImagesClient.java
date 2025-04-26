package fctreddit.clients.ImagesClients;

import com.google.common.primitives.Bytes;
import com.google.protobuf.ByteString;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import io.grpc.*;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;
import java.util.Iterator;

public class GrpcImagesClient extends ImagesClient{

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final ImageGrpc.ImageBlockingStub stub;

    public GrpcImagesClient(URI  serverURI) {
        Channel channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = ImageGrpc.newBlockingStub(channel);
    }

    public Result<String> createImage(String userId, ByteString imageContents, String password) {
        try {

            ImageProtoBuf.CreateImageResult res = stub.createImage(ImageProtoBuf.CreateImageArgs.newBuilder()
                    .setUserId(userId)
                    .setImageContents(imageContents)
                    .setPassword(password)
                    .build());
            return  Result.ok(res.getImageId());

        }
        catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()) );

        }
    }

    public Result<byte[]> getImage(String userId, String imageId) {
        try {

            Iterator<ImageProtoBuf.GetImageResult> it = stub.getImage(
                    ImageProtoBuf.GetImageArgs.newBuilder()
                            .setUserId(userId)
                            .setImageId(imageId)
                            .build()
            );

            if (it.hasNext()) {
                ImageProtoBuf.GetImageResult res = it.next();
                return Result.ok(res.getData().toByteArray());
            }
            else {
                return Result.error( statusToErrorCode(Status.NOT_FOUND) );
            }

        }
        catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()) );

        }
    }

    public Result<Void> deleteImage(String userId, String imageId, String password) {
        try {

            ImageProtoBuf.DeleteImageResult res = stub.deleteImage(ImageProtoBuf.DeleteImageArgs.newBuilder()
                    .setUserId(userId)
                    .setImageId(imageId)
                    .setPassword(password)
                    .build());
            return Result.ok(null);
        }
        catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()) );
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
