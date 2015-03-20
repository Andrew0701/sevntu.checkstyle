package com.github.sevntu.checkstyle.checks.coding;

import java.lang.*;

public class InputNonSharableTypeCheck3
{
    class TestClass1
    {
        String a; //violation
        int b;
        private class TestClass2
        {
            String a;
            public class TestClass3
            {
                String a;
            }
        }
    }
    
    private class TestClass2
    {
        String a;
    }
    
    public class TestClass3
    {
        String a; //violation
        public class TestClass4
        {
            protected String a; //violation
            class TestClass5
            {
                final String a = "a"; //violation
                private class TestClass6
                {
                    public String a;
                }
            }
        }
    }
    
    public void method()
    {
        class innerClass
        {
            public String a;
        }
    }
    
    interface inter1
    {
        static interface inter2
        {
            interface inter3
            {
                final java.lang.String a = "a"; //Violation
                String method(); //Violation
            }
        }
    }
    
    enum  testEnum
    {
        testVar1(1);
        private int value;
        public String a; //Violation
        private final String b = "b";
        
        testEnum(int value)
        {
            this.value = value;
        }
        
        enum testEnum2
        {
            testVar1(1), testVar2(2), testVar3(1);
            
            private static String a;
            private int value;
            public String b; //Violation
            
            private testEnum2(int value)
            {
                this.value = value;
            }
            
            public static String getA() //Violation
            {
                return a;
            }
        }
    }
    
}
