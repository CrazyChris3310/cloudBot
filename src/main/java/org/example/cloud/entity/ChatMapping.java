package org.example.cloud.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_mapping")
@Getter
@Setter
public class ChatMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long telegramChat;
    private Long vkChat;

}
