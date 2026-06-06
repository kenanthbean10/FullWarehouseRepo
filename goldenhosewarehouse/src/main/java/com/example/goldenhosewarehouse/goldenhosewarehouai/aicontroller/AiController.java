//package com.example.goldenhosewarehouse.goldenhosewarehouai.aicontroller;
//
//import com.example.goldenhosewarehouse.goldenhosewarehouai.service.WarehouseAiService;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("api/ai")
//public class AiController {
//private final WarehouseAiService aiService;
//
//    public AiController(WarehouseAiService aiService) {
//        this.aiService = aiService;
//    }
//    @GetMapping("/ask")
//    public String ask(@RequestParam String q){
//        return aiService.askWarehouse(q);
//    }
//}
