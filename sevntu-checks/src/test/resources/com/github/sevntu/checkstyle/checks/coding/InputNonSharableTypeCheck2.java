package com.github.sevntu.checkstyle.checks.coding;

import java.util.regex.Pattern;
import java.lang.String;

public class InputNonSharableTypeCheck2
{
    public Pattern getTestVar() //violation
    {
        return Pattern.compile("1");
    }
    
    java.util.regex.Pattern someMethod() // violation
    {
        String testVar1;
        Pattern testVar2;
        return Pattern.compile("1");
    }
    
    private Pattern someMethod2()
    {
        return Pattern.compile("1");
    }
    
    @Override
    public String toString()
    {
        return super.toString();
    }
}
