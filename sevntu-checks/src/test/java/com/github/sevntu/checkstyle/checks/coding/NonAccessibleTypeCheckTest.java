////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2012  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.github.sevntu.checkstyle.checks.coding;

import static com.github.sevntu.checkstyle.checks.coding.NonAccessibleTypeCheck.*;

import org.junit.Test;
import com.github.sevntu.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class NonAccessibleTypeCheckTest
        extends BaseCheckTestSupport
{
    @Test
    public void testFields()
            throws Exception
    {
        DefaultConfiguration checkConfig = createCheckConfig(NonAccessibleTypeCheck.class);
        final String[] expected = {
                "10: " + getCheckMessage(MSG_KEY, "Pattern"),
                "11: " + getCheckMessage(MSG_KEY, "Pattern"),
                "12: " + getCheckMessage(MSG_KEY, "Pattern"),
                "15: " + getCheckMessage(MSG_KEY, "java.util.regex.Pattern"),
                "16: " + getCheckMessage(MSG_KEY, "java.util.regex.Pattern"),
                "17: " + getCheckMessage(MSG_KEY, "java.util.regex.Pattern"),
                "20: " + getCheckMessage(MSG_KEY, "String"),
                "22: " + getCheckMessage(MSG_KEY, "Pattern"),
                "22: " + getCheckMessage(MSG_KEY, "String"),
                "24: " + getCheckMessage(MSG_KEY, "String"),
                "27: " + getCheckMessage(MSG_KEY, "NonAccessibleTypeCheck"),
                "30: " + getCheckMessage(MSG_KEY, "Matcher"),
                "30: " + getCheckMessage(MSG_KEY, "Pattern"),
        };
        
        checkConfig.addAttribute("nonAccessibleTypes", 
                "java.util.regex.Pattern"
                    + "|java.util.regex.Matcher"
                    + "|java.lang.String"
                    + "|com.github.sevntu.checkstyle.checks.coding.NonAccessibleTypeCheck");
    verify(checkConfig, getPath("InputNonAccessibleTypeCheck1.java"), expected);
    }
    
    @Test
    public void testMethods()
            throws Exception
    {
        DefaultConfiguration checkConfig = createCheckConfig(NonAccessibleTypeCheck.class);
        final String[] expected = {
                "7: " + getCheckMessage(MSG_KEY, "Pattern"),
                "12: " + getCheckMessage(MSG_KEY, "java.util.regex.Pattern"),
        };
        
        checkConfig.addAttribute("nonAccessibleTypes", "java.util.regex.Pattern|java.lang.String");
    verify(checkConfig, getPath("InputNonAccessibleTypeCheck2.java"), expected);
    }
    
    @Test
    public void testNestedClasses()
            throws Exception
    {
        DefaultConfiguration checkConfig = createCheckConfig(NonAccessibleTypeCheck.class);
        final String[] expected = {
                "9: " + getCheckMessage(MSG_KEY, "String"),
                "28: " + getCheckMessage(MSG_KEY, "String"),
                "31: " + getCheckMessage(MSG_KEY, "String"),
                "34: " + getCheckMessage(MSG_KEY, "String"),

        };
        
        checkConfig.addAttribute("nonAccessibleTypes","java.lang.String");
    verify(checkConfig, getPath("InputNonAccessibleTypeCheck3.java"), expected);
    }
    
    @Test
    public void testInterfaceAndEnum()
            throws Exception
    {
        DefaultConfiguration checkConfig = createCheckConfig(NonAccessibleTypeCheck.class);
        final String[] expected = {
                "11: " + getCheckMessage(MSG_KEY, "Pattern"),
                "12: " + getCheckMessage(MSG_KEY, "Pattern"),
                "14: " + getCheckMessage(MSG_KEY, "Pattern"),
                "23: " + getCheckMessage(MSG_KEY, "Pattern"),
                "30: " + getCheckMessage(MSG_KEY, "Pattern"),
        };
        
        checkConfig.addAttribute("nonAccessibleTypes", "java.util.regex.Pattern");
    verify(checkConfig, getPath("InputNonAccessibleTypeCheck4.java"), expected);
    }
}
