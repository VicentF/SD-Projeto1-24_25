package fctreddit.impl.rest;

import java.util.logging.Logger;

import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.api.rest.RestImage; 
import fctreddit.impl.java.JavaImages;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status; 

public class ImagesResource implements RestImage{
    
    private static Logger Log = Logger.getLogger(UsersResource.class.getName());

    final Image impl;

    public ImagesResource() {
        impl = new JavaImages();
    }

    @Override
    public String createImage(String userId, byte[] imageContents, String password) {
        Log.info("Create Image");
		
		Result<String> res = impl.createImage(userId, imageContents, password);
		if(!res.isOK()) {
			throw new WebApplicationException(errorCodeToStatus(res.error()));
		}
		return res.value();
    }

    @Override
    public byte[] getImage(String userId, String imageId) {
        Log.info("Get Image");

        Result<byte[]> res = impl.getImage(userId, imageId);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
        return res.value();
    }

    @Override
    public void deleteImage(String userId, String imageId, String password) {
        Log.info("Delete Image");

        Result<Void> res = impl.deleteImage(userId, imageId, password);
        if(!res.isOK()) {
            throw new WebApplicationException(errorCodeToStatus(res.error()));
        }
    }

    protected static Status errorCodeToStatus( Result.ErrorCode error ) {
    	Status status =  switch( error) {
    	case NOT_FOUND -> Status.NOT_FOUND; 
    	case CONFLICT -> Status.CONFLICT;
    	case FORBIDDEN -> Status.FORBIDDEN;
    	case NOT_IMPLEMENTED -> Status.NOT_IMPLEMENTED;
    	case BAD_REQUEST -> Status.BAD_REQUEST;
    	default -> Status.INTERNAL_SERVER_ERROR;
    	};
    	
    	return status;
    }

}
