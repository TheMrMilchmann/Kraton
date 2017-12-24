package com.github.themrmilchmann.kraton.test.lang.java.classes;

/**
 * A slightly more complex "Hello World" sample for showing off more of Kraton's capabilities.
 *
 * @since 1.0.0
 *
 * @author Leon Linhart <themrmilchmann@gmail.com>
 */
public final class HelloWorld2 {

    public static void main(String[] args) {
        new HelloWorld("Hello World").run();
    }

    private final String text;

    private HelloWorld2(String text) {
        this.text = text;
    }

    private void run() {
        System.out.println(this.text);
    }

}