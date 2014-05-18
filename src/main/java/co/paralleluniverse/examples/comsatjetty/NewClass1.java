package co.paralleluniverse.examples.comsatjetty;

import java.lang.reflect.Field;

public class NewClass1 {
    public int a;
    Runnable r = new Runnable() {

        @Override
        public void run() {
            System.out.println(a);
        }
    };
    
    public static void main(String[] args) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        NewClass1 obj = new NewClass1();
//        Field field = newClass1.r.getClass().getDeclaredField("this$0");
//        System.out.println(((NewClass1)field.get(newClass1.r)).a);
        System.out.println(obj.r.getClass().getEnclosingClass());
        
//        Object get = field.get(newClass1.r);
        
    }
}
