////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>
 * Checks that certain type members or reference to type instances are not accessible from outside 
 * of a class. <br>
 * Example: Return Logger out of the class is the very bad practice as it produce logs that are hard
 * to investigate as logging class does not contains that code and search should be done in 
 * other classes or in hierarchy (if filed is public or accessible by other protected or package).
 * </p>
 * <p>
 * Check has two parameters:<br>
 * <b>nonSharableTypes</b> - Names of types that will restricted for public access. Names must be 
 * qualified or canonical to avoid detecting classes that are named the same way but located in 
 * different packages.<br>
 * Default value for this parameter: &quot; java.util.logging.Logger, org.apache.log4j.Logger, 
 * org.slf4j.Logger, org.apache.commons.logging.Log &quot;<br>
 * <b>ignoreAnnotations</b> - Names of annotations that will ignored for this check.<br>
 * Default value for this parameter : &quot; java.lang.Override, 
 * com.google.common.annotations.VisibleForTesting &quot;
 * </p>
 * <p>
 * <b>Example 1.</b><br>
 * Check will verify that the type org.slf4j.Logger could not back out of the current class.<br>
 * Configuration:
 * </p>
 * <pre>
 * &lt;module name="nonSharableType"&gt; 
 *     &lt;property name="nonSharableTypes" value="org.slf4j.Logger"/&gt; 
 * &lt;/module&gt;
 * </pre>
 * <p>
 * Result:
 * </p>
 * <pre>
 * <code>
 * package com.test;
 * 
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 * 
 * public class Check {
 * 
 *     private Logger log = LoggerFactory.getLogger(getClass()); //Correct
 * 
 *     protected Logger log2 = LoggerFactory.getLogger(getClass()); //Violation
 * 
 *     public Logger log3 = LoggerFactory.getLogger(getClass()); //Violation
 * 
 *     Logger log4 = LoggerFactory.getLogger(getClass()); //Violation
 * 
 *     public Logger getLog() { //Violation
 *         return log;
 *     }
 * }
 * </code>
 * </pre>
 * <b>Example 2.</b><br>
 * Check will verify that the types java.util.regex.Pattern and java.util.regex.Matcher could not 
 * back out of the current class. Methods with annotation "MyAnnotation" will be ignored.<br>
 * Configuration:
 * </p>
 * <pre>
 * &lt;module name="nonSharableType"&gt; 
 *     &lt;property name="nonSharableTypes" value="java.util.regex.Pattern, java.util.regex.Matcher"/&gt;
 *     &lt;property name="ignoreAnnotationCanonicalNames" value="java.lang.SuppressWarnings"/&gt; 
 * &lt;/module&gt;
 * </pre>
 * <p>
 * Result:
 * </p>
 * <pre>
 * <code>
 * package com.test;
 * 
 * import java.util.regex.*;
 * 
 * public class Check {
 * 
 *     private List<Pattern> pattern1; //Correct
 * 
 *     List <Pattern> pattern2; //Violation
 *     
 *     protected Pattern pattern3; //Violation
 *     
 *     public Matcher matcher1; //Violation
 *     
 *     {@literal @}SuppressWarnings
 *     public Matcher method() {}; //Correct
 *     
 * }    
 * </code>
 * </pre>
 * <p>
 * For suppress this check on projects that do management of Loggers you should use 
 * <a href="http://checkstyle.sourceforge.net/config.html#SuppressionFilter">SuppressionFilter</a><br>
 * Example of configuration:<br>
 * </p>
 * <pre>
 * &lt;suppress checks="NonSharableType" files="com[\\/]mycompany[\\/]app[\\/]logging.*[\\/].*.java"/&gt;
 * </pre>
 * 
 * @author <a href="mailto:andrew.uljanenko@gmail.com">Andrew Uljanenko</a>
 */
public class NonSharableTypeCheck extends Check
{
    /**
     * Violation message key.
     */
    public static final String MSG_KEY = "non.sharable.type";
    
    /**
     * Default non sharable types.
     */
    private static final String[] DEFAULT_TYPES = {
        "java.util.logging.Logger",
        "org.apache.log4j.Logger",
        "org.slf4j.Logger",
        "org.apache.commons.logging.Log"        
    };
    
