package br.com.example.senac.businessDocsAi.chat.controller;

import br.com.example.senac.businessDocsAi.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;

}
