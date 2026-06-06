//package com.example.goldenhosewarehouse.goldenhosewarehouai.service;
//
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.stereotype.Service;
//
//@Service
//public class WarehouseAiService {
//
//private final ChatClient chatClient;
//
//
//    public WarehouseAiService(ChatClient.Builder builder) {
//        this.chatClient = builder.defaultFunctions("fetchAssetFromWarehouse").build();
//
//    }
//    public String askWarehouse(String userQuery)
//    {
//        return chatClient.prompt().user(userQuery).call().content();
//    }
//}