    /**
     * Default ignore annotation canonical names.
     */
    private static final String[] DEFAULT_ANNOTATIONS = {
      "java.lang.Override",
      "com.google.common.annotations.VisibleForTesting"
    };

    /**
     * List with specified types.
     */
    private List<String> nonSharableTypes = new ArrayList<String>();
    
    /**
     * List with specified ignored annotations.
     */
    private List<String> ignoreAnnotations = new ArrayList<String>();

    /**
     * List of on-demand-imports for current AST.
     */
    private static List<String> onDemandImports = new ArrayList<String>();

    /**
     * List of single-type-imports for current AST.
     */
    private static List<String> singleTypeImports = new ArrayList<String>();

    /**
     * Name of current package.
     */
    private static String packageName;

    /**
     * Constructor to set default configuration.
     */
    public NonSharableTypeCheck()
    {
        setNonSharableTypes(DEFAULT_TYPES);
        setIgnoreAnnotations(DEFAULT_ANNOTATIONS);
    }
    
    /**
     * Initialization of variable nonSharableTypes.
     * @param nonSharableTypes
     *        string array with types.
     */
    public void setNonSharableTypes(String[] nonSharableTypes)
    {
        this.nonSharableTypes.clear();
        for (String currentType : nonSharableTypes) {
            if (isStandardName(currentType)) {
                String typeNameLastPart = 
                        currentType.substring(currentType.lastIndexOf(".") + 1);
                this.nonSharableTypes.add(typeNameLastPart);
            }
            this.nonSharableTypes.add(currentType);
        }
    }
    
    /**
     * Initialization of variable ignoreAnnotations.
     * @param ignoreAnnotations
     *        string array with annotation names.
     */
    public void setIgnoreAnnotations(String[] ignoreAnnotations)
    {
        for (String currentAnnotation : ignoreAnnotations) {
            if (isStandardName(currentAnnotation)) {
                String annotationNameLastPart = 
                        currentAnnotation.substring(currentAnnotation.lastIndexOf(".") + 1);
                this.ignoreAnnotations.add(annotationNameLastPart);
            }
            this.ignoreAnnotations.add(currentAnnotation);
        }
    }
    
    @Override
    public final int[] getAcceptableTokens()
    {
        return new int[] {
                TokenTypes.PACKAGE_DEF,
                TokenTypes.IMPORT,
                TokenTypes.VARIABLE_DEF,
                TokenTypes.METHOD_DEF,
        };
    }
    
    @Override
    public final int[] getDefaultTokens()
    {
        return new int[] {
                TokenTypes.PACKAGE_DEF,
                TokenTypes.IMPORT,
                TokenTypes.VARIABLE_DEF,
                TokenTypes.METHOD_DEF,
        };
    }
    
    @Override
    public final int[] getRequiredTokens()
    {
        return new int[] {
                TokenTypes.PACKAGE_DEF,
                TokenTypes.IMPORT,
                TokenTypes.VARIABLE_DEF,
                TokenTypes.METHOD_DEF,
        };
    }
    
    @Override
    public void beginTree(DetailAST node)
    {
        onDemandImports.clear();
        singleTypeImports.clear();
        packageName = "";
    }
    
    @Override
    public void visitToken(DetailAST node)
    {
        switch (node.getType()) {

            case TokenTypes.PACKAGE_DEF:
                packageName = getIdentifierName(node);
                break;
    
            case TokenTypes.IMPORT:
                String currentImport = getIdentifierName(node);
                if (isOnDemandImport(currentImport)) {
                    onDemandImports.add(currentImport);
                }
                else {
                    singleTypeImports.add(currentImport);
                }
                break;
    
            case TokenTypes.VARIABLE_DEF:
                if (isMember(node) && !isPrivateMember(node) && isAccessibleFromOutside(node)) {
                    List<String> variableTypes = getMemberTypes(node);
                    for (String currentType : variableTypes) {
                        if (containsCurrentName(currentType, nonSharableTypes)) {
                            log(node.getLineNo(), MSG_KEY, currentType);
                        }
                    }
                }
                break;
                
            case TokenTypes.METHOD_DEF:
                if (!isPrivateMember(node) && !hasIgnoreAnnotation(node)) {
                    List<String> typesOfMethodReturn = getMemberTypes(node);
                    for (String currentType : typesOfMethodReturn) {
                        if (containsCurrentName(currentType, nonSharableTypes)) {
                            log(node.getLineNo(), MSG_KEY, currentType);
                        }
                    }
                }
                break;
            default:
                throw new IllegalStateException(node.toString());
        }
    }

