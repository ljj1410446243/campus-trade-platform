package com.campus.trade.chat.service.impl;

import com.campus.trade.chat.client.FileServiceClient;
import com.campus.trade.chat.client.dto.FileMetadataDTO;
import com.campus.trade.chat.common.BargainAction;
import com.campus.trade.chat.common.BargainStatus;
import com.campus.trade.chat.common.BaseException;
import com.campus.trade.chat.common.MessageType;
import com.campus.trade.chat.dto.MessagePayload;
import com.campus.trade.chat.dto.MessageVO;
import com.campus.trade.chat.dto.SendMessageRequest;
import com.campus.trade.chat.dto.SendMessageVO;
import com.campus.trade.chat.entity.ChatConversation;
import com.campus.trade.chat.entity.ChatMessage;
import com.campus.trade.chat.repository.ChatMessageRepository;
import com.campus.trade.chat.service.ChatConversationService;
import com.campus.trade.chat.service.ChatMessageService;
import com.campus.trade.chat.util.UserAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final String FILE_BIZ_TYPE_CHAT_IMAGE = "CHAT_IMAGE";
    private static final String FILE_STATUS_UPLOADED = "UPLOADED";
    private static final String FILE_STATUS_BOUND = "BOUND";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationService chatConversationService;
    private final UserAccessGuard userAccessGuard;
    private final FileServiceClient fileServiceClient;

    @Override
    public List<MessageVO> getMessagesByConversationId(String conversationId, String currentUserId) {
        ChatConversation conversation = chatConversationService.getById(conversationId);
        chatConversationService.validateConversationMember(conversation, currentUserId);
        return chatMessageRepository.findByConversationIdOrderBySeqAsc(conversationId)
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    @Override
    public ChatMessage saveMessage(String senderId, SendMessageRequest request) {
        userAccessGuard.assertWritable(senderId);
        ChatConversation conversation = chatConversationService.getById(request.getConversationId());
        chatConversationService.validateConversationMember(conversation, senderId);

        String normalizedType = normalizeType(request.getType());
        validateRequestPayload(senderId, request, normalizedType);

        ChatMessage message = new ChatMessage();
        message.setConversationId(request.getConversationId());
        message.setSenderId(senderId);
        message.setType(normalizedType);
        message.setContent(resolveContent(request, normalizedType));
        message.setImage(copyImagePayload(request.getImage()));
        message.setEmoji(copyEmojiPayload(request.getEmoji()));
        message.setSeq(nextSeq(request.getConversationId()));
        message.setCreatedAt(LocalDateTime.now());

        ChatMessage saved = chatMessageRepository.save(message);
        if (MessageType.IMAGE.equals(normalizedType) && saved.getImage() != null) {
            fileServiceClient.bindChatImage(senderId, saved.getImage().getFileId(), saved.getId());
        }

        chatConversationService.updateConversationAfterMessage(
                conversation,
                senderId,
                buildMessagePreview(saved)
        );
        return saved;
    }

    @Override
    public ChatMessage saveBargainMessage(String conversationId,
                                          String senderId,
                                          MessagePayload.BargainPayload bargainPayload) {
        userAccessGuard.assertWritable(senderId);
        ChatConversation conversation = chatConversationService.getById(conversationId);
        chatConversationService.validateConversationMember(conversation, senderId);

        if (bargainPayload == null || bargainPayload.getRecordId() == null || bargainPayload.getRecordId().isBlank()) {
            throw new BaseException(400, "议价消息缺少recordId");
        }
        if (bargainPayload.getPrice() == null || bargainPayload.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BaseException(400, "议价金额非法");
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setType(MessageType.BARGAIN);
        message.setContent(buildBargainContent(bargainPayload));
        message.setBargain(copyBargainPayload(bargainPayload));
        message.setSeq(nextSeq(conversationId));
        message.setCreatedAt(LocalDateTime.now());

        ChatMessage saved = chatMessageRepository.save(message);
        chatConversationService.updateConversationAfterMessage(
                conversation,
                senderId,
                buildMessagePreview(saved)
        );
        return saved;
    }

    @Override
    public Long nextSeq(String conversationId) {
        return chatMessageRepository.findTopByConversationIdOrderBySeqDesc(conversationId)
                .map(msg -> msg.getSeq() + 1)
                .orElse(1L);
    }

    @Override
    public MessageVO toMessageVO(ChatMessage message) {
        MessageVO vo = new MessageVO();
        vo.setMessageId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setType(message.getType());
        vo.setContent(message.getContent());
        vo.setImage(copyImagePayload(message.getImage()));
        vo.setEmoji(copyEmojiPayload(message.getEmoji()));
        vo.setBargain(copyBargainPayload(message.getBargain()));
        vo.setSeq(message.getSeq());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    @Override
    public SendMessageVO toSendMessageVO(ChatMessage message) {
        SendMessageVO vo = new SendMessageVO();
        vo.setMessageId(message.getId());
        vo.setConversationId(message.getConversationId());
        vo.setSenderId(message.getSenderId());
        vo.setType(message.getType());
        vo.setContent(message.getContent());
        vo.setImage(copyImagePayload(message.getImage()));
        vo.setEmoji(copyEmojiPayload(message.getEmoji()));
        vo.setBargain(copyBargainPayload(message.getBargain()));
        vo.setSeq(message.getSeq());
        vo.setCreatedAt(message.getCreatedAt());
        return vo;
    }

    private void validateRequestPayload(String senderId, SendMessageRequest request, String normalizedType) {
        if (MessageType.BARGAIN.equals(normalizedType)) {
            throw new BaseException(400, "议价消息仅支持专用接口发送");
        }

        if (MessageType.TEXT.equals(normalizedType)) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new BaseException(400, "文本消息内容不能为空");
            }
            return;
        }

        if (MessageType.EMOJI.equals(normalizedType)) {
            if (request.getEmoji() == null) {
                throw new BaseException(400, "表情消息缺少emoji信息");
            }
            if (isBlank(request.getEmoji().getCode()) || isBlank(request.getEmoji().getDisplay())) {
                throw new BaseException(400, "表情消息缺少code或display");
            }
            return;
        }

        if (MessageType.IMAGE.equals(normalizedType)) {
            if (request.getImage() == null || isBlank(request.getImage().getFileId())) {
                throw new BaseException(400, "图片消息缺少fileId");
            }
            FileMetadataDTO metadata = fileServiceClient.getFileMetadata(request.getImage().getFileId());
            validateImageMetadata(senderId, metadata);
            request.setImage(mergeImagePayload(request.getImage(), metadata));
            return;
        }

        throw new BaseException(400, "不支持的消息类型");
    }

    private void validateImageMetadata(String senderId, FileMetadataDTO metadata) {
        if (metadata == null || isBlank(metadata.getFileId())) {
            throw new BaseException(400, "图片文件不存在");
        }
        if (!senderId.equals(metadata.getOwnerId())) {
            throw new BaseException(403, "只能发送本人上传的聊天图片");
        }
        if (!FILE_BIZ_TYPE_CHAT_IMAGE.equalsIgnoreCase(metadata.getBizType())) {
            throw new BaseException(400, "仅支持CHAT_IMAGE类型图片");
        }
        if (!FILE_STATUS_UPLOADED.equalsIgnoreCase(metadata.getStatus())
                && !FILE_STATUS_BOUND.equalsIgnoreCase(metadata.getStatus())) {
            throw new BaseException(400, "当前图片状态不可引用");
        }
    }

    private MessagePayload.ImagePayload mergeImagePayload(MessagePayload.ImagePayload payload, FileMetadataDTO metadata) {
        MessagePayload.ImagePayload image = new MessagePayload.ImagePayload();
        image.setFileId(metadata.getFileId());
        image.setUrl(defaultIfBlank(payload == null ? null : payload.getUrl(), metadata.getUrl()));
        image.setPreviewUrl(defaultIfBlank(payload == null ? null : payload.getPreviewUrl(), metadata.getPreviewUrl()));
        image.setThumbnailUrl(defaultIfBlank(payload == null ? null : payload.getThumbnailUrl(), metadata.getThumbnailUrl()));
        image.setWidth(payload != null && payload.getWidth() != null ? payload.getWidth() : metadata.getWidth());
        image.setHeight(payload != null && payload.getHeight() != null ? payload.getHeight() : metadata.getHeight());
        return image;
    }

    private String resolveContent(SendMessageRequest request, String normalizedType) {
        if (MessageType.TEXT.equals(normalizedType)) {
            return request.getContent().trim();
        }
        if (MessageType.EMOJI.equals(normalizedType)) {
            return request.getEmoji().getDisplay();
        }
        if (MessageType.IMAGE.equals(normalizedType)) {
            return isBlank(request.getContent()) ? null : request.getContent().trim();
        }
        return request.getContent();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new BaseException(400, "消息类型不能为空");
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private String buildMessagePreview(ChatMessage message) {
        if (MessageType.IMAGE.equals(message.getType())) {
            return "[图片]";
        }
        if (MessageType.EMOJI.equals(message.getType())) {
            return "[表情]";
        }
        if (MessageType.BARGAIN.equals(message.getType()) && message.getBargain() != null) {
            String price = formatPrice(message.getBargain().getPrice());
            if (BargainStatus.ACCEPTED.equals(message.getBargain().getStatus())) {
                return "[议价已接受] ￥" + price;
            }
            if (BargainStatus.REJECTED.equals(message.getBargain().getStatus())) {
                return "[议价已拒绝] ￥" + price;
            }
            return "[议价] ￥" + price;
        }
        String content = message.getContent();
        if (content == null) {
            return "";
        }
        return content.length() > 50 ? content.substring(0, 50) : content;
    }

    private String buildBargainContent(MessagePayload.BargainPayload bargainPayload) {
        String price = formatPrice(bargainPayload.getPrice());
        if (BargainAction.ACCEPT.equals(bargainPayload.getAction())) {
            return "[议价已接受] ￥" + price;
        }
        if (BargainAction.REJECT.equals(bargainPayload.getAction())) {
            return "[议价已拒绝] ￥" + price;
        }
        return "[议价] ￥" + price;
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "0.00" : price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private MessagePayload.ImagePayload copyImagePayload(MessagePayload.ImagePayload payload) {
        if (payload == null) {
            return null;
        }
        MessagePayload.ImagePayload copy = new MessagePayload.ImagePayload();
        copy.setFileId(payload.getFileId());
        copy.setUrl(payload.getUrl());
        copy.setPreviewUrl(payload.getPreviewUrl());
        copy.setThumbnailUrl(payload.getThumbnailUrl());
        copy.setWidth(payload.getWidth());
        copy.setHeight(payload.getHeight());
        return copy;
    }

    private MessagePayload.EmojiPayload copyEmojiPayload(MessagePayload.EmojiPayload payload) {
        if (payload == null) {
            return null;
        }
        MessagePayload.EmojiPayload copy = new MessagePayload.EmojiPayload();
        copy.setCode(payload.getCode());
        copy.setDisplay(payload.getDisplay());
        return copy;
    }

    private MessagePayload.BargainPayload copyBargainPayload(MessagePayload.BargainPayload payload) {
        if (payload == null) {
            return null;
        }
        MessagePayload.BargainPayload copy = new MessagePayload.BargainPayload();
        copy.setRecordId(payload.getRecordId());
        copy.setAction(payload.getAction());
        copy.setPrice(payload.getPrice());
        copy.setStatus(payload.getStatus());
        copy.setInitiatorId(payload.getInitiatorId());
        return copy;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultIfBlank(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }
}
