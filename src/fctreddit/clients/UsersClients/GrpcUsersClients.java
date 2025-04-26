package fctreddit.clients.UsersClients;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;
import fctreddit.impl.grpc.util.DataModelAdaptor;
import io.grpc.*;
import io.grpc.internal.PickFirstLoadBalancerProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GrpcUsersClients extends UsersClient{

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final UsersGrpc.UsersBlockingStub stub;

    public GrpcUsersClients(URI serverURI) {
        Channel channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = UsersGrpc.newBlockingStub( channel );
    }

    /*public Result<String> createUser(User user) {

        try {

            UsersProtoBuf.CreateUserResult res = stub.createUser(UsersProtoBuf.CreateUserArgs.newBuilder()
                    .setUser(DataModelAdaptor.User_to_GrpcUser(user))
                    .build());
            return Result.ok(res.getUserId());

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }*/

    public Result<User> getUser(String id, String password) {

        try {

            UsersProtoBuf.GetUserResult res = stub.getUser(UsersProtoBuf.GetUserArgs.newBuilder()
                    .setUserId(id)
                    .setPassword(password)
                    .build());
            return Result.ok(DataModelAdaptor.GrpcUser_to_User(res.getUser()));

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    /*public Result<User> updateUser(String userId, String password, User user) {

        try {

            UsersProtoBuf.UpdateUserResult res = stub.updateUser(UsersProtoBuf.UpdateUserArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(password)
                    .setUser(DataModelAdaptor.User_to_GrpcUser(user))
                    .build());
            return  Result.ok(DataModelAdaptor.GrpcUser_to_User(res.getUser()));

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<User> deleteUser(String userId, String password) {

        try {

            UsersProtoBuf.DeleteUserResult res = stub.deleteUser(UsersProtoBuf.DeleteUserArgs.newBuilder()
                    .setUserId(userId)
                    .setPassword(password)
                    .build());
            return  Result.ok(DataModelAdaptor.GrpcUser_to_User(res.getUser()));

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }

    public Result<List<User>> searchUsers(String pattern) {

        try{

            Iterator<UsersProtoBuf.GrpcUser> res = stub.searchUsers(UsersProtoBuf.SearchUserArgs.newBuilder()
                    .setPattern(pattern)
                    .build());
            List<User> ret = new ArrayList<User>();
            while(res.hasNext()){
                ret.add(DataModelAdaptor.GrpcUser_to_User(res.next()));
            }
            return Result.ok(ret);

        } catch (StatusRuntimeException sre) {
            return Result.error( statusToErrorCode(sre.getStatus()));
        }

    }*/

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
