package com.dcs.semantic.projection.process;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 *
 * Simple enum for supported languages.
 *
 * Created by Alan Akbik on 8/30/17.
 */

public enum Language {

    ENGLISH,

    GERMAN,

    FRENCH,

    SPANISH,

    SINHALA,

    CHINESE;

    @PostConstruct
    public void method(){}

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
