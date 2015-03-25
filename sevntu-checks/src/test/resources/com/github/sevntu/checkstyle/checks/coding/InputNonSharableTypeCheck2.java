package com.github.sevntu.checkstyle.checks.coding;

import com.github.sevntu.checkstyle.checks.coding.InputNonSharableTypeCheck6.myAnnotation;
import com.google.common.annotations.VisibleForTesting;

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
    
    private Pattern someMethod2() {
        return Pattern.compile("1");
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
    
    @VisibleForTesting 
    public Pattern someMethod3() {
        return Pattern.compile("1");
    }
    
    @com.google.common.annotations.VisibleForTesting
    public Pattern someMethod4() {
        return Pattern.compile("1");
    }
    
    @myAnnotation
    public Pattern someMethod5() {
        return Pattern.compile("1");
    }
    
    @com.github.sevntu.checkstyle.checks.coding.InputNonSharableTypeCheck6.myAnnotation
    public Pattern someMethod6() {
        return Pattern.compile("1");
    }
    
}
