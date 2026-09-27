package br.com.example.senac.businessDocsAi.chat.service;

import br.com.example.senac.businessDocsAi.chat.entity.Chat;
import br.com.example.senac.businessDocsAi.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public List<Chat> findAll() {
        return chatRepository.findAll();
    }


}
