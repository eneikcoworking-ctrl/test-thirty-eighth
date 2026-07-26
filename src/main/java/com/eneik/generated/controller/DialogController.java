package com.eneik.generated.controller;

import com.eneik.generated.model.Dialog;
import com.eneik.generated.model.Message;
import com.eneik.generated.repository.DialogRepository;
import com.eneik.generated.repository.MessageRepository;
import com.eneik.generated.service.TelegramDispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/dialogs")
public class DialogController {

    @Autowired
    private DialogRepository dialogRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TelegramDispatchService telegramDispatchService;

    @GetMapping
    public List<Dialog> getAllDialogs() {
        return dialogRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dialog> getDialogById(@PathVariable String id) {
        return dialogRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/messages")
    public List<Message> getDialogMessages(@PathVariable String id) {
        return messageRepository.findByDialogId(id);
    }

    @PostMapping("/{id}/override")
    @Transactional
    public ResponseEntity<?> processOverride(
            @PathVariable String id,
            @RequestBody OperatorOverrideRequest request,
            WebRequest webRequest) {

        // 1. Fetch Dialog
        Dialog dialog = dialogRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Dialog with ID " + id + " not found."));

        // 2. Determine Telegram Account ID to use
        Long telegramAccountId = request.getTelegramAccountId();
        if (telegramAccountId == null) {
            telegramAccountId = dialog.getTelegramAccountId();
        }

        if (telegramAccountId == null) {
            throw new IllegalArgumentException("No Telegram session/account is assigned to this dialog, and none was supplied in the payload.");
        }

        // 3. Atomically update the AI State and Status to "PAUSED"
        String currentStatus = dialog.getStatus();
        if (!"PAUSED".equals(currentStatus)) {
            int updatedRows = dialogRepository.updateStatusAtomically(
                    id,
                    currentStatus,
                    "PAUSED",
                    "PAUSED",
                    telegramAccountId,
                    LocalDateTime.now()
            );
            if (updatedRows == 0) {
                throw new ConcurrentModificationException("The dialogue status was modified concurrently by another process. Please retry.");
            }
            // Update local object representation for subsequent steps in this transaction context
            dialog.setStatus("PAUSED");
            dialog.setAiState("PAUSED");
            dialog.setTelegramAccountId(telegramAccountId);
        } else if (dialog.getTelegramAccountId() == null) {
            // If already paused but we need to assign the account, update database
            dialog.setTelegramAccountId(telegramAccountId);
            dialogRepository.save(dialog);
        }

        // 4. Dispatch the message via Telegram Dispatch Service
        Long telegramMessageId = telegramDispatchService.dispatchMessage(
                telegramAccountId,
                dialog.getTelegramChatId(),
                request.getContent()
        );

        // 5. Save the operator manual reply as a Message
        Message operatorMessage = new Message();
        operatorMessage.setId(UUID.randomUUID().toString());
        operatorMessage.setDialogId(id);
        operatorMessage.setSenderType("OPERATOR");
        operatorMessage.setContent(request.getContent());
        operatorMessage.setTelegramMessageId(telegramMessageId);

        Message savedMessage = messageRepository.save(operatorMessage);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
    }

    // --- DTO for Operator Override Request ---
    public static class OperatorOverrideRequest {
        private String content;
        private Long telegramAccountId;

        public OperatorOverrideRequest() {}

        public OperatorOverrideRequest(String content, Long telegramAccountId) {
            this.content = content;
            this.telegramAccountId = telegramAccountId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Long getTelegramAccountId() {
            return telegramAccountId;
        }

        public void setTelegramAccountId(Long telegramAccountId) {
            this.telegramAccountId = telegramAccountId;
        }
    }

    // --- Standard Error Handlers conforming to OpenAPI ErrorResponse Schema ---
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflictOrStateError(IllegalStateException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrentModification(ConcurrentModificationException ex, WebRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Concurrent Modification", ex.getMessage(), request);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(status).body(body);
    }
}
