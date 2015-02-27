package com.github.sevntu.checkstyle.checks.coding;

import java.util.regex.Pattern;

public class InputNonAccessibleTypeCheck2
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
        // TODO Auto-generated method stub
        return super.toString();
    }
}
