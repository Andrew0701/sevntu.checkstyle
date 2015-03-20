package com.github.sevntu.checkstyle.checks.coding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InputNonSharableTypeCheck4
{
    interface testInterface1
    {
        public List <Pattern> testVar1 = new ArrayList<Pattern>(); //violation
        Pattern testVar2 = Pattern.compile("1"); //violation
        int testVar3 = 1;
        Pattern someMethod(); // violation
    }
    
    enum testEnum
    {
        testVar1(1), testVar2(2), testVar3(1);
        
        private static Pattern a;
        private int value;
        public Pattern b; //violation
        
        private testEnum(int value)
        {
            this.value = value;
        }
        
        public static Pattern getA() //violation
        {
            return a;
        }
    }
}
