package com.dcs.semantic.projection.process;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import javax.annotation.PostConstruct;


/**
 *
 * Simple enum for supported languages.
 *
 * Created by DCS Group on 8/30/17.
 */

public enum Language {

    ENGLISH,

    GERMAN,

    FRENCH,

    SPANISH,

    SINHALA,

    CHINESE;

    public void method(){
    }

    public static Language get(String tl) {
        if (tl.equals("english") || tl.equals("en")) return ENGLISH;
        if (tl.equals("german") || tl.equals("de")) return GERMAN;
        if (tl.equals("french") || tl.equals("fr")) return FRENCH;
        if (tl.equals("spanish") || tl.equals("es")) return SPANISH;
        if (tl.equals("chinese") || tl.equals("zh")) return CHINESE;
        if (tl.equals("sinhala") || tl.equals("si")) return SINHALA;
        return ENGLISH;
    }
}
