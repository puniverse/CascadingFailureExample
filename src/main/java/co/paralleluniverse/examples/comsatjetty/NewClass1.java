package co.paralleluniverse.examples.comsatjetty;

import java.nio.channels.spi.SelectorProvider;

public class NewClass1 {
    
    public static void main(String[] args) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        System.out.println(SelectorProvider.provider());
    }
}
