package com.github.sevntu.checkstyle.checks.coding;

import java.lang.*;

public class InputNonAccessibleTypeCheck3
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
            String a; //violation
            class TestClass5
            {
                String a; //violation
                private class TestClass6
                {
                    String a;
                }
            }
        }
    }
}
