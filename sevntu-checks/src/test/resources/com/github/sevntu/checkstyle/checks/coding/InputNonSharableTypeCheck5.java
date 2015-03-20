package com.github.sevntu.checkstyle.checks.coding;

import org.slf4j.Logger;

public class InputNonSharableTypeCheck5
{
    private Logger log;

    protected Logger log2; //Violation

    public Logger log3; //Violation

    Logger log4; //Violation

    public Logger getLog() { //Violation
        return log;
    }
    
    static class TestLogger
    {
        private Logger log;
    }
    
}