    /**
     * Checks whether the given name in the list.
     * @param verifiableName
     *        String with current name.
     * @param listWithNames
     *        List with expected names.
     * @return true, if current name is in the list.
     */
    private static boolean containsCurrentName(String verifiableName, List<String> listWithNames)
    {
        boolean result = false;
        for (String currentName : listWithNames) {
            //if qualified type
            if (currentName.equals(verifiableName)) {
                result = true;
            }
            else {
                //try to join type with on-demand-imports
                for (String currentOnDemandImport : onDemandImports) {
                    String fullyQualifiedType =
                            joinOnDemandImportWithIdentifier(currentOnDemandImport, verifiableName);
                    if (currentName.equals(fullyQualifiedType)) {
                        result = true;
                        break;
                    }
                }
                if (!result) {
                    //try to join type with single-type-imports
                    for (String currentSingleTypeImport : singleTypeImports) {
                        String importEntryLastPart = currentSingleTypeImport.
                                substring(currentSingleTypeImport.lastIndexOf(".") + 1);
                        if (importEntryLastPart.equals(verifiableName)
                                && currentName.equals(currentSingleTypeImport)) {
                            result = true;
                            break;
                        }
                    }
                }
                if (!result) {
                    //if the type described in current package
                    String fullyQualifiedTypeWithPackage = packageName + "." + verifiableName;
                    if (currentName.equals(fullyQualifiedTypeWithPackage)) {
                        result = true;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks for ignore annotation.
     * @param definitionNode
     *        AST method definition node.
     * @return true, if method has ignoring annotation.
     */
    private boolean hasIgnoreAnnotation(DetailAST definitionNode)
    {
        boolean result = false;
        DetailAST modifiersNode = definitionNode.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiersNode != null) {
            DetailAST annotationNode = modifiersNode.findFirstToken(TokenTypes.ANNOTATION);
            if (annotationNode != null) {
                String annotationName = getIdentifierName(annotationNode);
                if (containsCurrentName(annotationName, ignoreAnnotations)) {
                    result = true;
                }
            }    
        }
        return result;
    }

    /**
     * Checks where there is a field(in class, enum or interface), and whether you can get access to
     * it.
     * @param defNode
     *        AST field definition node.
     * @return true, if field is not private and accessible from outside.
     */
    private static boolean isAccessibleFromOutside(DetailAST fieldDefNode)
    {
        boolean result = false;
        DetailAST definitionNode = fieldDefNode.getParent().getParent();
        if ((definitionNode.getType() == TokenTypes.CLASS_DEF
                    && !isClassDefInMethodDef(definitionNode)
                    && isAccessibleFromOutsideClass(definitionNode))
                || definitionNode.getType() == TokenTypes.ENUM_DEF
                || definitionNode.getType() == TokenTypes.INTERFACE_DEF) {
            result = true;
        }
        return result;
    }
    
    /**
     * Checks the location of the member.
     * @param memberName
     * @return true, if member located in standard package.
     */
    private static boolean isStandardName(String memberName)
    {
        String memberNameLastPart =
                memberName.substring(0, memberName.lastIndexOf("."));
        return memberNameLastPart.equals("java.lang");
    }
    
    /**
     * Returns full name of identifier.
     * @param identifierNode
     *        AST node.
     * @return string with full name of current identifier.
     */
    private static String getIdentifierName(DetailAST identifierNode)
    {
        DetailAST identNode = identifierNode.findFirstToken(TokenTypes.IDENT);
        DetailAST dotNode = identifierNode.findFirstToken(TokenTypes.DOT);
        String result;

        if (identNode != null) {
            result = identNode.getText();
        }
        else if (dotNode != null) {
            StringBuilder builder = new StringBuilder(40);
            while (dotNode.getType() == TokenTypes.DOT) {
                builder.insert(0, '.').insert(1, dotNode.getLastChild().getText());
                dotNode = dotNode.getFirstChild();
            }
            builder.insert(0, dotNode.getText());
            result = builder.toString();
        }
        else {
            result = identifierNode.getFirstChild().getText();
        }
        return result;
    }

    /**
     * Checks whether import is on demand import.
     * @param importName
     *        name of import.
     * @return true, if import is on-demand import.
     */
    private static boolean isOnDemandImport(String importName)
    {
        return importName.endsWith(".*");
    }

    /**
     * Return all member types from declaration.
     * @param definitionNode
     *        AST definition node.
     * @return list, which contains all member types.
     */
    private static List<String> getMemberTypes(DetailAST definitionNode)
    {
        List<String> memberTypes = new ArrayList<String>();
        DetailAST typeNode = definitionNode.findFirstToken(TokenTypes.TYPE);

        if (typeNode.getChildCount() == 1) {
            memberTypes.add(getIdentifierName(typeNode));
        }
        else {
            DetailAST typeArgumentsNode = typeNode.findFirstToken(TokenTypes.TYPE_ARGUMENTS)
                    .getFirstChild();
            while (typeArgumentsNode.getNextSibling() != null) {
                if (typeArgumentsNode.getType() == TokenTypes.TYPE_ARGUMENT) {
                    memberTypes.add(getIdentifierName(typeArgumentsNode));
                }
                typeArgumentsNode = typeArgumentsNode.getNextSibling();
            }
        }
        return memberTypes;
    }

    /**
     * Checks whether the field a member of the class.
     * @param definitionNode
     *        AST definition node.
     * @return true, if a field is a member.
     */
    private static boolean isMember(DetailAST definitionNode)
    {
        return definitionNode.getParent().getParent().getType() == TokenTypes.CLASS_DEF
                || definitionNode.getParent().getParent().getType() == TokenTypes.INTERFACE_DEF
                || definitionNode.getParent().getParent().getType() == TokenTypes.ENUM_DEF;
    }

    /**
     * Checks whether a member of a private.
     * @param definitionNode
     *        AST definition node.
     * @return true, if member is private.
     */
    private static boolean isPrivateMember(DetailAST definitionNode)
    { 
        boolean result = false;
        DetailAST modifiersNode = definitionNode.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiersNode != null &&
                modifiersNode.branchContains(TokenTypes.LITERAL_PRIVATE)) {
            result = true;
        }
        return result;
    }
    
    /**
     * Joins on demand import entry and identifier name into fully qualified name.
     * @param importEntry
     *        on demand import entry for join.
     * @param identifierName
     *        identifier name to join to import.
     * @return fully qualified identifier name.
     */
    private static String
            joinOnDemandImportWithIdentifier(String importEntry, String identifierName)
    {
        return importEntry.substring(0, importEntry.length() - 1) + identifierName;
    }

    /**
     * Checks nesting of classes and the ability to access them.
     * @param definitionNode
     *        AST definition node.
     * @return true, if external classes are not private.
     */
    private static boolean isAccessibleFromOutsideClass(DetailAST definitionNode)
    {
        boolean result = true;
        DetailAST currentNode = definitionNode;
        while (currentNode != null) {
            if (isPrivateMember(currentNode)) {
                result = false;
                break;
            }
            currentNode = currentNode.getParent();
        }
        return result;
    }

    /**
     * Verify that class definition is in method definition.
     * @param classDefNode
     *        AST definition node.
     * @return true, if class definition is in method definition.
     */
    private static boolean isClassDefInMethodDef(DetailAST classDefNode)
    {
        boolean result = false;
        DetailAST currentParentNode = classDefNode.getParent();
        while (currentParentNode != null) {
            if (currentParentNode.getType() == TokenTypes.METHOD_DEF) {
                result = true;
                break;
            }
            currentParentNode = currentParentNode.getParent();
        }
        return result;
    }
}
