package ws.furrify.artists.avatar.strategy;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import ws.furrify.shared.exception.Errors;
import ws.furrify.shared.exception.FileContentIsCorruptedException;
import ws.furrify.shared.exception.FileUploadCannotCreatePathException;
import ws.furrify.shared.exception.FileUploadFailedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.UUID;

/**
 * Upload avatar file to local storage strategy.
 * Needs to be created as bean for @Value to work.
 *
 * @author sky
 */
@RequiredArgsConstructor
public class LocalStorageAvatarUploadStrategy implements AvatarUploadStrategy {

    @Value("${LOCAL_STORAGE_AVATAR_PATH:/data/artist/{0}/avatar/{1}}")
    private String LOCAL_STORAGE_AVATAR_PATH;

    @Value("${REMOTE_STORAGE_AVATAR_PATH:/artist/{0}/avatar/{1}}")
    private String REMOTE_STORAGE_AVATAR_PATH;

    @Value("${THUMBNAIL_WIDTH:600}")
    private int THUMBNAIL_WIDTH;

    @Value("${THUMBNAIL_QUALITY:0.90}")
    private float THUMBNAIL_QUALITY;

    @Value("${THUMBNAIL_PREFIX:thumbnail_}")
    private String THUMBNAIL_PREFIX;

    private final static String THUMBNAIL_EXTENSION = ".jpg";

    @Override
    public UploadedAvatarFile uploadAvatarWithGeneratedThumbnail(final UUID artistId, final UUID avatarId, final MultipartFile fileSource) {
        try (
                // Generate thumbnail
                InputStream thumbnailInputStream = AvatarUploadStrategyUtils.generateThumbnail(
                        THUMBNAIL_WIDTH,
                        THUMBNAIL_QUALITY,
                        fileSource.getInputStream()
                );
                InputStream avatarInputStream = fileSource.getInputStream()
        ) {

            // Check if filename is not null
            if (fileSource.getOriginalFilename() == null) {
                throw new IllegalStateException("Filename cannot be empty.");
            }

            // Sanitize filename
            String filename = fileSource.getOriginalFilename().replaceAll("\\s+","_");

            // Storage paths used for file creation and CDN requests
            String localStoragePath = MessageFormat.format(LOCAL_STORAGE_AVATAR_PATH, artistId, avatarId);
            String remoteStoragePath = MessageFormat.format(REMOTE_STORAGE_AVATAR_PATH, artistId, avatarId);

            // Create files
            File avatarFile = new File(localStoragePath + "/" + filename);

            // Create thumbnail filename by removing extension from original filename
            String thumbnailFileName = THUMBNAIL_PREFIX +
                    filename.substring(
                            0,
                            filename.lastIndexOf(".")
                    ) + THUMBNAIL_EXTENSION;

            File thumbnailFile = new File(localStoragePath + "/" + thumbnailFileName);
            // Create directories where files need to be located
            boolean wasAvatarFileCreated = avatarFile.getParentFile().mkdirs() || avatarFile.getParentFile().exists();
            boolean wasAvatarThumbnailFileCreated = thumbnailFile.getParentFile().mkdirs() || avatarFile.getParentFile().exists();

            if (!wasAvatarFileCreated || !wasAvatarThumbnailFileCreated) {
                throw new FileUploadCannotCreatePathException(Errors.FILE_UPLOAD_CANNOT_CREATE_PATH.getErrorMessage());
            }

            // Upload files
            writeToFile(avatarFile, avatarInputStream);
            writeToFile(thumbnailFile, thumbnailInputStream);

            // Return created urls
            return new UploadedAvatarFile(
                    // Original
                    new URI(remoteStoragePath + "/" + filename),
                    // Thumbnail
                    new URI(remoteStoragePath + "/" + thumbnailFileName)
            );

        } catch (IOException | URISyntaxException e) {
            throw new FileContentIsCorruptedException(Errors.FILE_CONTENT_IS_CORRUPTED.getErrorMessage());
        }
    }


    private void writeToFile(File file, InputStream inputStream) {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new FileUploadFailedException(Errors.FILE_UPLOAD_FAILED.getErrorMessage());
        }
    }

}
