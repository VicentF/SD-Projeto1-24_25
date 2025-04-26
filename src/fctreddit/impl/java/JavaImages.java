package fctreddit.impl.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.api.java.Result;
import fctreddit.clients.UsersClients.UsersClient;
import fctreddit.clients.UsersClients.UsersClientFactory;

public class JavaImages implements Image {

    private final String baseDir;
	private static Logger Log = Logger.getLogger(JavaImages.class.getName());
    private static UsersClient client = null;
    private static final UsersClientFactory userClientFactory = new UsersClientFactory();

    public JavaImages(String uri) {
        this.baseDir = uri;
    }

    
    @Override
    public Result<String> createImage(String userId, byte[] imageContents, String password) {
        initializeContentClient();
        Result<User> resUser = client.getUser(userId, password);
        if (!resUser.isOK()) {
            return Result.error(resUser.error());
        }
        if (imageContents.length == 0) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        String imageName = UUID.randomUUID().toString() + ".png";
        Path imagePath = Paths.get(userId, imageName);

        try {
            Files.createDirectories(imagePath.getParent());
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }

        try {
            Files.write(imagePath, imageContents);
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        
        //Log.info("JAVA: Image created with ID: " + imageName);
        return Result.ok(baseDir + imagePath.toString()); //perguntar se é suposto ser o uri absoluto ou só esta situation
    }

    @Override
    public Result<byte[]> getImage(String userId, String imageId) {
        Path imagePath = Paths.get(userId, imageId);
        if (!Files.exists(imagePath)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        try {
            byte[] imageData = Files.readAllBytes(imagePath);
            return Result.ok(imageData);
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        if(password == null){
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
        initializeContentClient();
        Result<User> resUser = client.getUser(userId, password);
        if(!resUser.isOK()){
            return Result.error(resUser.error());
        }
        Path imagePath = Paths.get(userId, imageId);
        if (!Files.exists(imagePath)) {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
        try {
            Files.delete(imagePath);
            return Result.ok(null);
        } catch (IOException e) {
            return Result.error(Result.ErrorCode.BAD_REQUEST);
        }
    }

    private static void initializeContentClient() {
        if (client == null) {
            client = userClientFactory.createClient();
        }
    }
    
}
