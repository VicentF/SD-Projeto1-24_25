package fctreddit.server.grpc;

import fctreddit.api.User;
import fctreddit.api.java.Result;
import fctreddit.api.java.Users;
import fctreddit.impl.grpc.generated_java.UsersGrpc;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;
import fctreddit.impl.grpc.util.DataModelAdaptor;
import fctreddit.impl.java.JavaUsers;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.StreamObserver;
import jakarta.ws.rs.core.Response;

import java.util.List;

public class GrpcUsersServerStub implements UsersGrpc.AsyncService, BindableService{

    Users impl = new JavaUsers();

    @Override
    public ServerServiceDefinition bindService() {
        return UsersGrpc.bindService(this);
    }

    @Override
    public void createUser(UsersProtoBuf.CreateUserArgs request, StreamObserver<UsersProtoBuf.CreateUserResult> responseObserver) {

        Result<String> res = impl.createUser(DataModelAdaptor.GrpcUser_to_User(request.getUser()));

        if (!res.isOK())
                responseObserver.onError(errorCodeToStatus(res.error()));
        else  {
            responseObserver.onNext( UsersProtoBuf.CreateUserResult.newBuilder().setUserId( res.value() ).build() );
            responseObserver.onCompleted();
        }

    }

    @Override
    public void getUser(UsersProtoBuf.GetUserArgs request, StreamObserver<UsersProtoBuf.GetUserResult> responseObserver) {
        /**
        Result<User> res = impl.getUser(
                DataModelAdaptor.GrpcUser_to_User(request.getUserId()),
                DataModelAdaptor.GrpcUser_to_User(request.getPassword()));
        if ( !res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {

            responseObserver.onCompleted();
        }
         **/

    }

    @Override
    public void updateUser(UsersProtoBuf.UpdateUserArgs request, StreamObserver<UsersProtoBuf.UpdateUserResult> responseObserver) {
        UsersGrpc.AsyncService.super.updateUser(request, responseObserver);
    }

    @Override
    public void deleteUser(UsersProtoBuf.DeleteUserArgs request, StreamObserver<UsersProtoBuf.DeleteUserResult> responseObserver) {
        UsersGrpc.AsyncService.super.deleteUser(request, responseObserver);
    }

    @Override
    public void searchUsers(UsersProtoBuf.SearchUserArgs request, StreamObserver<UsersProtoBuf.GrpcUser> responseObserver) {

        Result<List<User>> res = impl.searchUsers(request.getPattern());
        if ( !res.isOK() )
            responseObserver.onError(errorCodeToStatus(res.error()));
        else {
            for (User u: res.value()){
                responseObserver.onNext(DataModelAdaptor.User_to_GrpcUser(u));
            }
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
