package com.github.sevntu.checkstyle.checks.coding;

import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.lang.String;

public class InputNonAccessibleTypeCheck1
{
    public Pattern testVar1; //violation
    protected Pattern testVar2; //violation
    Pattern testVar3; //violation
    private Pattern testVar4;

    public java.util.regex.Pattern testVar5; //violation
    protected java.util.regex.Pattern testVar6; //violation
    java.util.regex.Pattern testVar7; //violation
    private java.util.regex.Pattern testVar8;
    
    String testVar9; //violation
    private String testVar10;
    Map <String, Pattern> testVar11; //violation
    private Map <String, Pattern> testVar12;
    List <String> testVar13; //violation
    private List <String> testVar14;
    
    protected NonAccessibleTypeCheck testVar15; //violation
    private NonAccessibleTypeCheck testVar16;
    
    public Map <Pattern, Matcher> a; //violation
    
}
