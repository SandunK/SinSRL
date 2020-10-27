package com.dcs.semantic.projection;

import com.dcs.semantic.projection.process.Annotator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
public class endpoints {
    private final
    Annotator annotator;

    @Autowired
    public endpoints(Annotator annotator) {
        this.annotator = annotator;
    }

    @RequestMapping("/")
    public String health() {
        return "I am Ok";
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PostMapping("project")
    public Map<String, String> project(@RequestBody Map<String, String> payload) {
        System.out.println("Inside Sinhala SRL Projector ...");
        ArrayList result = annotator.project(payload.get("engSentence"),payload.get("sinSentence"));
        System.out.println("Sentence was tagged ...");
        System.out.println(result);
        Map<String,String> returnResult = new HashMap<>();
        returnResult.put("result",result.toString());
        return returnResult;
    }
}
