package com.dcs.pos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class endpoints {
    private final
    ProcessData processData;

    @Autowired
    public endpoints(ProcessData processData) {
        this.processData = processData;
    }

    @RequestMapping("/")
    public String health() {
        return "I am Ok";
    }

    @PostMapping("getpos")
    public Map<String, String> getPos(@RequestBody Map<String, String> payload) {
        System.out.println("Inside TNT PosTagger ...");
        processData.processSentence(payload.get("sentence"));
        Map<String,String> taggedData = processData.getTaggings();
        System.out.println("Sentence was tagged ...");
        System.out.println(taggedData.toString());
        return taggedData;
    }
}
