package ws.furrify.posts.attachment;

import ws.furrify.posts.attachment.dto.AttachmentDTO;

import java.time.ZonedDateTime;
import java.util.UUID;

class AttachmentFactory {

    Attachment from(AttachmentDTO attachmentDTO) {
        AttachmentSnapshot attachmentSnapshot = AttachmentSnapshot.builder()
                .id(attachmentDTO.getId())
                .attachmentId(
                        (attachmentDTO.getAttachmentId() != null) ? attachmentDTO.getAttachmentId() : UUID.randomUUID()
                )
                .postId(attachmentDTO.getPostId())
                .ownerId(attachmentDTO.getOwnerId())
                .filename(attachmentDTO.getFilename())
                .md5(attachmentDTO.getMd5())
                .extension(attachmentDTO.getExtension())
                .fileUrl(attachmentDTO.getFileUrl())
                .createDate(
                        (attachmentDTO.getCreateDate() != null) ? attachmentDTO.getCreateDate() : ZonedDateTime.now()
                )
                .build();

        return Attachment.restore(attachmentSnapshot);
    }

}